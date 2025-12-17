package nextflow.validation.utils

import org.json.JSONObject
import org.json.JSONArray
import org.json.JSONPointer
import org.json.JSONPointerException
import groovy.util.logging.Slf4j
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
public class Common {

    private static final List<String> CLOUD_STORAGE_SCHEMES = ['s3://', 'az://', 'gs://']

    //
    // Check if a path string is a cloud storage path (S3, Azure, GCS)
    //
    public static boolean isCloudStoragePath(String value) {
        return CLOUD_STORAGE_SCHEMES.any { value.startsWith(it) }
    }

    //
    // Get full path based on the base directory of the pipeline run
    //
    public static String getBasePath(String baseDir, String schemaFilename) {
        if (Path.of(schemaFilename).exists()) {
            return schemaFilename
        } else {
            return "${baseDir}/${schemaFilename}"
        }
    }

    //
    // Function to get the value from a JSON pointer
    //
    public static Object getValueFromJsonPointer(String jsonPointer, Object json) {
        def JSONPointer schemaPointer = new JSONPointer(jsonPointer)
        try {
            return schemaPointer.queryFrom(json) ?: ""
        } catch (JSONPointerException e) {
            return ""
        }
    }

    //
    // Get the amount of character of the largest key in a map
    //
    public static Integer getLongestKeyLength( Map input ) {
        return Collections.max(input.collect { _, val -> 
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

    //
    // Find a value in a nested map
    //
    public static Object findDeep(Object m, String key) {
        if (m instanceof Map) {
            if (m.containsKey(key)) {
                return m[key]
            } else {
                return m.findResult { k, v -> findDeep(v, key) }
            }
        } 
        else if (m instanceof List) {
            return m.findResult { element -> findDeep(element, key) }
        }
        return null
    }

    //
    // Check if a key exists in a nested map
    //
    public static boolean hasDeepKey(Object m, String key) {
        if (m instanceof Map) {
            if (m.containsKey(key)) {
                return true
            } else {
                return m.any { k, v -> hasDeepKey(v, key) }
            }
        } 
        else if (m instanceof List) {
            return m.any { element -> hasDeepKey(element, key) }
        }
        return false
    }

    public static void findAllKeys(Object object, String key, Set<String> finalKeys, String sep) {
        if (object instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) object;

            jsonObject.keySet().forEach{ childKey ->
                findAllKeys(jsonObject.get(childKey), key != null ? key + sep + childKey : childKey, finalKeys, sep)
            };
        } else if (object instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) object;
            key != null ? finalKeys.add(key) : ''

            IntStream.range(0, jsonArray.length())
                    .mapToObj(jsonArray::get)
                    .forEach{ jObj -> findAllKeys(jObj, key, finalKeys, sep)};
        }
        else{
            key != null ? finalKeys.add(key) : ''
        }
    }
    public static String kebabToCamel(String s) {
        return s.replaceAll( "(-)([A-Za-z0-9])", { Object[] it -> it[2].toUpperCase() } )
    }
}