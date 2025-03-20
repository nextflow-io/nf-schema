package nextflow.validation.utils

import org.json.JSONObject

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
    // Cast a value to the provided type in a Strict mode
    //
    public static Object inferType(Object input) {
        def Set<String> validBooleanValues = ['true', 'false'] as Set

        if (input instanceof Map) {
            // Cast all values in the map
            def Map output = [:]
            input.each { k, v ->
                output[k] = inferType(v)
            }
            return output
        }
        else if (input instanceof List) {
            // Cast all values in the list
            def List output = []
            for( entry : input ) {
                output.add(inferType(entry))
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