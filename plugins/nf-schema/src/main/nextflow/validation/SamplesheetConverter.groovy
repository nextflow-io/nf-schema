package nextflow.validation

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import java.nio.file.Path

import org.json.JSONArray

import nextflow.Nextflow

/**
 * @author : mirpedrol <mirp.julia@gmail.com>
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 * @author : awgymer
 */

@Slf4j
@CompileStatic
class SamplesheetConverter {

    private static ValidationConfig config

    SamplesheetConverter(ValidationConfig config) {
        this.config = config
    }

    private static List<Map> rows = []
    private static Map meta = [:]

    private static Map getMeta() {
        this.meta
    }

    private static Map resetMeta() {
        this.meta = [:]
    }

    private static addMeta(Map newEntries) {
        this.meta = this.meta + newEntries
    }

    private static Boolean isMeta() {
        this.meta.size() > 0
    }

    private static List unrecognisedHeaders = []

    private static addUnrecognisedHeader (String header) {
        this.unrecognisedHeaders.add(header)
    }

    private static logUnrecognisedHeaders(String fileName) {
        log.info("HEADERS: ${this.unrecognisedHeaders}" as String)
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
    public static List validateAndConvertToList(
        Path samplesheetFile,
        Path schemaFile,
        Map options
    ) {

        def colors = Utils.logColours(config.monochromeLogs)

        // Some checks before validating
        if(!schemaFile.exists()) {
            def msg = "${colors.red}JSON schema file ${schemaFile.toString()} does not exist\n${colors.reset}\n"
            throw new SchemaValidationException(msg)
        }

        if(!samplesheetFile.exists()) {
            def msg = "${colors.red}Samplesheet file ${samplesheetFile.toString()} does not exist\n${colors.reset}\n"
            throw new SchemaValidationException(msg)
        }

        // Validate
        final validator = new JsonSchemaValidator(config)
        def JSONArray samplesheet = Utils.fileToJsonArray(samplesheetFile, schemaFile)
        def List<String> validationErrors = validator.validate(samplesheet, schemaFile.text)
        if (validationErrors) {
            def msg = "${colors.red}The following errors have been detected in ${samplesheetFile.toString()}:\n\n" + validationErrors.join('\n').trim() + "\n${colors.reset}\n"
            log.error("Validation of samplesheet failed!")
            throw new SchemaValidationException(msg, validationErrors)
        }

        // Convert
        def LinkedHashMap schemaMap = new JsonSlurper().parseText(schemaFile.text) as LinkedHashMap
        def List samplesheetList = Utils.fileToList(samplesheetFile, schemaFile)

        this.rows = []

        def List channelFormat = samplesheetList.collect { entry ->
            resetMeta()
            def Object result = formatEntry(entry, schemaMap["items"] as LinkedHashMap)
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
    private static Object formatEntry(Object input, LinkedHashMap schema, String headerPrefix = "") {

        // Add default values for missing entries
        input = input != null ? input : schema.containsKey("default") ? schema.default : []

        if (input instanceof Map) {
            def List result = []
            def Map properties = schema["properties"] as Map
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
                        meta["${it}"] = processMeta(value, schemaValues as LinkedHashMap, prefix)
                    }
                } 
                // return the correctly casted value
                else {
                    result.add(formatEntry(value, schemaValues as LinkedHashMap, prefix))
                }
            }
            return result
        } else if (input instanceof List) {
            def List result = []
            def Integer count = 0
            input.each {
                // return the correctly casted value
                def String prefix = headerPrefix ? "${headerPrefix}${count}." : "${count}."
                result.add(formatEntry(it, schema["items"] as LinkedHashMap, prefix))
                count++
            }
            return result
        } else {
            // Cast value to path type if needed and return the value
            return processValue(input, schema)
        }

    }

    private static List validPathFormats = ["file-path", "path", "directory-path", "file-path-pattern"]
    private static List schemaOptions = ["anyOf", "oneOf", "allOf"]

    /*
    This function processes a value that's not a map or list and casts it to a file type if necessary.
    When there is uncertainty if the value should be a path, some simple logic is applied that tries
    to guess if it should be a file type
    */
    private static Object processValue(Object value, Map schemaEntry) {
        if(!(value instanceof String)) {
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
    private static Object processMeta(Object input, LinkedHashMap schema, String headerPrefix) {
        // Add default values for missing entries
        input = input != null ? input : schema.containsKey("default") ? schema.default : []

        if (input instanceof Map) {
            def Map result = [:]
            def Map properties = schema["properties"] as Map
            def Set unusedKeys = input.keySet() - properties.keySet()
            
            // Check for properties in the samplesheet that have not been defined in the schema
            unusedKeys.each{addUnrecognisedHeader("${headerPrefix}${it}" as String)}

            // Loop over every property to maintain the correct order
            properties.each { property, schemaValues ->
                def value = input[property]
                def String prefix = headerPrefix ? "${headerPrefix}${property}." : "${property}."
                result[property] = processMeta(value, schemaValues as LinkedHashMap, prefix)
            }
            return result
        } else if (input instanceof List) {
            def List result = []
            def Integer count = 0
            input.each {
                // return the correctly casted value
                def String prefix = headerPrefix ? "${headerPrefix}${count}." : "${count}."
                result.add(processMeta(it, schema["items"] as LinkedHashMap, prefix))
                count++
            }
            return result
        } else {
            // Cast value to path type if needed and return the value
            return processValue(input, schema)
        }
    }

}
