package nextflow.validation.samplesheet

import static nextflow.validation.utils.Colors.getLogColors
import static nextflow.validation.utils.Files.fileToJson
import static nextflow.validation.utils.Files.fileToObject
import static nextflow.validation.utils.Common.findDeep
import static nextflow.validation.utils.Common.hasDeepKey

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import groovy.transform.CompileDynamic
import java.nio.file.Path

import org.json.JSONArray

import nextflow.Nextflow

import nextflow.validation.config.ValidationConfig
import nextflow.validation.exceptions.SchemaValidationException
import nextflow.validation.validators.JsonSchemaValidator
import nextflow.validation.validators.ValidationResult

/**
 * @author : mirpedrol <mirp.julia@gmail.com>
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 * @author : awgymer
 */

@Slf4j
@CompileDynamic
class SamplesheetConverter {

    final private ValidationConfig config

    SamplesheetConverter(ValidationConfig configInput) {
        config = configInput
    }

    final private Set<String> unrecognisedHeaders = []
    private List<Map> rows = []
    private Map meta = [:]

    /*
    Convert the samplesheet to a list of entries based on a schema
    */
    List validateAndConvertToList(
        Path samplesheetFile,
        Path schemaFile
    ) {

        Map colors = getLogColors(config.monochromeLogs)

        // Some checks before validating
        if (!schemaFile.exists()) {
            String msg = "${colors.red}JSON schema file ${schemaFile} does not exist\n${colors.reset}\n"
            throw new SchemaValidationException(msg)
        }

        Map schemaMap = new JsonSlurper().parseText(schemaFile.text) as Map
        List<String> schemaKeys = schemaMap.keySet() as List<String>
        if (schemaKeys.contains('properties') || !schemaKeys.contains('items')) {
            /* groovylint-disable-next-line LineLength */
            String msg = "${colors.red}The schema for '${samplesheetFile}' (${schemaFile}) is not valid. Please make sure that 'items' is the top level keyword and not 'properties'\n${colors.reset}\n"
            throw new SchemaValidationException(msg)
        }

        if (!samplesheetFile.exists()) {
            String msg = "${colors.red}Samplesheet file ${samplesheetFile} does not exist\n${colors.reset}\n"
            throw new SchemaValidationException(msg)
        }

        // Validate
        JsonSchemaValidator validator = new JsonSchemaValidator(config)
        JSONArray samplesheet = fileToJson(samplesheetFile, schemaFile) as JSONArray
        ValidationResult validationResult = validator.validate(samplesheet, schemaFile.toString())
        List<String> validationErrors = validationResult.getErrors('field')
        if (validationErrors) {
            /* groovylint-disable-next-line LineLength */
            String msg = "${colors.red}The following errors have been detected in ${samplesheetFile}:\n\n" + validationErrors.join('\n').trim() + "\n${colors.reset}\n"
            log.error('Validation of samplesheet failed!')
            throw new SchemaValidationException(msg, validationErrors)
        }

        // Convert
        List samplesheetList = fileToObject(samplesheetFile, schemaFile) as List
        rows = []

        List channelFormat = samplesheetList.collect { entry ->
            resetMeta()
            Object result = formatEntry(entry, schemaMap['items'] as Map)
            if (isMeta()) {
                if (result in List) {
                    result.add(0, meta)
                } else {
                    result = [meta, result]
                }
            }
            return result
        }

        logUnrecognisedHeaders(samplesheetFile.toString())

        return channelFormat
    }

    private void resetMeta() {
        meta = [:]
    }

    private Boolean isMeta() {
        return meta.size() > 0
    }

    private void addUnrecognisedHeader(String header) {
        unrecognisedHeaders.add(header)
    }

    private void logUnrecognisedHeaders(String fileName) {
        if (unrecognisedHeaders.size() > 0) {
            String processedHeaders = unrecognisedHeaders.collect { header -> "\t- ${header}" }.join('\n')
            String msg = "Found the following unidentified headers in ${fileName}:\n${processedHeaders}\n" as String
            config.logging.unrecognisedHeaders.log(msg)
        }
    }

    /*
    This function processes an input value based on a schema.
    The output will be created for addition to the output channel.
    */
    private Object formatEntry(Object inputObject, Map schema, String headerPrefix = '') {
        // Add default values for missing entries
        Object input = inputObject != null ?
            inputObject :
            hasDeepKey(schema, 'default') ?
                findDeep(schema, 'default') :
                []

        if (input in Map) {
            List result = []
            Map properties = findDeep(schema, 'properties') as Map
            Set unusedKeys = input.keySet() - properties.keySet()

            // Check for properties in the samplesheet that have not been defined in the schema
            unusedKeys.each { key -> addUnrecognisedHeader("${headerPrefix}${key}" as String) }

            // Loop over every property to maintain the correct order
            properties.each { property, schemaValues ->
                Object value = input[property]
                List metaIds = schemaValues['meta'] in List ?
                    schemaValues['meta'] as List :
                    schemaValues['meta'] in String ?
                        [schemaValues['meta']] :
                        []
                String prefix = headerPrefix ? "${headerPrefix}${property}." : "${property}."

                // Add the value to the meta map if needed
                if (metaIds) {
                    metaIds.each { id ->
                        meta["${id}"] = processMeta(value, schemaValues as Map, prefix)
                    }
                }
                // return the correctly casted value
                else {
                    result.add(formatEntry(value, schemaValues as Map, prefix))
                }
            }
            return result
        } else if (input in List) {
            List result = []
            Integer count = 0
            input.each { entry ->
                // return the correctly casted value
                String prefix = headerPrefix ? "${headerPrefix}${count}." : "${count}."
                result.add(formatEntry(entry, findDeep(schema, 'items') as Map, prefix))
                count++
            }
            return result
        }
        // Cast value to path type if needed and return the value
        return processValue(input, schema)
    }

    final private List validPathFormats = ['file-path', 'path', 'directory-path', 'file-path-pattern']
    final private List schemaOptions = ['anyOf', 'oneOf', 'allOf']

    /*
    This function processes a value that's not a map or list and casts it to a file type if necessary.
    When there is uncertainty if the value should be a path, some simple logic is applied that tries
    to guess if it should be a file type
    */
    private Object processValue(Object value, Map schemaEntry) {
        if (!(value in String) || schemaEntry == null) {
            return value
        }

        String defaultFormat = schemaEntry.format ?: ''

        // A valid path format has been found in the schema
        Boolean foundStringFileFormat = false

        // Type string has been found without a valid path format
        Boolean foundStringNoFileFormat = false

        if ((schemaEntry.type ?: '') == 'string') {
            if (validPathFormats.contains(schemaEntry.format ?: defaultFormat)) {
                foundStringFileFormat = true
            } else {
                foundStringNoFileFormat = true
            }
        }

        schemaOptions.each { option ->
            schemaEntry[option]?.each { subSchema ->
                if ((subSchema['type'] ?: '' ) == 'string') {
                    if (validPathFormats.contains(subSchema['format'] ?: defaultFormat)) {
                        foundStringFileFormat = true
                    } else {
                        foundStringNoFileFormat = true
                    }
                }
            }
        }

        if (foundStringFileFormat && !foundStringNoFileFormat) {
            return Nextflow.file(value)
        } else if (foundStringFileFormat && foundStringNoFileFormat) {
            // Do a simple check if the object could be a path
            // This check looks for / in the filename or if a dot is
            // present in the last 7 characters (possibly indicating an extension)
            if (
                value.contains('/') ||
                (value.size() >= 7 && value[-7..-1].contains('.')) ||
                (value.size() < 7 && value.contains('.'))
            ) {
                return Nextflow.file(value)
            }
        }
        return value
    }

    /*
    This function processes an input value based on a schema.
    The output will be created for addition to the meta map.
    */
    private Object processMeta(Object inputObject, Map schema, String headerPrefix) {
        // Add default values for missing entries
        Object input = inputObject != null ?
            inputObject :
            hasDeepKey(schema, 'default') ?
                findDeep(schema, 'default') :
                []

        if (input in Map) {
            Map result = [:]
            Map properties = findDeep(schema, 'properties') as Map
            Set unusedKeys = input.keySet() - properties.keySet()
            // Check for properties in the samplesheet that have not been defined in the schema
            unusedKeys.each { key -> addUnrecognisedHeader("${headerPrefix}${key}" as String) }

            // Loop over every property to maintain the correct order
            properties.each { property, schemaValues ->
                Object value = input[property]
                String prefix = headerPrefix ? "${headerPrefix}${property}." : "${property}."
                result[property] = processMeta(value, schemaValues as Map, prefix)
            }
            return result
        } else if (input in List) {
            List result = []
            Integer count = 0
            input.each { entry ->
                // return the correctly casted value
                String prefix = headerPrefix ? "${headerPrefix}${count}." : "${count}."
                result.add(processMeta(entry, findDeep(schema, 'items') as Map, prefix))
                count++
            }
            return result
        }
        // Cast value to path type if needed and return the value
        return processValue(input, schema)
    }

}
