package nextflow.validation.utils

import org.json.JSONObject

import groovy.util.logging.Slf4j
import groovy.transform.CompileDynamic

/**
 * A collection of functions related to type casting and type checking
 *
 * @author : mirpedrol <mirp.julia@gmail.com>
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 * @author : KevinMenden
 */

@Slf4j
@CompileDynamic
public class Types {

    //
    // Cast a value to the provided type in a Strict mode
    //
    static Object inferType(Object input) {
        if (input in Map) {
            // Cast all values in the map
            Map output = [:]
            input.each { k, v ->
                output[k] = inferType(v)
            }
            return output
        }
        else if (input in List) {
            // Cast all values in the list
            List output = []
            input.each { entry ->
                output.add(inferType(entry))
            }
            return output
        } else if (input in String) {
            // Cast the string if there is one
            if (input == '') {
                return null
            }
            return JSONObject.stringToValue(input)
        }
    }

    //
    // Function to check if a String value is an Integer
    //
    static Boolean isInteger(String input) {
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
    static Boolean isFloat(String input) {
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
    static Boolean isDouble(String input) {
        try {
            input as Double
            return true
        } catch (NumberFormatException e) {
            return false
        }
    }

}
