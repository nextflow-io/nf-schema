package nextflow.validation.utils

import static nextflow.validation.utils.Colors.getLogColors
import static nextflow.validation.utils.Common.getBasePath

import groovy.util.logging.Slf4j
import groovy.transform.CompileStatic
import java.nio.file.Path
import org.json.JSONObject

/**
 * Helper class for discovering and generating help for schema files referenced in nextflow_schema.json
 *
 * @author : jfouret
 */

@Slf4j
@CompileStatic
class AssetsHelper {

    private final Path baseDir
    private final Map colors
    private final Path schemaPath
    private final Map schemaMap

    AssetsHelper(Path baseDir, CharSequence schemaFileName, Boolean monochromeLogs = false) {
        this.baseDir = baseDir
        this.schemaPath = getBasePath(baseDir, schemaFileName)
        if (schemaPath.exists()) {
            JSONObject schemaJson = new JSONObject(schemaPath.text)
            schemaMap = schemaJson.toMap()
        } else {
            schemaMap = [:]
            log.warn("nextflow_schema.json not found at: ${schemaPath.toUriString()}")
        }
        this.colors = getLogColors(monochromeLogs)
    }

    /**
     * Discover schema file for a specific parameter
     * @param param Parameter name to look for
     * @return Schema file path if found, null otherwise
     */
    String discoverSchemaFile(String param) {
        try {
            return findSchemaReference(this.schemaMap, param, '')
        } catch (e) {
            log.error("Error discovering schema file for parameter ${param}: ${e.message}")
        }
        return null
    }

    /**
     * Generate formatted help output for a specific schema file
     * @param schemaPath Path to the schema file
     * @return Formatted help string
     */
    String generateSchemaHelp(String schemaPath) {
        try {
            Path fullSchemaPath = getBasePath(baseDir, schemaPath)
            if (!fullSchemaPath.exists()) {
                log.warn("Schema file not found: ${fullSchemaPath.toUriString()}")
                return null
            }

            JSONObject schemaJson = new JSONObject(fullSchemaPath.text)

            return formatSchemaHelp(schemaJson.toMap())
        } catch (e) {
            log.error("Error generating help for schema ${schemaPath}: ${e.message}")
        }
        return null
    }

    /**
     * Recursively search for a specific parameter's schema reference in JSON structure
     */
    private String findSchemaReference(Object obj, String targetParam, String currentPath) {
        String result
        if (obj in Map) {
            Map map = obj as Map
            map.any { entry ->
                String key = entry.key
                Object value = entry.value
                String newPath = currentPath ? "${currentPath}.${key}" : key

                if (key == 'schema' && value in String) {
                    // Extract the argument name from the current path
                    String argumentName = currentPath.split('\\.').last()
                    if (argumentName == targetParam) {
                        result = value as String
                    }
                } else if (key == 'properties' && value in Map) {
                    // When we find properties, we're at the level where argument names are defined
                    Map properties = value as Map
                    properties.any { propEntry ->
                        String propKey = propEntry.key
                        Object propValue = propEntry.value
                        result = findSchemaReference(propValue, targetParam, propKey)
                        return result
                    }
                } else {
                    result = findSchemaReference(value, targetParam, newPath)
                }
                return result
            }
        } else if (obj in List) {
            List list = obj as List
            list.any { item ->
                result = findSchemaReference(item, targetParam, currentPath)
                return result
            }
        }
        return result
    }

    /**
     * Format schema help output
     */
    private String formatSchemaHelp(Map schema) {
        StringBuilder help = new StringBuilder()

        // Header
        help.append("== Top-level schema below ==${colors.reset}\n")

        if (schema.title) {
            help.append("${colors.bold}Title:${colors.reset} ${schema.title}\n")
        }

        if (schema.description) {
            help.append("${colors.bold}Description:${colors.reset} ${schema.description}\n")
        }

        // Handle array schemas (typical for samplesheets)
        if (schema.type == 'array' && schema.items) {
            help.append(formatArraySchema(schema.items as Map))
        }
        // Handle object schemas
        else if (schema.type == 'object' && schema.properties) {
            help.append(formatObjectSchema(schema.properties as Map, schema.required as List))
        }
        // Handle items directly if no explicit type
        else if (schema.items) {
            help.append(formatArraySchema(schema.items as Map))
        }
        help.append('\n')

        return help.toString()
    }

    /**
     * Format array schema (typical for samplesheets)
     */
    private String formatArraySchema(Map items) {
        StringBuilder help = new StringBuilder()

        if (items.type == 'object' && items.properties) {
            help.append(formatObjectSchema(items.properties as Map, items.required as List))
        }

        return help.toString()
    }

    /**
     * Format object schema properties
     */
    private String formatObjectSchema(Map properties, List required = []) {
        StringBuilder help = new StringBuilder()

        help.append("${colors.bold}Fields:${colors.reset}\n")

        // Calculate max width for alignment
        Integer maxWidth = properties.keySet().collect { w -> (w as String).length() }.max() ?: 0
        maxWidth = Math.max(maxWidth, 10)

        properties.each { key, property ->
            String keyStr = key as String
            Map propertyMap = property as Map
            String type = propertyMap.type ?: 'string'
            String typeStr = "[${type}]"
            String requiredStr = required?.contains(keyStr) ? '[required]' : ''

            /* groovylint-disable-next-line LineLength */
            help.append("    ${colors.cyan}${keyStr.padRight(maxWidth)}${colors.reset} ${colors.dim}${typeStr.padRight(10)}${colors.reset}")

            if (requiredStr) {
                help.append(" ${colors.dim}${requiredStr}${colors.reset}")
            }

            if (propertyMap.description) {
                help.append(" ${propertyMap.description}")
            }

            // Add pattern information
            if (propertyMap.pattern) {
                help.append(" ${colors.dim}(pattern: ${propertyMap.pattern})${colors.reset}")
            }

            // Add enum values
            if (propertyMap.enum) {
                String enumStr = (propertyMap.enum as List).join(', ')
                help.append(" ${colors.dim}(allowed: ${enumStr})${colors.reset}")
            }

            // Add error message if available
            if (propertyMap.errorMessage) {
                help.append("\n      ${colors.yellow}Note: ${propertyMap.errorMessage}${colors.reset}")
            }

            help.append('\n')
        }

        return help.toString()
    }

}
