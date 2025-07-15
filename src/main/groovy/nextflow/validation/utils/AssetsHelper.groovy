package nextflow.validation.utils

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files

import static nextflow.validation.utils.Colors.getLogColors
import static nextflow.validation.utils.Common.getBasePath

/**
 * Helper class for discovering and generating help for schema files referenced in nextflow_schema.json
 *
 * @author : jfouret
 */

@Slf4j
class AssetsHelper {

    private final String baseDir
    private final Map colors
    private final String schemaFileName

    AssetsHelper(String baseDir, String schemaFileName, Boolean monochromeLogs = false) {
        this.baseDir = baseDir
        this.schemaFileName = schemaFileName
        this.colors = getLogColors(monochromeLogs)
    }

    /**
     * Discover schema file for a specific parameter
     * @param param Parameter name to look for
     * @return Schema file path if found, null otherwise
     */
    String discoverSchemaFile(String param) {
        try {
            def Path schemaPath = Paths.get(getBasePath(baseDir, schemaFileName))
            if (!Files.exists(schemaPath)) {
                log.warn("nextflow_schema.json not found at: ${schemaPath}")
                return null
            }

            def JsonSlurper jsonSlurper = new JsonSlurper()
            def Map schema = jsonSlurper.parse(schemaPath.toFile()) as Map
            
            // Search for the specific parameter's schema reference
            return findSchemaReference(schema, param, "")
            
        } catch (Exception e) {
            log.error("Error discovering schema file for parameter ${param}: ${e.message}")
            return null
        }
    }

    /**
     * Generate formatted help output for a specific schema file
     * @param argumentName Name of the argument that uses this schema
     * @param schemaPath Path to the schema file
     * @return Formatted help string
     */
    String generateSchemaHelp(String argumentName, String schemaPath) {
        try {
            def Path fullSchemaPath = Paths.get(getBasePath(baseDir, schemaPath))
            if (!Files.exists(fullSchemaPath)) {
                log.warn("Schema file not found: ${fullSchemaPath}")
                return null
            }

            def JsonSlurper jsonSlurper = new JsonSlurper()
            def Map schema = jsonSlurper.parse(fullSchemaPath.toFile()) as Map
            
            return formatSchemaHelp(schema, argumentName, schemaPath)
            
        } catch (Exception e) {
            log.error("Error generating help for schema ${schemaPath}: ${e.message}")
            return null
        }
    }

    /**
     * Recursively search for a specific parameter's schema reference in JSON structure
     */
    private String findSchemaReference(Object obj, String targetParam, String currentPath) {
        if (obj instanceof Map) {
            Map map = obj as Map
            for (entry in map) {
                def String key = entry.key
                def value = entry.value
                def String newPath = currentPath ? "${currentPath}.${key}" : key
                
                if (key == "schema" && value instanceof String) {
                    // Extract the argument name from the current path
                    def String argumentName = currentPath.split('\\.').last()
                    if (argumentName == targetParam) {
                        return value as String
                    }
                } else if (key == "properties" && value instanceof Map) {
                    // When we find properties, we're at the level where argument names are defined
                    Map properties = value as Map
                    for (propEntry in properties) {
                        def String propKey = propEntry.key
                        def propValue = propEntry.value
                        if (propKey == targetParam) {
                            def String result = findSchemaReference(propValue, targetParam, propKey)
                            if (result) return result
                        } else {
                            def String result = findSchemaReference(propValue, targetParam, propKey)
                            if (result) return result
                        }
                    }
                } else {
                    def String result = findSchemaReference(value, targetParam, newPath)
                    if (result) return result
                }
            }
        } else if (obj instanceof List) {
            List list = obj as List
            for (item in list) {
                def String result = findSchemaReference(item, targetParam, currentPath)
                if (result) return result
            }
        }
        return null
    }

    /**
     * Format schema help output
     */
    private String formatSchemaHelp(Map schema, String argumentName, String schemaPath) {
        def StringBuilder help = new StringBuilder()
        
        // Header
        help.append("== Top-level schema below ==${colors.reset}\n")
        
        if (schema.title) {
            help.append("${colors.bold}Title:${colors.reset} ${schema.title}\n")
        }
        
        if (schema.description) {
            help.append("${colors.bold}Description:${colors.reset} ${schema.description}\n")
        }
        
        // Handle array schemas (typical for samplesheets)
        if (schema.type == "array" && schema.items) {
            help.append(formatArraySchema(schema.items as Map))
        }
        // Handle object schemas
        else if (schema.type == "object" && schema.properties) {
            help.append(formatObjectSchema(schema.properties as Map, schema.required as List))
        }
        // Handle items directly if no explicit type
        else if (schema.items) {
            help.append(formatArraySchema(schema.items as Map))
        }
        help.append("\n")
        
        return help.toString()
    }

    /**
     * Format array schema (typical for samplesheets)
     */
    private String formatArraySchema(Map items) {
        def StringBuilder help = new StringBuilder()
        
        if (items.type == "object" && items.properties) {
            help.append(formatObjectSchema(items.properties as Map, items.required as List))
        }
        
        return help.toString()
    }

    /**
     * Format object schema properties
     */
    private String formatObjectSchema(Map properties, List required = []) {
        def StringBuilder help = new StringBuilder()
        
        help.append("${colors.bold}Fields:${colors.reset}\n")
        
        // Calculate max width for alignment
        def Integer maxWidth = properties.keySet().collect { it.length() }.max() ?: 0
        maxWidth = Math.max(maxWidth, 10)
        
        properties.each { String key, Map property ->
            def String type = property.type ?: "string"
            def String typeStr = "[${type}]"
            def String requiredStr = required?.contains(key) ? "[required]" : ""
            
            help.append("    ${colors.cyan}${key.padRight(maxWidth)}${colors.reset} ${colors.dim}${typeStr.padRight(10)}${colors.reset}")
            
            if (requiredStr) {
                help.append(" ${colors.dim}${requiredStr}${colors.reset}")
            }
            
            if (property.description) {
                help.append(" ${property.description}")
            }
            
            // Add pattern information
            if (property.pattern) {
                help.append(" ${colors.dim}(pattern: ${property.pattern})${colors.reset}")
            }
            
            // Add enum values
            if (property.enum) {
                def String enumStr = (property.enum as List).join(", ")
                help.append(" ${colors.dim}(allowed: ${enumStr})${colors.reset}")
            }
            
            // Add error message if available
            if (property.errorMessage) {
                help.append("\n      ${colors.yellow}Note: ${property.errorMessage}${colors.reset}")
            }
            
            help.append("\n")
        }
        
        return help.toString()
    }
}
