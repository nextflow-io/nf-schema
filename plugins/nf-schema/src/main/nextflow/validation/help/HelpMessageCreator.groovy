package nextflow.validation.help

import groovy.util.logging.Slf4j

import java.nio.file.Path

import nextflow.Session

import nextflow.validation.config.ValidationConfig
import static nextflow.validation.utils.Colors.getLogColors
import static nextflow.validation.utils.Files.paramsLoad
import static nextflow.validation.utils.Common.getBasePath
import static nextflow.validation.utils.Common.longestStringLength
import static nextflow.validation.utils.Common.getLongestKeyLength

/**
 * This class contains methods to write a help message
 *
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 *
 */

@Slf4j
class HelpMessageCreator {

    private final ValidationConfig config
    private final Map colors
    private Integer hiddenParametersCount = 0
    private Map<String,Map> paramsMap

    // The length of the terminal
    private Integer terminalLength = System.getenv("COLUMNS")?.toInteger() ?: 100

    HelpMessageCreator(ValidationConfig inputConfig, Session session) {
        config = inputConfig
        colors = getLogColors(config.monochromeLogs)
        paramsMap = paramsLoad( Path.of(getBasePath(session.baseDir.toString(), config.parametersSchema)) )
        addHelpParameters()
    }

    public String getShortMessage(String param) {
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
                throw new Exception("Unable to create help message: Specified param '${param}' does not exist in JSON schema.")
            }
            if(paramOptions.containsKey("properties")) {
                paramOptions.properties = removeHidden(paramOptions.properties)
            }
            helpMessage = getDetailedHelpString(param, paramOptions)
        } else {
            helpMessage = getGroupHelpString()
        }
        return helpMessage
    }

    public String getFullMessage() {
        return getGroupHelpString(true)
    }

    public String getBeforeText() {
        def String beforeText = config.help.beforeText
        if (config.help.command) {
            beforeText += "Typical pipeline command:\n\n"
            beforeText += "  ${colors.cyan}${config.help.command}${colors.reset}\n\n"
        }
        return beforeText
    }

    public String getAfterText() {
        def String afterText = ""
        if (hiddenParametersCount > 0) {
            afterText += " ${colors.dim}!! Hiding ${hiddenParametersCount} param(s), use the `--${config.help.showHiddenParameter}` parameter to show them !!${colors.reset}\n"
        }
        afterText += "-${colors.dim}----------------------------------------------------${colors.reset}-\n"
        afterText += config.help.afterText
        return afterText
    }

    //
    // Get a detailed help string from one parameter
    //
    private String getDetailedHelpString(String paramName, Map paramOptions) {
        def String helpMessage = "${colors.underlined}${colors.bold}--${paramName}${colors.reset}\n"
        def Integer optionMaxChars = longestStringLength(paramOptions.keySet().collect { it == "properties" ? "options" : it } as List<String>)
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
                def Integer maxChars = longestStringLength(subParamsOptions.keySet() as List<String>) + 1
                def String subParamsHelpString = getHelpListParams(subParamsOptions, maxChars, paramName)
                    .collect {
                        "      --" + it[4..it.length()-1]
                    }
                    .join("\n")
                helpMessage += "    " + colors.dim + "options".padRight(optionMaxChars) + ": " + colors.reset + "\n" + subParamsHelpString + "\n\n"
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
        def Integer maxChars = getLongestKeyLength(parsedParams) + 1
        if (parsedParams.containsKey("Other parameters")) {
            def Map ungroupedParams = parsedParams["Other parameters"]
            parsedParams.remove("Other parameters")
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
        if (config.help.showHidden) {
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
        def Integer typeMaxChars = longestStringLength(params.collect { key, value -> value.type instanceof String ? "[${value.type}]" : value.type as String})
        for (String paramName in params.keySet()) {
            def Map paramOptions = params.get(paramName) as Map 
            def String type = paramOptions.type instanceof String ? '[' + paramOptions.type + ']' : paramOptions.type as String
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
            helpMessage.add("  --" +  paramName.padRight(maxChars) + colors.dim + type.padRight(typeMaxChars + 1) + colors.reset + descriptionDefault)
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

    //
    // This function adds the help parameters to the main parameters map as ungrouped parameters
    //
    private void addHelpParameters() {
        if (!paramsMap.containsKey("Other parameters")) {
            paramsMap["Other parameters"] = [:]
        }
        paramsMap["Other parameters"][config.help.shortParameter] = [
            "type": ["boolean", "string"],
            "description": "Show the help message for all top level parameters. When a parameter is given to `--${config.help.shortParameter}`, the full help message of that parameter will be printed."
        ]
        paramsMap["Other parameters"][config.help.fullParameter] = [
            "type": "boolean",
            "description": "Show the help message for all non-hidden parameters."
        ]
        paramsMap["Other parameters"][config.help.showHiddenParameter] = [
            "type": "boolean",
            "description": "Show all hidden parameters in the help message. This needs to be used in combination with `--${config.help.shortParameter}` or `--${config.help.fullParameter}`."
        ]
    }

    //
    // Wrap too long text
    //
    private String wrapText(String text) {
        def List olines = []
        def String oline = ""
        text.split(" ").each() { wrd ->
            if ((oline.size() + wrd.size()) <= terminalLength) {
                oline += wrd + " "
            } else {
                olines += oline
                oline = wrd + " "
            }
        }
        olines += oline
        return olines.join("\n")
    }


}