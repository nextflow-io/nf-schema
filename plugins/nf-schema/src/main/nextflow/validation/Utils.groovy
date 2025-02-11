package nextflow.validation

import org.yaml.snakeyaml.Yaml
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONPointer
import org.json.JSONPointerException
import nextflow.Global

import groovy.json.JsonGenerator
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import java.nio.file.Path

/**
 * @author : mirpedrol <mirp.julia@gmail.com>
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 * @author : KevinMenden
 */

@Slf4j
public class Utils {

    // Function to infer the file type of a samplesheet
    public static String getFileType(Path file) {
        def String extension = file.getExtension()
        if (extension in ["csv", "tsv", "yml", "yaml", "json"]) {
            return extension == "yml" ? "yaml" : extension
        }

        def String header = getHeader(file)

        def Integer commaCount = header.count(",")
        def Integer tabCount = header.count("\t")

        if ( commaCount == tabCount ){
            log.error("Could not derive file type from ${file}. Please specify the file extension (CSV, TSV, YML, YAML and JSON are supported).".toString())
        }
        if ( commaCount > tabCount ){
            return "csv"
        }
        else {
            return "tsv"
        }
    }

    // Function to get the header from a CSV or TSV file
    public static String getHeader(Path file) {
        def String header
        file.withReader { header = it.readLine() }
        return header
    }

    // Converts a given file to a List
    public static Object fileToObject(Path file, Path schema) {
        def String fileType = Utils.getFileType(file)
        def String delimiter = fileType == "csv" ? "," : fileType == "tsv" ? "\t" : null
        def Map schemaMap = (Map) new JsonSlurper().parse( schema )
        def Map types = variableTypes(schema)

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
            def Boolean header = getValueFromJson("#/items/properties", new JSONObject(schema.text)) ? true : false
            def List fileContent = file.splitCsv(header:header, strip:true, sep:delimiter, quote:'\"')
            if (!header) {
                // Flatten no header inputs if they contain one value
                fileContent = fileContent.collect { it instanceof List && it.size() == 1 ? it[0] : it }
            }

            return castToType(fileContent)
        }
    }

    // Converts a given file to a JSONArray
    public static Object fileToJson(Path file, Path schema) {
        // Remove all null values from JSON object
        // and convert the groovy object to a JSONArray
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
    // Cast a value to the provided type in a Strict mode
    //

    public static Object castToType(Object input) {
        def Set<String> validBooleanValues = ['true', 'false'] as Set

        if (input instanceof Map) {
            // Cast all values in the map
            def Map output = [:]
            input.each { k, v ->
                output[k] = castToType(v)
            }
            return output
        }
        else if (input instanceof List) {
            // Cast all values in the list
            def List output = []
            for( entry : input ) {
                output.add(castToType(entry))
            }
            return output
        } else if (input instanceof String) {
            // Cast the string if there is one
            if (input == "") {
                return null
            }
            return JSONObject.stringToValue(input)
        }
    }

    // Resolve Schema path relative to main workflow directory
    public static String getSchemaPath(String baseDir, String schemaFilename) {
        if (Path.of(schemaFilename).exists()) {
            return schemaFilename
        } else {
            return "${baseDir}/${schemaFilename}"
        }
    }

    // Function to obtain the variable types of properties from a JSON Schema
    public static Map variableTypes(Path schema) {
        def Map variableTypes = [:]
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
                variableTypes[key] = type
            }
            else {
                variableTypes[key] = 'string' // If there isn't a type specified, return 'string' to avoid having a null value
            }
        }

        return variableTypes
    }

    // Function to check if a String value is an Integer
    public static Boolean isInteger(String input) {
        try {
            input as Integer
            return true
        } catch (NumberFormatException e) {
            return false
        }
    }

    // Function to check if a String value is a Float
    public static Boolean isFloat(String input) {
        try {
            input as Float
            return true
        } catch (NumberFormatException e) {
            return false
        }
    }

    // Function to check if a String value is a Double
    public static Boolean isDouble(String input) {
        try {
            input as Double
            return true
        } catch (NumberFormatException e) {
            return false
        }
    }

    // Function to get the value from a JSON pointer
    public static Object getValueFromJson(String jsonPointer, Object json) {
        def JSONPointer schemaPointer = new JSONPointer(jsonPointer)
        try {
            return schemaPointer.queryFrom(json) ?: ""
        } catch (JSONPointerException e) {
            return ""
        }
    }

    //
    // ANSII Colours used for terminal logging
    //
    public static Map logColours(Boolean monochrome_logs) {
        Map colorcodes = [:]

        // Reset / Meta
        colorcodes['reset']      = monochrome_logs ? '' : "\033[0m"
        colorcodes['bold']       = monochrome_logs ? '' : "\033[1m"
        colorcodes['dim']        = monochrome_logs ? '' : "\033[2m"
        colorcodes['underlined'] = monochrome_logs ? '' : "\033[4m"
        colorcodes['blink']      = monochrome_logs ? '' : "\033[5m"
        colorcodes['reverse']    = monochrome_logs ? '' : "\033[7m"
        colorcodes['hidden']     = monochrome_logs ? '' : "\033[8m"

        // Regular Colors
        colorcodes['black']      = monochrome_logs ? '' : "\033[0;30m"
        colorcodes['red']        = monochrome_logs ? '' : "\033[0;31m"
        colorcodes['green']      = monochrome_logs ? '' : "\033[0;32m"
        colorcodes['yellow']     = monochrome_logs ? '' : "\033[0;33m"
        colorcodes['blue']       = monochrome_logs ? '' : "\033[0;34m"
        colorcodes['purple']     = monochrome_logs ? '' : "\033[0;35m"
        colorcodes['cyan']       = monochrome_logs ? '' : "\033[0;36m"
        colorcodes['white']      = monochrome_logs ? '' : "\033[0;37m"

        // Bold
        colorcodes['bblack']     = monochrome_logs ? '' : "\033[1;30m"
        colorcodes['bred']       = monochrome_logs ? '' : "\033[1;31m"
        colorcodes['bgreen']     = monochrome_logs ? '' : "\033[1;32m"
        colorcodes['byellow']    = monochrome_logs ? '' : "\033[1;33m"
        colorcodes['bblue']      = monochrome_logs ? '' : "\033[1;34m"
        colorcodes['bpurple']    = monochrome_logs ? '' : "\033[1;35m"
        colorcodes['bcyan']      = monochrome_logs ? '' : "\033[1;36m"
        colorcodes['bwhite']     = monochrome_logs ? '' : "\033[1;37m"

        // Underline
        colorcodes['ublack']     = monochrome_logs ? '' : "\033[4;30m"
        colorcodes['ured']       = monochrome_logs ? '' : "\033[4;31m"
        colorcodes['ugreen']     = monochrome_logs ? '' : "\033[4;32m"
        colorcodes['uyellow']    = monochrome_logs ? '' : "\033[4;33m"
        colorcodes['ublue']      = monochrome_logs ? '' : "\033[4;34m"
        colorcodes['upurple']    = monochrome_logs ? '' : "\033[4;35m"
        colorcodes['ucyan']      = monochrome_logs ? '' : "\033[4;36m"
        colorcodes['uwhite']     = monochrome_logs ? '' : "\033[4;37m"

        // High Intensity
        colorcodes['iblack']     = monochrome_logs ? '' : "\033[0;90m"
        colorcodes['ired']       = monochrome_logs ? '' : "\033[0;91m"
        colorcodes['igreen']     = monochrome_logs ? '' : "\033[0;92m"
        colorcodes['iyellow']    = monochrome_logs ? '' : "\033[0;93m"
        colorcodes['iblue']      = monochrome_logs ? '' : "\033[0;94m"
        colorcodes['ipurple']    = monochrome_logs ? '' : "\033[0;95m"
        colorcodes['icyan']      = monochrome_logs ? '' : "\033[0;96m"
        colorcodes['iwhite']     = monochrome_logs ? '' : "\033[0;97m"

        // Bold High Intensity
        colorcodes['biblack']    = monochrome_logs ? '' : "\033[1;90m"
        colorcodes['bired']      = monochrome_logs ? '' : "\033[1;91m"
        colorcodes['bigreen']    = monochrome_logs ? '' : "\033[1;92m"
        colorcodes['biyellow']   = monochrome_logs ? '' : "\033[1;93m"
        colorcodes['biblue']     = monochrome_logs ? '' : "\033[1;94m"
        colorcodes['bipurple']   = monochrome_logs ? '' : "\033[1;95m"
        colorcodes['bicyan']     = monochrome_logs ? '' : "\033[1;96m"
        colorcodes['biwhite']    = monochrome_logs ? '' : "\033[1;97m"

        return colorcodes
    }

    public static String removeColors(String input) {
        if (!input) {return input}
        String output = input
        List colors = logColours(false).collect { it.value }
        colors.each { color ->
            output = output.replace(color, "")
        }
        return output
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

    //
    // Get maximum number of characters across all parameter names
    //
    public static Integer paramsMaxChars( Map paramsMap ) {
        return Collections.max(paramsMap.collect { _, val -> 
            def Map groupParams = val as Map
            longestStringLength(groupParams.keySet() as List<String> )
        })
    }

    //
    // Get the size of the longest string value in a list of strings
    //
    public static Integer longestStringLength( List<String> strings ) {
        return strings ? Collections.max(strings.collect { it.size() }) : 0
    }
}