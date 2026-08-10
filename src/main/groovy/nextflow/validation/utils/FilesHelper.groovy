package nextflow.validation.utils

import static nextflow.validation.utils.Common.getValueFromJsonPointer
import static nextflow.validation.utils.Types.inferType

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.LoaderOptions
import org.json.JSONArray
import org.json.JSONObject

import groovy.json.JsonGenerator
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import groovy.transform.CompileDynamic
import java.nio.file.Path
import java.nio.file.Files

import nextflow.validation.exceptions.SchemaValidationException

/**
 * A collection of functions used to get data from files
 *
 * @author : mirpedrol <mirp.julia@gmail.com>
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 * @author : KevinMenden
 */

@Slf4j
@CompileDynamic
public class FilesHelper {

    //
    // Function to detect if a file is a CSV, TSV, JSON or YAML file
    //
    static String getFileType(Path file) {
        String extension = file.extension
        if (extension in ['csv', 'tsv', 'yml', 'yaml', 'json']) {
            return extension == 'yml' ? 'yaml' : extension
        }

        String header = getFileHeader(file)

        Integer commaCount = header.count(',')
        Integer tabCount = header.count('\t')

        if (commaCount == tabCount) {
            /* groovylint-disable-next-line LineLength */
            log.error("Could not derive file type from ${file}. Please specify the file extension (CSV, TSV, YML, YAML and JSON are supported).".toString())
        }
        return commaCount > tabCount ? 'csv' : 'tsv'
    }

    //
    // Function to get the header from a CSV or TSV file
    //
    static String getFileHeader(Path file) {
        String header
        file.withReader { line -> header = line.readLine() }
        return header
    }

    //
    // Converts a given file to an Groovy object (either a List or a Map)
    //
    static Object fileToObject(Path file, Path schema) {
        String fileType = getFileType(file)
        String delimiter = fileType == 'csv' ? ',' : fileType == 'tsv' ? '\t' : null
        Map schemaMap = (Map) new JsonSlurper().parse(schema)
        Map types = getSchemaTypes(schema)
        List<String> separatedFileTypes = ['csv', 'tsv']
        if (schemaMap.type == 'object' && fileType in separatedFileTypes) {
            /* groovylint-disable-next-line LineLength */
            String msg = "CSV or TSV files are not supported. Use a JSON or YAML file instead of ${file}. (Expected a non-list data structure, which is not supported in CSV or TSV)"
            throw new SchemaValidationException(msg, [])
        }

        if (
            (types.find { type -> type.value == 'array' || type.value == 'object' } as Boolean) &&
            fileType in separatedFileTypes
        ) {
            /* groovylint-disable-next-line LineLength */
            String msg = "Using \"type\": \"array\" or \"type\": \"object\" in schema with a \".$fileType\" samplesheet is not supported\n"
            log.error('ERROR: Validation of pipeline parameters failed!')
            throw new SchemaValidationException(msg)
        }

        if (fileType == 'yaml') {
            LoaderOptions yamlLoaderOptions = new LoaderOptions()
            yamlLoaderOptions.codePointLimit = 50 * 1024 * 1024
            return new Yaml(yamlLoaderOptions).load((file.text))
        }
        else if (fileType == 'json') {
            return new JsonSlurper().parseText(file.text)
        }
        Boolean header = getValueFromJsonPointer('#/items/properties', new JSONObject(schema.text)) ? true : false
        Path cleanFile = header ? sanitize(file) : file
        List fileContent = cleanFile.splitCsv(header:header, strip:true, sep:delimiter, quote:'\"')
        if (!header) {
            // Flatten no header inputs if they contain one value
            fileContent = fileContent.collect { cont -> cont in List && cont.size() == 1 ? cont[0] : cont }
        }

        return inferType(fileContent)
    }

    //
    // Converts a given file to a JSON type (either JSONArray or JSONObject)
    //
    static Object fileToJson(Path file, Path schema) {
        // Remove all null values from JSON object
        JsonGenerator jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()
        Object obj = fileToObject(file, schema)
        if (obj in List) {
            return new JSONArray(jsonGenerator.toJson(obj))
        } else if (obj in Map) {
            return new JSONObject(jsonGenerator.toJson(obj))
        }
        String msg = 'Could not determine if the file is a list or map of values'
        throw new SchemaValidationException(msg, [])
    }

    //
    // Get a map that contains the type for each key in a JSON schema file
    //
    static Map getSchemaTypes(Path schema) {
        Map types = [:]
        String type = ''

        // Read the schema
        JsonSlurper slurper = new JsonSlurper()
        Map parsed = (Map) slurper.parse(schema)

        // Obtain the type of each variable in the schema
        Map properties = (Map) parsed['items'] ? parsed['items']['properties'] : parsed['properties']
        properties.each { p ->
            String key = (String) p.key
            Map property = properties[key] as Map
            if (property.containsKey('type')) {
                if (property['type'] == 'number') {
                    type = 'float'
                }
                else {
                    type = property['type']
                }
                types[key] = type
            }
            else {
                types[key] = 'string' // If there isn't a type specified, return 'string' to avoid having a null value
            }
        }

        return types
    }

    //
    // This function tries to read a JSON params file
    //
    static Map paramsLoad(Path jsonSchema) {
        Map paramsMap = [:]
        try {
            paramsMap = paramsRead(jsonSchema)
        } catch (e) {
            println "Could not read parameters settings from JSON. $e"
        }
        return paramsMap
    }

    //
    // Sanitizes a CSV or TSV file by removing all trailing commas or tabs in the header
    //
    private static Path sanitize(Path file) {
        // Check if sanitization is needed
        Reader reader = file.newReader()
        String firstLine = reader.readLine()
        reader.close()
        if (!firstLine.endsWith(',') && !firstLine.endsWith('\t')) {
            // No sanitization needed
            return file
        }
        Path tempFile = Files.createTempFile('sanitized_', file.fileName.toString())
        tempFile.toFile().deleteOnExit()

        PrintWriter writer = new PrintWriter(tempFile.toFile())
        file.withReader { readerObj ->
            // Remove trailing commas or tabs from the line
            while (true) {
                String line = readerObj.readLine()
                if (!line) {
                    break
                }
                String sanitizedLine = line.replaceAll('[,\\t]*\$', '')
                writer.println(sanitizedLine)
            }
        }
        writer.close()
        return tempFile
    }

    //
    // Method to actually read in JSON file using Groovy.
    // Group (as Key), values are all parameters
    //    - Parameter1 as Key, Description as Value
    //    - Parameter2 as Key, Description as Value
    //    ....
    // Group
    //    -
    private static Map paramsRead(Path jsonSchema) throws Exception {
        JsonSlurper slurper = new JsonSlurper()
        Map schema = (Map) slurper.parse(jsonSchema)
        // $defs is the adviced keyword for definitions. Keeping defs in for backwards compatibility
        Map schemaDefs = (Map) (schema.get('$defs') ?: schema.get('defs'))
        Map schemaProperties = (Map) schema.get('properties')
        /* Tree looks like this in nf-core schema
        * $defs <- this is what the first get('$defs') gets us
                group 1
                    title
                    description
                        properties
                        parameter 1
                            type
                            description
                        parameter 2
                            type
                            description
                group 2
                    title
                    description
                        properties
                        parameter 1
                            type
                            description
        * properties <- parameters can also be ungrouped, outside of $defs
                parameter 1
                    type
                    description
        */

        Map paramsMap = [:]
        // Grouped params
        if (schemaDefs) {
            schemaDefs.each { String name, Map group ->
                Map groupProperty = (Map) group.get('properties') // Gets the property object of the group
                String title = (String) group.get('title') ?: name
                Map subParams = [:]
                groupProperty.each { innerkey, value ->
                    subParams.put(innerkey, value)
                }
                paramsMap.put(title, subParams)
            }
        }

        // Ungrouped params
        if (schemaProperties) {
            Map ungroupedParams = [:]
            schemaProperties.each { innerkey, value ->
                ungroupedParams.put(innerkey, value)
            }
            paramsMap.put('Other parameters', ungroupedParams)
        }

        return paramsMap
    }

}
