package nextflow.validation

import groovy.util.logging.Slf4j

import java.nio.file.Path

import nextflow.Session

/**
 * This class contains methods to write a help message
 *
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 *
 */

@Slf4j
class HelpMessage {

    private final ValidationConfig config
    private final Map colors
    private Integer hiddenParametersCount = 0
    private Map<String,Map> paramsMap

    // The length of the terminal
    private Integer terminalLength = System.getenv("COLUMNS")?.toInteger() ?: 100

    HelpMessage(ValidationConfig inputConfig, Session session) {
        config = inputConfig
        colors = Utils.logColours(config.monochromeLogs)
        paramsMap = Utils.paramsLoad( Path.of(Utils.getSchemaPath(session.baseDir.toString(), config.parametersSchema)) )
    }

    public void printShortHelpMessage(String param) {
        def String helpMessage = ""
        if (param) {
            def List<String> paramNames = param.tokenize(".") as List<String>
            def Map paramOptions = [:]
            paramsMap.each { String group, Map groupParams ->
                if (groupParams.containsKey(paramNames[0])) {
                    paramOptions = groupParams.get(paramNames[0]) as Map 
                }
            }
            if (paramNames.size() > 1) {
                paramNames.remove(0)
                paramNames.each {
                    paramOptions = (Map) paramOptions?.properties?[it] ?: [:]
                }
            }
            if (!paramOptions) {
                throw new Exception("Specified param '${paramName}' does not exist in JSON schema.")
            }
            if(paramOptions.containsKey("properties")) {
                paramOptions.properties = removeHidden(paramOptions.properties)
            }
            helpMessage = getDetailedHelpString(param, paramOptions)
        } else {
            helpMessage = getGroupHelpString()
        }
        log.info(helpMessage)
    }

    public printFullHelpMessage() {
        log.info(getGroupHelpString(true))
    }

    public printBeforeText() {
        log.info(config.help.beforeText)
        if (config.help.command) {
            log.info("Typical pipeline command:\n")
            log.info("  ${colors.cyan}${config.help.command}${colors.reset}\n")
        }
    }

    public printAfterText() {
        if (hiddenParametersCount > 0) {
            log.info(" ${colors.dim}!! Hiding ${hiddenParametersCount} param(s), use the `validation.showHiddenParams` config value to show them !!${colors.reset}")
        }
        log.info(config.help.afterText)
    }

    //
    // Get a detailed help string from one parameter
    //
    private String getDetailedHelpString(String paramName, Map paramOptions) {
        def String helpMessage = "${colors.underlined}${colors.bold}--${paramName}${colors.reset}\n"
        def Integer optionMaxChars = Utils.longestStringLength(paramOptions.keySet().collect { it == "properties" ? "options" : it } as List<String>)
        for (option in paramOptions) {
            def String key = option.key
            if (key == "fa_icon" || (key == "type" && option.value == "object")) {
                continue
            }
            if (key == "properties") {
                def Map subParamsOptions = [:]
                flattenNestedSchemaMap(option.value as Map).each { String subParam, Map value ->
                    subParamsOptions.put("${paramName}.${subParam}" as String, value)
                }
                def Integer maxChars = Utils.longestStringLength(subParamsOptions.keySet() as List<String>) + 1
                def String subParamsHelpString = getHelpListParams(subParamsOptions, maxChars, paramName)
                    .collect {
                        "      --" + it[4..it.length()-1]
                    }
                    .join("\n")
                helpMessage += "    " + colors.dim + "options".padRight(optionMaxChars) + ": " + colors.reset + "\n" + subParamsHelpString + "\n"
                continue
            }
            def String value = option.value
            if (value.length() > terminalLength) {
                value = wrapText(value)
            }
            helpMessage += "    " + colors.dim + key.padRight(optionMaxChars) + ": " + colors.reset + value + '\n'
        }
        return helpMessage
    }

    //
    // Get the full help message for a grouped params structure in list format
    //
    private String getGroupHelpString(Boolean showNested = false) {
        def String helpMessage = ""
        def Map<String,Map> visibleParamsMap = paramsMap.collectEntries { key, Map value -> [key, removeHidden(value)]}
        def Map<String,Map> parsedParams = showNested ? visibleParamsMap.collectEntries { key, Map value -> [key, flattenNestedSchemaMap(value)] } : visibleParamsMap
        def Integer maxChars = Utils.paramsMaxChars(parsedParams) + 1
        if (parsedParams.containsKey(null)) {
            def Map ungroupedParams = parsedParams[null]
            parsedParams.remove(null)
            helpMessage += getHelpListParams(ungroupedParams, maxChars + 2).collect {
                it[2..it.length()-1]
            }.join("\n") + "\n\n"
        }
        parsedParams.each { String group, Map groupParams ->
            def List<String> helpList = getHelpListParams(groupParams, maxChars)
            if (helpList.size() > 0) {
                helpMessage += "${colors.underlined}${colors.bold}${group}${colors.reset}\n" as String
                helpMessage += helpList.join("\n") + "\n\n"
            }
        }
        return helpMessage
    }

    private Map<String,Map> removeHidden(Map<String,Map> map) {
        if (config.help.showHiddenParams) {
            return map
        }
        def Map<String,Map> returnMap = [:]
        map.each { String key, Map value ->
            if(!value.hidden) {
                returnMap[key] = value
            } else if(value.containsKey("properties")) {
                value.properties = removeHidden(value.properties)
                returnMap[key] = value
            } else {
                hiddenParametersCount++
            }
        }
        return returnMap
    }

    //
    // Get help for params in list format
    //
    private List<String> getHelpListParams(Map<String,Map> params, Integer maxChars, String parentParameter = "") {
        def List helpMessage = []
        for (String paramName in params.keySet()) {
            def Map paramOptions = params.get(paramName) as Map 
            if (paramOptions.hidden && !config.showHiddenParams) {
                hiddenParametersCount += 1
                continue
            }
            def String type = '[' + paramOptions.type + ']'
            def String enumsString = ""
            if (paramOptions.enum != null) {
                def List enums = (List) paramOptions.enum
                def String chopEnums = enums.join(", ")
                if(chopEnums.length() > terminalLength){
                    chopEnums = chopEnums.substring(0, terminalLength-5)
                    chopEnums = chopEnums.substring(0, chopEnums.lastIndexOf(",")) + ", ..."
                }
                enumsString = " (accepted: " + chopEnums + ") "
            }
            def String description = paramOptions.description ? paramOptions.description as String + " " : ""
            def defaultValue = paramOptions.default != null ? "[default: " + paramOptions.default.toString() + "] " : ''
            def String nestedParamName = parentParameter ? parentParameter + "." + paramName : paramName
            def String nestedString = paramOptions.properties ? "(This parameter has sub-parameters. Use '--help ${nestedParamName}' to see all sub-parameters) " : ""
            def descriptionDefault = description + colors.dim + enumsString + defaultValue + colors.reset + nestedString
            // Wrap long description texts
            // Loosely based on https://dzone.com/articles/groovy-plain-text-word-wrap
            if (descriptionDefault.length() > terminalLength){
                descriptionDefault = wrapText(descriptionDefault)
            }
            helpMessage.add("  --" +  paramName.padRight(maxChars) + colors.dim + type.padRight(10) + colors.reset + descriptionDefault)
        }
        return helpMessage
    }

    //
    // Flattens the schema params map so all nested parameters are shown as their full name
    //
    private Map<String,Map> flattenNestedSchemaMap(Map params) {
        def Map returnMap = [:]
        params.each { String key, Map value ->
            if (value.containsKey("properties")) {
                def flattenedMap = flattenNestedSchemaMap(value.properties)
                flattenedMap.each { String k, Map v ->
                    returnMap.put(key + "." + k, v)
                }
            } else {
                returnMap.put(key, value)
            }
        }
        return returnMap
    }

}