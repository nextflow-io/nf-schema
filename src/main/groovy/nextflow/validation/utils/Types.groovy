package nextflow.validation.utils

import org.json.JSONObject
import org.apache.groovy.json.internal.LazyMap

import groovy.util.logging.Slf4j

/**
 * A collection of functions related to type casting and type checking
 *
 * @author : mirpedrol <mirp.julia@gmail.com>
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 * @author : KevinMenden
 */

@Slf4j
public class Types {

    //
    // This function converts a value to the correct type based on the schema.
    //
    public static Object castToType(Object value, Map schemaMap) {
        if(value == null) {
            return null
        }
        if (value instanceof Map) {
            // Cast all values in the map
            def Map output = [:]
            value.each { k, v ->
                output[k] = castToType(v, schemaMap.get("properties", null).get(k, null))
            }
            return output
        }
        else if (value instanceof List) {
            // Cast all values in the list
            def List output = []
            value.eachWithIndex { entry, index ->
                output.add(castToType(entry, schemaMap.get("items", null)))
            }
            return output
        } else if (value instanceof String) {
            // Cast the string if there is one
            if (value == "") {
                return null
            }
            if(schemaMap == null) {
                return JSONObject.stringToValue(value)
            }
            def Set<String> types = getTypes(schemaMap as Map)
            if (types.size() == 0) {
                return JSONObject.stringToValue(value)
            }
            return castString(value, types, schemaMap)
        }
    }

    //
    // Cast a string to the correct type based on the schema
    //
    private Object castString(String value, Set<String> types, Map schema) {
        if(types.size() == 1) {
            switch(types[0]) {
                case "string":
                    return value
                    break
                case "integer":
                    return value as Integer
                    break
                case "number":
                    return value as Float
                    break
                case "null":
                    return null
                    break
                case "boolean":
                    return value.toLowerCase() as Boolean
                    break
            }
        } else {
            // TODO implement validation based casting
            return JSONObject.stringToValue(value)
        }
    }

    //
    // This function retrieves all possible types from a schema
    //
    private Set<String> getTypes(Object schema) {
        def Set<String> types = []
        if (!(schema instanceof Map || schema instanceof LazyMap)) {
            return types
        }
        if (schema.containsKey("type")) {
            types.add(schema["type"])
        }
        ["anyOf", "allOf", "oneOf"].each { allowedKey ->
            if(schema.containsKey(allowedKey)) {
                schema[allowedKey].each { subSchema ->
                    types.addAll(getTypes(subSchema))
                }
            }
        }
        return (types.flatten() - "array") - "object" as Set<String>
    }

    //
    // Function to check if a String value is an Integer
    //
    public static Boolean isInteger(String input) {
        try {
            input as Integer
            return true
        } catch (NumberFormatException e) {
            return false
        }
    }

    //
    // Function to check if a String value is a Float
    //
    public static Boolean isFloat(String input) {
        try {
            input as Float
            return true
        } catch (NumberFormatException e) {
            return false
        }
    }

    //
    // Function to check if a String value is a Double
    //
    public static Boolean isDouble(String input) {
        try {
            input as Double
            return true
        } catch (NumberFormatException e) {
            return false
        }
    }
}