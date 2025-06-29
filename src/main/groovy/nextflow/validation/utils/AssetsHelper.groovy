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
     * Discover schema files referenced in nextflow_schema.json
     * @return Map of argument names to schema file paths found in the nextflow_schema.json
     */
    Map<String, String> discoverSchemaFiles() {
        def Map<String, String> schemaFiles = new HashMap<>()
        
        try {
            def Path schemaPath = Paths.get(getBasePath(baseDir, schemaFileName))
            if (!Files.exists(schemaPath)) {
                log.warn("nextflow_schema.json not found at: ${schemaPath}")
                return [:]
            }

            def JsonSlurper jsonSlurper = new JsonSlurper()
            def Map schema = jsonSlurper.parse(schemaPath.toFile()) as Map
            
            // Search for schema references in the JSON structure
            findSchemaReferences(schema, schemaFiles, "")
            
        } catch (Exception e) {
            log.error("Error discovering schema files: ${e.message}")
            return [:]
        }
        
        return schemaFiles
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
     * Recursively search for schema references in JSON structure
     */
    private void findSchemaReferences(Object obj, Map<String, String> schemaFiles, String currentPath) {
        if (obj instanceof Map) {
            Map map = obj as Map
            map.each { key, value ->
                def String newPath = currentPath ? "${currentPath}.${key}" : key
                if (key == "schema" && value instanceof String) {
                    // Extract the argument name from the current path
                    def String argumentName = currentPath.split('\\.').last()
                    schemaFiles.put(argumentName, value as String)
                } else if (key == "properties" && value instanceof Map) {
                    // When we find properties, we're at the level where argument names are defined
                    Map properties = value as Map
                    properties.each { propKey, propValue ->
                        findSchemaReferences(propValue, schemaFiles, propKey)
                    }
                } else {
                    findSchemaReferences(value, schemaFiles, newPath)
                }
            }
        } else if (obj instanceof List) {
            List list = obj as List
            list.each { item ->
                findSchemaReferences(item, schemaFiles, currentPath)
            }
        }
    }

    /**
     * Format schema help output
     */
    private String formatSchemaHelp(Map schema, String argumentName, String schemaPath) {
        def StringBuilder help = new StringBuilder()
        
        // Header
        help.append("\n${colors.underlined}${colors.bold}Argument: --${argumentName}${colors.reset}\n")
        help.append("${colors.bold}Schema: ${schemaPath}${colors.reset}\n")
        
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
        
        // Show required fields first if any
        if (required && required.size() > 0) {
            help.append("${colors.bold}Required fields${colors.reset} (${colors.red}*${colors.reset}): ${required.join(', ')}\n")
        }
        help.append("${colors.bold}Fields:${colors.reset}\n")

        
        // Calculate max width for alignment
        def Integer maxWidth = properties.keySet().collect { it.length() }.max() ?: 0
        maxWidth = Math.max(maxWidth, 10)
        
        properties.each { String key, Map property ->
            def String requiredMarker = required?.contains(key) ? "(${colors.red}*${colors.reset}) " : "( ) "
            def String type = property.type ?: "string"
            def String typeStr = "[${type}]"
            
            help.append("  ${requiredMarker}${colors.cyan}${key.padRight(maxWidth)}${colors.reset} ${colors.dim}${typeStr.padRight(10)}${colors.reset}")
            
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
                help.append("\n    ${colors.yellow}Note: ${property.errorMessage}${colors.reset}")
            }
            
            help.append("\n")
        }
        
        return help.toString()
    }
}
