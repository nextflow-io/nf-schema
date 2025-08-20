package nextflow.validation.validators

import groovy.util.logging.Slf4j

import java.util.regex.Matcher

import org.json.JSONObject
import dev.harrel.jsonschema.ValidatorFactory
import dev.harrel.jsonschema.Validator

import nextflow.validation.config.ValidationConfig
import static nextflow.validation.utils.Common.getValueFromJsonPointer
import static nextflow.validation.utils.Common.findAllKeys
import static nextflow.validation.utils.Common.kebabToCamel
import static nextflow.validation.utils.Types.isInteger


@Slf4j
public class ValidationResult {

    public Validator.Result result
    private Object rawInput 
    private String schemaString
    private JSONObject schemaJson
    private ValidationConfig config

    ValidationResult(Validator.Result result, Object rawInput, String schemaString, ValidationConfig config ) {
        this.result = result
        this.rawInput = rawInput
        this.schemaString = schemaString
        this.schemaJson = new JSONObject(schemaString)
        this.config = config
    }

    public List<String> getUnevaluated() {
        def Set<String> evaluated = []
        this.result.getAnnotations().each{ anno ->
            if(anno.keyword in ["properties", "patternProperties", "additionalProperties"]){
                evaluated.addAll(
                    anno.annotation.collect{ it ->
                    "${anno.instanceLocation.toString()}/${it.toString()}".replaceAll("^/+", "")
                    }
                )
            }
        }
        def Set<String> all_keys = []
        findAllKeys(this.rawInput, null, all_keys, '/')
        def unevaluated_ = all_keys - evaluated
        def unevaluated = unevaluated_.collect{ it -> !evaluated.contains(kebabToCamel(it)) ? it : null }
        return unevaluated - null
    }

    public List<String> getErrors(String validationType) {
        def List<String> errors = []
        this.result.getErrors().each { error ->
            def String errorString = error.getError()

            // Skip double error in the parameter schema
            if (errorString.startsWith("Value does not match against the schemas at indexes") && validationType == "parameter") {
                return
            }

            def String instanceLocation = error.getInstanceLocation()
            def String value = getValueFromJsonPointer(instanceLocation, this.rawInput)
            if(config.maxErrValSize >= 1 && value.size() > config.maxErrValSize) {
                value = "${value[0..(config.maxErrValSize/2-1)]}...${value[-config.maxErrValSize/2..-1]}" as String
            }

            // Return a standard error message for object validation
            if (validationType == "object") {
                errors.add("${instanceLocation ? instanceLocation + ' ' : ''}(${value}): ${errorString}" as String)
                return
            }

            // Get the custom errorMessage if there is one and the validation errors are not about the content of the file
            def String schemaLocation = error.getSchemaLocation().replaceFirst(/^[^#]+/, "")
            def String customError = ""
            if (!errorString.startsWith("Validation of file failed:")) {
                customError = getValueFromJsonPointer("${schemaLocation}/errorMessage", this.schemaJson) as String
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
        return errors
    }

}