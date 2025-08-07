package nextflow.validation.samplesheet

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import java.nio.file.Path

import org.json.JSONArray

import nextflow.Nextflow

import static nextflow.validation.utils.Colors.getLogColors
import static nextflow.validation.utils.Files.fileToJson
import static nextflow.validation.utils.Files.fileToObject
import static nextflow.validation.utils.Common.findDeep
import static nextflow.validation.utils.Common.hasDeepKey
import nextflow.validation.config.ValidationConfig
import nextflow.validation.exceptions.SchemaValidationException
import nextflow.validation.validators.JsonSchemaValidator

/**
 * @author : mirpedrol <mirp.julia@gmail.com>
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 * @author : awgymer
 */

@Slf4j
class SamplesheetConverter {

    private ValidationConfig config

    SamplesheetConverter(ValidationConfig config) {
        this.config = config
    }

    private List<Map> rows = []
    private Map meta = [:]

    private Map getMeta() {
        this.meta
    }

    private Map resetMeta() {
        this.meta = [:]
    }

    private addMeta(Map newEntries) {
        this.meta = this.meta + newEntries
    }

    private Boolean isMeta() {
        this.meta.size() > 0
    }

    private List unrecognisedHeaders = []

    private addUnrecognisedHeader (String header) {
        this.unrecognisedHeaders.add(header)
    }

    private logUnrecognisedHeaders(String fileName) {
        def Set unrecognisedHeaders = this.unrecognisedHeaders as Set
        if(unrecognisedHeaders.size() > 0) {
            def String processedHeaders = unrecognisedHeaders.collect { "\t- ${it}" }.join("\n")
            def String msg = "Found the following unidentified headers in ${fileName}:\n${processedHeaders}\n" as String
            if( config.failUnrecognisedHeaders ) {
                throw new SchemaValidationException(msg)
            } else {
                log.warn(msg)
            }
        }
    }

    /*
    Convert the samplesheet to a list of entries based on a schema
    */
    public List validateAndConvertToList(
        Path samplesheetFile,
        Path schemaFile,
        Map options
    ) {

        def colors = getLogColors(config.monochromeLogs)

        // Some checks before validating
        if(!schemaFile.exists()) {
            def msg = "${colors.red}JSON schema file ${schemaFile.toString()} does not exist\n${colors.reset}\n"
            throw new SchemaValidationException(msg)
        }

        def Map schemaMap = new JsonSlurper().parseText(schemaFile.text) as Map
        def List<String> schemaKeys = schemaMap.keySet() as List<String>
        if(schemaKeys.contains("properties") || !schemaKeys.contains("items")) {
            def msg = "${colors.red}The schema for '${samplesheetFile.toString()}' (${schemaFile.toString()}) is not valid. Please make sure that 'items' is the top level keyword and not 'properties'\n${colors.reset}\n"
            throw new SchemaValidationException(msg)
        }

        if(!samplesheetFile.exists()) {
            def msg = "${colors.red}Samplesheet file ${samplesheetFile.toString()} does not exist\n${colors.reset}\n"
            throw new SchemaValidationException(msg)
        }

        // Validate
        final validator = new JsonSchemaValidator(config)
        def JSONArray samplesheet = fileToJson(samplesheetFile, schemaFile) as JSONArray
        def Tuple2<List<String>,List<String>> validationResults = validator.validate(samplesheet, schemaFile.text)
        def validationErrors = validationResults[0]
        if (validationErrors) {
            def msg = "${colors.red}The following errors have been detected in ${samplesheetFile.toString()}:\n\n" + validationErrors.join('\n').trim() + "\n${colors.reset}\n"
            log.error("Validation of samplesheet failed!")
            throw new SchemaValidationException(msg, validationErrors)
        }

        // Convert
        def List samplesheetList = fileToObject(samplesheetFile, schemaFile) as List
        this.rows = []

        def List channelFormat = samplesheetList.collect { entry ->
            resetMeta()
            def Object result = formatEntry(entry, schemaMap["items"] as Map)
            if(isMeta()) {
                if(result instanceof List) {
                    result.add(0,getMeta())
                } else {
                    result = [getMeta(), result]
                }
            }
            return result
        }

        logUnrecognisedHeaders(samplesheetFile.toString())

        return channelFormat

    }

    /*
    This function processes an input value based on a schema. 
    The output will be created for addition to the output channel.
    */
    private Object formatEntry(Object input, Map schema, String headerPrefix = "") {

        // Add default values for missing entries
        input = input != null ? input : hasDeepKey(schema, "default") ? findDeep(schema, "default") : []

        if (input instanceof Map) {
            def List result = []
            def Map properties = findDeep(schema, "properties") as Map
            def Set unusedKeys = input.keySet() - properties.keySet()
            
            // Check for properties in the samplesheet that have not been defined in the schema
            unusedKeys.each{addUnrecognisedHeader("${headerPrefix}${it}" as String)}

            // Loop over every property to maintain the correct order
            properties.each { property, schemaValues ->
                def value = input[property]
                def List metaIds = schemaValues["meta"] instanceof List ? schemaValues["meta"] as List : schemaValues["meta"] instanceof String ? [schemaValues["meta"]] : []
                def String prefix = headerPrefix ? "${headerPrefix}${property}." : "${property}."
                
                // Add the value to the meta map if needed
                if (metaIds) {
                    metaIds.each {
                        meta["${it}"] = processMeta(value, schemaValues as Map, prefix)
                    }
                } 
                // return the correctly casted value
                else {
                    result.add(formatEntry(value, schemaValues as Map, prefix))
                }
            }
            return result
        } else if (input instanceof List) {
            def List result = []
            def Integer count = 0
            input.each {
                // return the correctly casted value
                def String prefix = headerPrefix ? "${headerPrefix}${count}." : "${count}."
                result.add(formatEntry(it, findDeep(schema, "items") as Map, prefix))
                count++
            }
            return result
        } else {
            // Cast value to path type if needed and return the value
            return processValue(input, schema)
        }

    }

    private List validPathFormats = ["file-path", "path", "directory-path", "file-path-pattern"]
    private List schemaOptions = ["anyOf", "oneOf", "allOf"]

    /*
    This function processes a value that's not a map or list and casts it to a file type if necessary.
    When there is uncertainty if the value should be a path, some simple logic is applied that tries
    to guess if it should be a file type
    */
    private Object processValue(Object value, Map schemaEntry) {
        if(!(value instanceof String) || schemaEntry == null) {
            return value
        }

        def String defaultFormat = schemaEntry.format ?: ""

        // A valid path format has been found in the schema
        def Boolean foundStringFileFormat = false

        // Type string has been found without a valid path format
        def Boolean foundStringNoFileFormat = false

        if ((schemaEntry.type ?: "") == "string") {
            if (validPathFormats.contains(schemaEntry.format ?: defaultFormat)) {
                foundStringFileFormat = true
            } else {
                foundStringNoFileFormat = true
            }
        }

        schemaOptions.each { option ->
            schemaEntry[option]?.each { subSchema ->
                if ((subSchema["type"] ?: "" ) == "string") {
                    if (validPathFormats.contains(subSchema["format"] ?: defaultFormat)) {
                        foundStringFileFormat = true
                    } else {
                        foundStringNoFileFormat = true
                    }
                }
            }
        }

        if(foundStringFileFormat && !foundStringNoFileFormat) {
            return Nextflow.file(value)
        } else if(foundStringFileFormat && foundStringNoFileFormat) {
            // Do a simple check if the object could be a path
            // This check looks for / in the filename or if a dot is
            // present in the last 7 characters (possibly indicating an extension)
            if(
                value.contains("/") || 
                (value.size() >= 7 && value[-7..-1].contains(".")) || 
                (value.size() < 7 && value.contains("."))
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
    private Object processMeta(Object input, Map schema, String headerPrefix) {
        // Add default values for missing entries
        input = input != null ? input : hasDeepKey(schema, "default") ? findDeep(schema, "default") : []

        if (input instanceof Map) {
            def Map result = [:]
            def Map properties = findDeep(schema, "properties") as Map
            def Set unusedKeys = input.keySet() - properties.keySet()
            
            // Check for properties in the samplesheet that have not been defined in the schema
            unusedKeys.each{addUnrecognisedHeader("${headerPrefix}${it}" as String)}

            // Loop over every property to maintain the correct order
            properties.each { property, schemaValues ->
                def value = input[property]
                def String prefix = headerPrefix ? "${headerPrefix}${property}." : "${property}."
                result[property] = processMeta(value, schemaValues as Map, prefix)
            }
            return result
        } else if (input instanceof List) {
            def List result = []
            def Integer count = 0
            input.each {
                // return the correctly casted value
                def String prefix = headerPrefix ? "${headerPrefix}${count}." : "${count}."
                result.add(processMeta(it, findDeep(schema, "items") as Map, prefix))
                count++
            }
            return result
        } else {
            // Cast value to path type if needed and return the value
            return processValue(input, schema)
        }
    }

}
