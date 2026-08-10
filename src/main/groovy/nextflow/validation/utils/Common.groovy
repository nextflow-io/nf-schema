package nextflow.validation.utils

import org.json.JSONObject
import org.json.JSONArray
import org.json.JSONPointer
import org.json.JSONPointerException
import groovy.util.logging.Slf4j
import groovy.transform.CompileDynamic
import java.nio.file.Path
import java.util.stream.IntStream

/**
 * A collection of commonly used functions
 *
 * @author : mirpedrol <mirp.julia@gmail.com>
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 * @author : KevinMenden
 */

@Slf4j
@CompileDynamic
public class Common {

    //
    // Get full path based on the base directory of the pipeline run
    //
    static String getBasePath(String baseDir, String schemaFilename) {
        if (Path.of(schemaFilename).exists()) {
            return schemaFilename
        }
        return "${baseDir}/${schemaFilename}"
    }

    //
    // Function to get the value from a JSON pointer
    //
    static Object getValueFromJsonPointer(String jsonPointer, Object json) {
        JSONPointer schemaPointer = new JSONPointer(jsonPointer)
        try {
            return schemaPointer.queryFrom(json) ?: ''
        } catch (JSONPointerException e) {
            return ''
        }
    }

    //
    // Get the amount of character of the largest key in a map
    //
    static Integer getLongestKeyLength(Map input) {
        return Collections.max(input.collect { key, val ->
            Map groupParams = val as Map
            longestStringLength(groupParams.keySet() as List<String>)
        })
    }

    //
    // Get the size of the longest string value in a list of strings
    //
    static Integer longestStringLength(List<String> strings) {
        return strings ? Collections.max(strings*.size()) : 0
    }

    //
    // Find a value in a nested map
    //
    static Object findDeep(Object m, String key) {
        if (m in Map) {
            if (m.containsKey(key)) {
                return m[key]
            }
            return m.findResult { k, v -> findDeep(v, key) }
        }
        else if (m in List) {
            return m.findResult { element -> findDeep(element, key) }
        }
        return null
    }

    //
    // Check if a key exists in a nested map
    //
    static boolean hasDeepKey(Object m, String key) {
        if (m in Map) {
            if (m.containsKey(key)) {
                return true
            }
            return m.any { k, v -> hasDeepKey(v, key) }
        }
        else if (m in List) {
            return m.any { element -> hasDeepKey(element, key) }
        }
        return false
    }

    static void findAllKeys(Object object, String key, Set<String> finalKeys, String sep) {
        if (object in JSONObject) {
            JSONObject jsonObject = (JSONObject) object

            jsonObject.keySet().forEach { childKey ->
                findAllKeys(jsonObject.get(childKey), key != null ? key + sep + childKey : childKey, finalKeys, sep)
            }
        } else if (object in JSONArray) {
            JSONArray jsonArray = (JSONArray) object
            key != null ? finalKeys.add(key) : ''

            IntStream.range(0, jsonArray.length())
                    .mapToObj(jsonArray::get)
                    .forEach { jObj -> findAllKeys(jObj, key, finalKeys, sep) }
        }
        else {
            key != null ? finalKeys.add(key) : ''
        }
    }

    static String kebabToCamel(String s) {
        Closure<Object[]> toUpper = { Object[] strs -> strs[2].toUpperCase() }
        return s.replaceAll('(-)([A-Za-z0-9])', toUpper)
    }

}
