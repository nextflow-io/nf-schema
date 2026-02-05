package nextflow.validation.validators

import static nextflow.validation.utils.Common.getValueFromJsonPointer
import static nextflow.validation.utils.Common.findAllKeys
import static nextflow.validation.utils.Common.kebabToCamel
import static nextflow.validation.utils.Types.isInteger

import groovy.util.logging.Slf4j
import groovy.transform.CompileDynamic

import java.util.regex.Matcher

import org.json.JSONObject
import dev.harrel.jsonschema.Validator

import nextflow.validation.config.ValidationConfig

/**
 * The validation result wrapper
 *
 * @author : arthur
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */

@Slf4j
@CompileDynamic
public class ValidationResult {

    final private Validator.Result result
    final private Object rawInput
    final private String schemaString
    final private JSONObject schemaJson
    final private ValidationConfig config

    ValidationResult(Validator.Result result, Object rawInput, String schemaString, ValidationConfig config) {
        this.result = result
        this.rawInput = rawInput
        this.schemaString = schemaString
        this.schemaJson = new JSONObject(schemaString)
        this.config = config
    }

    List<String> getUnevaluated() {
        Set<String> evaluated = []
        this.result.annotations.each { anno ->
            if (anno.keyword in ['properties', 'patternProperties', 'additionalProperties']) {
                evaluated.addAll(
                    anno.annotation.collect { annotation ->
                    "${anno.instanceLocation}/${annotation}".replaceAll('^/+', '')
                    }
                )
            }
        }
        Set<String> allKeys = []
        findAllKeys(this.rawInput, null, allKeys, '/')
        Set<String> unevaluatedUnformatted = allKeys - evaluated
        List<String> unevaluated = unevaluatedUnformatted
            .collect { key -> evaluated.contains(kebabToCamel(key)) ? null : key }
        return unevaluated - null
    }

    List<String> getErrors(String validationType) {
        List<String> errors = []
        this.result.errors.each { error ->
            String errorString = error.error

            // Skip double error in the parameter schema
            if (
                errorString.startsWith('Value does not match against the schemas at indexes') &&
                validationType == 'parameter'
            ) {
                return
            }

            String instanceLocation = error.instanceLocation
            String value = getValueFromJsonPointer(instanceLocation, this.rawInput)
            if (config.maxErrValSize >= 1 && value.size() > config.maxErrValSize) {
                /* groovylint-disable-next-line LineLength */
                value = "${value[0..(config.maxErrValSize / 2 - 1)]}...${value[-config.maxErrValSize / 2..-1]}" as String
            }

            // Return a standard error message for object validation
            if (validationType == 'object') {
                errors.add("${instanceLocation ? instanceLocation + ' ' : ''}(${value}): ${errorString}" as String)
                return
            }

            // Get the custom errorMessage if there is one and the validation errors are not about the file content
            String schemaLocation = error.schemaLocation.replaceFirst(/^[^#]+/, '')
            String customError = ''
            if (!errorString.startsWith('Validation of file failed:')) {
                customError = getValueFromJsonPointer("${schemaLocation}/errorMessage", this.schemaJson) as String
            }

            // Change some error messages to make them more clear
            String keyword = error.keyword
            if (keyword == 'required') {
                Matcher matcher = errorString =~ ~/\[\[([^\[\]]*)\]\]$/
                String missingKeywords = matcher.findAll().flatten().last()
                errorString = "Missing required ${validationType}(s): ${missingKeywords}"
            }

            List<String> locationList = instanceLocation.split('/').findAll { loc -> loc != '' } as List

            String printableError = "${validationType == 'field' ? '->' : '*'} ${errorString}" as String
            if (locationList.size() > 0 && isInteger(locationList[0]) && validationType == 'field') {
                Integer entryInteger = locationList[0] as Integer
                String entryString = "Entry ${entryInteger + 1}" as String
                String fieldError = "${errorString}" as String
                if (locationList.size() > 1) {
                    /* groovylint-disable-next-line LineLength */
                    fieldError = "Error for ${validationType} '${locationList[1..-1].join('/')}' (${value}): ${errorString}"
                }
                printableError = "-> ${entryString}: ${fieldError}" as String
            } else if (validationType == 'parameter') {
                String fieldName = locationList.join('.')
                if (fieldName != '') {
                    printableError = "* --${fieldName} (${value}): ${errorString}" as String
                }
            }

            if (customError != '') {
                printableError = printableError + " (${customError})"
            }

            errors.add(printableError)
        }
        return errors
    }

    Validator.Result getResult() {
        return this.result
    }

}
