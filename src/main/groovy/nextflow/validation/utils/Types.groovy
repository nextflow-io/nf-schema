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

    //
    // Function that casts parameters to the correct type, copied from the Nextflow codebase:
    /* groovylint-disable-next-line LineLength */
    // https://github.com/nextflow-io/nextflow/blob/eae9f7d09576cbc97107f472d833910e24bcb85c/modules/nextflow/src/main/groovy/nextflow/cli/CmdRun.groovy#L777-L791
    //
    static Object parseParamValue(String str) {
        if (str == null) { return null }

        if (str.toLowerCase() == 'true') { return Boolean.TRUE }
        if (str.toLowerCase() == 'false') { return Boolean.FALSE }

        /* groovylint-disable-next-line UnnecessaryGetter */
        if (str ==~ /-?\d+(\.\d+)?/ && str.isInteger()) { return str.toInteger() }
        /* groovylint-disable-next-line UnnecessaryGetter */
        if (str ==~ /-?\d+(\.\d+)?/ && str.isLong()) { return str.toLong() }
        /* groovylint-disable-next-line UnnecessaryGetter */
        if (str ==~ /-?\d+(\.\d+)?/ && str.isDouble()) { return str.toDouble() }

        return str
    }

}
