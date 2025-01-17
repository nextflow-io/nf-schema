package nextflow.validation.utils

import org.yaml.snakeyaml.Yaml
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONPointer

import groovy.json.JsonGenerator
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import java.nio.file.Path
import java.nio.file.NoSuchFileException

import nextflow.validation.exceptions.SchemaValidationException
import static nextflow.validation.utils.Common.getValueFromJsonPointer
import static nextflow.validation.utils.Types.inferType

/**
 * A collection of functions used to get data from files
 *
 * @author : mirpedrol <mirp.julia@gmail.com>
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 * @author : KevinMenden
 */

@Slf4j
public class Files {

    //
    // Function to detect if a file is a CSV, TSV, JSON or YAML file
    //
    public static String getFileType(Path file) {
        def String extension = file.getExtension()
        if (extension in ["csv", "tsv", "yml", "yaml", "json"]) {
            return extension == "yml" ? "yaml" : extension
        }

        def Integer commaCount = 0
        def Integer tabCount = 0
        try {
            def String header = getFileHeader(file)
            commaCount = header.count(",")
            tabCount = header.count("\t")
        } catch (NoSuchFileException e) {
            log.debug("${file.toString()} does not exist, cannot infer file type from file content.")
        }

        if ( commaCount == tabCount ){
            throw new SchemaValidationException("Could not derive file type from ${file}. Please specify the file extension (CSV, TSV, YML, YAML and JSON are supported).".toString())
        }
        if ( commaCount > tabCount ){
            return "csv"
        }
        else {
            return "tsv"
        }
    }

    //
    // Function to get the header from a CSV or TSV file
    //
    public static String getFileHeader(Path file) {
        def String header
        file.withReader { header = it.readLine() }
        return header
    }

    //
    // Converts a given file to an Groovy object (either a List or a Map)
    //
    public static Object fileToObject(Path file, Path schema) {
        def String fileType = getFileType(file)
        def String delimiter = fileType == "csv" ? "," : fileType == "tsv" ? "\t" : null
        def Map schemaMap = (Map) new JsonSlurper().parse( schema )
        def Map types = getSchemaTypes(schema)

        if (schemaMap.type == "object" && fileType in ["csv", "tsv"]) {
            def msg = "CSV or TSV files are not supported. Use a JSON or YAML file instead of ${file.toString()}. (Expected a non-list data structure, which is not supported in CSV or TSV)"
            throw new SchemaValidationException(msg, [])
        }

        if ((types.find{ it.value == "array" || it.value == "object" } as Boolean) && fileType in ["csv", "tsv"]){
            def msg = "Using \"type\": \"array\" or \"type\": \"object\" in schema with a \".$fileType\" samplesheet is not supported\n"
            log.error("ERROR: Validation of pipeline parameters failed!")
            throw new SchemaValidationException(msg, [])
        }

        if(fileType == "yaml"){
            return new Yaml().load((file.text))
        }
        else if(fileType == "json"){
            return new JsonSlurper().parseText(file.text)
        }
        else {
            def Boolean header = getValueFromJsonPointer("#/items/properties", new JSONObject(schema.text)) ? true : false
            def List fileContent = file.splitCsv(header:header, strip:true, sep:delimiter, quote:'\"')
            if (!header) {
                // Flatten no header inputs if they contain one value
                fileContent = fileContent.collect { it instanceof List && it.size() == 1 ? it[0] : it }
            }

            return inferType(fileContent)
        }
    }

    //
    // Converts a given file to a JSON type (either JSONArray or JSONObject)
    //
    public static Object fileToJson(Path file, Path schema) {
        // Remove all null values from JSON object
        def jsonGenerator = new JsonGenerator.Options()
            .excludeNulls()
            .build()
        def Object obj = fileToObject(file, schema)
        if (obj instanceof List) {
            return new JSONArray(jsonGenerator.toJson(obj))
        } else if (obj instanceof Map) {
            return new JSONObject(jsonGenerator.toJson(obj))
        } else {
            def msg = "Could not determine if the file is a list or map of values"
            throw new SchemaValidationException(msg, [])
        }
    }

    //
    // Get a map that contains the type for each key in a JSON schema file
    //
    public static Map getSchemaTypes(Path schema) {
        def Map types = [:]
        def String type = ''

        // Read the schema
        def slurper = new JsonSlurper()
        def Map parsed = (Map) slurper.parse( schema )

        // Obtain the type of each variable in the schema
        def Map properties = (Map) parsed['items'] ? parsed['items']['properties'] : parsed["properties"]
        for (p in properties) {
            def String key = (String) p.key
            def Map property = properties[key] as Map
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
    public static Map paramsLoad(Path json_schema) {
        def paramsMap = [:]
        try {
            paramsMap = paramsRead(json_schema)
        } catch (Exception e) {
            println "Could not read parameters settings from JSON. $e"
        }
        return paramsMap
    }

    //
    // Method to actually read in JSON file using Groovy.
    // Group (as Key), values are all parameters
    //    - Parameter1 as Key, Description as Value
    //    - Parameter2 as Key, Description as Value
    //    ....
    // Group
    //    -
    private static Map paramsRead(Path json_schema) throws Exception {
        def slurper = new JsonSlurper()
        def Map schema = (Map) slurper.parse( json_schema )
        // $defs is the adviced keyword for definitions. Keeping defs in for backwards compatibility
        def Map schema_defs = (Map) (schema.get('$defs') ?: schema.get("defs"))
        def Map schema_properties = (Map) schema.get('properties')
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

        def paramsMap = [:]
        // Grouped params
        if (schema_defs) {
            schema_defs.each { String name, Map group ->
                def Map group_property = (Map) group.get('properties') // Gets the property object of the group
                def String title = (String) group.get('title') ?: name
                def sub_params = [:]
                group_property.each { innerkey, value ->
                    sub_params.put(innerkey, value)
                }
                paramsMap.put(title, sub_params)
            }
        }

        // Ungrouped params
        if (schema_properties) {
            def ungrouped_params = [:]
            schema_properties.each { innerkey, value ->
                ungrouped_params.put(innerkey, value)
            }
            paramsMap.put("Other parameters", ungrouped_params)
        }

        return paramsMap
    }
}