package nextflow.validation

import groovy.util.logging.Slf4j
import groovy.transform.CompileStatic
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

import nextflow.validation.config.ValidationConfig
import nextflow.validation.exceptions.SchemaValidationException

/**
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */

@Slf4j
@CompileStatic
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

    private List<String> validateObject(JsonNode input, String validationType, Object rawJson, String schemaString) {
        def JSONObject schema = new JSONObject(schemaString)
        def String draft = Utils.getValueFromJson("#/\$schema", schema)
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
            def String value = Utils.getValueFromJson(instanceLocation, rawJson)

            // Get the custom errorMessage if there is one and the validation errors are not about the content of the file
            def String schemaLocation = error.getSchemaLocation().replaceFirst(/^[^#]+/, "")
            def String customError = ""
            if (!errorString.startsWith("Validation of file failed:")) {
                customError = Utils.getValueFromJson("${schemaLocation}/errorMessage", schema) as String
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
            if (locationList.size() > 0 && Utils.isInteger(locationList[0]) && validationType == "field") {
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
        return errors
    }

    public List<String> validate(JSONArray input, String schemaString) {
        def JsonNode jsonInput = new OrgJsonNode.Factory().wrap(input)
        return this.validateObject(jsonInput, "field", input, schemaString)
    }

    public List<String> validate(JSONObject input, String schemaString) {
        def JsonNode jsonInput = new OrgJsonNode.Factory().wrap(input)
        return this.validateObject(jsonInput, "parameter", input, schemaString)
    }
}