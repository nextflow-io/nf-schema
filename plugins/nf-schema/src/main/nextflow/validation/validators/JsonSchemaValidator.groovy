package nextflow.validation.validators

import groovy.util.logging.Slf4j
import org.json.JSONObject
import org.json.JSONArray
import dev.harrel.jsonschema.ValidatorFactory
import dev.harrel.jsonschema.Validator
import dev.harrel.jsonschema.EvaluatorFactory
import dev.harrel.jsonschema.FormatEvaluatorFactory
import dev.harrel.jsonschema.JsonNode
import dev.harrel.jsonschema.providers.OrgJsonNode

import java.util.regex.Pattern
import java.util.regex.Matcher

import static nextflow.validation.utils.Common.getValueFromJsonPointer
import static nextflow.validation.utils.Common.findAllKeys
import static nextflow.validation.utils.Common.kebabToCamel
import static nextflow.validation.utils.Types.isInteger
import nextflow.validation.config.ValidationConfig
import nextflow.validation.exceptions.SchemaValidationException
import nextflow.validation.validators.evaluators.CustomEvaluatorFactory

/**
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */

@Slf4j
public class JsonSchemaValidator {

    private ValidatorFactory validator
    private Pattern uriPattern = Pattern.compile('^#/(\\d*)?/?(.*)$')
    private ValidationConfig config

    JsonSchemaValidator(ValidationConfig config) {
        this.validator = new ValidatorFactory()
            .withJsonNodeFactory(new OrgJsonNode.Factory())
            // .withDialect() // TODO define the dialect
            .withEvaluatorFactory(EvaluatorFactory.compose(new CustomEvaluatorFactory(config), new FormatEvaluatorFactory()))
        this.config = config
    }

    private Tuple2<List<String>,List<String>> validateObject(JsonNode input, String validationType, Object rawJson, String schemaString) {
        def JSONObject schema = new JSONObject(schemaString)
        def String draft = getValueFromJsonPointer("#/\$schema", schema)
        if(draft != "https://json-schema.org/draft/2020-12/schema") {
            log.error("""Failed to load the meta schema:
    The used schema draft (${draft}) is not correct, please use \"https://json-schema.org/draft/2020-12/schema\" instead.
        - If you are a pipeline developer, check our migration guide for more information: https://nextflow-io.github.io/nf-schema/latest/migration_guide/
        - If you are a pipeline user, revert back to nf-validation to avoid this error: https://www.nextflow.io/docs/latest/plugins.html#using-plugins, i.e. set `plugins {
    id 'nf-validation@1.1.3'
}` in your `nextflow.config` file
            """)
            throw new SchemaValidationException("", [])
        }
        
        def Validator.Result result = this.validator.validate(schema, input)
        def List<String> errors = []
        result.getErrors().each { error ->
            def String errorString = error.getError()

            // Skip double error in the parameter schema
            if (errorString.startsWith("Value does not match against the schemas at indexes") && validationType == "parameter") {
                return
            }

            def String instanceLocation = error.getInstanceLocation()
            def String value = getValueFromJsonPointer(instanceLocation, rawJson)

            // Return a standard error message for object validation
            if (validationType == "object") {
                errors.add("${instanceLocation ? instanceLocation + ' ' : ''}(${value}): ${errorString}" as String)
                return
            }

            // Get the custom errorMessage if there is one and the validation errors are not about the content of the file
            def String schemaLocation = error.getSchemaLocation().replaceFirst(/^[^#]+/, "")
            def String customError = ""
            if (!errorString.startsWith("Validation of file failed:")) {
                customError = getValueFromJsonPointer("${schemaLocation}/errorMessage", schema) as String
            }

            // Change some error messages to make them more clear
            def String keyword = error.getKeyword()
            if (keyword == "required") {
                def Matcher matcher = errorString =~ ~/\[\[([^\[\]]*)\]\]$/
                def String missingKeywords = matcher.findAll().flatten().last()
                errorString = "Missing required ${validationType}(s): ${missingKeywords}"
            }

            def List<String> locationList = instanceLocation.split("/").findAll { it != "" } as List

            def String printableError = "${validationType == 'field' ? '->' : '*'} ${errorString}" as String
            if (locationList.size() > 0 && isInteger(locationList[0]) && validationType == "field") {
                def Integer entryInteger = locationList[0] as Integer
                def String entryString = "Entry ${entryInteger + 1}" as String
                def String fieldError = "${errorString}" as String
                if(locationList.size() > 1) {
                    fieldError = "Error for ${validationType} '${locationList[1..-1].join("/")}' (${value}): ${errorString}"
                }
                printableError = "-> ${entryString}: ${fieldError}" as String
            } else if (validationType == "parameter") {
                def String fieldName = locationList.join(".")
                if(fieldName != "") {
                    printableError = "* --${fieldName} (${value}): ${errorString}" as String
                }
            }

            if(customError != "") {
                printableError = printableError + " (${customError})"
            }

            errors.add(printableError)

        }
        def List<String> unevaluated = getUnevaluated(result, rawJson)
        return Tuple.tuple(errors, unevaluated)
    }

    public Tuple2<List<String>,List<String>> validate(JSONArray input, String schemaString) {
        def JsonNode jsonInput = new OrgJsonNode.Factory().wrap(input)
        return this.validateObject(jsonInput, "field", input, schemaString)
    }

    public Tuple2<List<String>,List<String>> validate(JSONObject input, String schemaString) {
        def JsonNode jsonInput = new OrgJsonNode.Factory().wrap(input)
        return this.validateObject(jsonInput, "parameter", input, schemaString)
    }

    public Tuple2<List<String>,List<String>> validateObj(Object input, String schemaString) {
        def JsonNode jsonInput = new OrgJsonNode.Factory().wrap(input)
        return this.validateObject(jsonInput, "object", input, schemaString)
    }

    public static List<String> getUnevaluated(Validator.Result result, Object rawJson) {
        def Set<String> evaluated = []
        result.getAnnotations().each{ anno ->
            if(anno.keyword in ["properties", "patternProperties", "additionalProperties"]){
                evaluated.addAll(
                    anno.annotation.collect{ it ->
                    "${anno.instanceLocation.toString()}/${it.toString()}".replaceAll("^/+", "")
                    }
                )
            }
        }
        def Set<String> all_keys = []
        findAllKeys(rawJson, null, all_keys, '/')
        def unevaluated_ = all_keys - evaluated
        def unevaluated = unevaluated_.collect{ it -> !evaluated.contains(kebabToCamel(it)) ? it : null }
        return unevaluated - null
    }
}