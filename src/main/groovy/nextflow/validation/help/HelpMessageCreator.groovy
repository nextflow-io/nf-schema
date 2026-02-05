package nextflow.validation.help

import static nextflow.validation.utils.Colors.getLogColors
import static nextflow.validation.utils.FilesHelper.paramsLoad
import static nextflow.validation.utils.Common.getBasePath
import static nextflow.validation.utils.Common.longestStringLength
import static nextflow.validation.utils.Common.getLongestKeyLength

import groovy.util.logging.Slf4j
import groovy.transform.CompileDynamic

import java.nio.file.Path

import nextflow.Session

import nextflow.validation.config.ValidationConfig
import nextflow.validation.utils.AssetsHelper
import nextflow.validation.exceptions.SchemaValidationException

/**
 * This class contains methods to write a help message
 *
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */

@Slf4j
@CompileDynamic
class HelpMessageCreator {

    private final ValidationConfig config
    private final Map colors
    private Integer hiddenParametersCount = 0
    private final Map<String,Map> paramsMap
    private final AssetsHelper assetsHelper
    private final Integer enumLength

    // The length of the terminal
    private final Integer terminalLength = System.getenv('COLUMNS')?.toInteger() ?: 100

    HelpMessageCreator(ValidationConfig inputConfig, Session session) {
        config = inputConfig
        this.assetsHelper = new AssetsHelper(session.baseDir.toString(), config.parametersSchema, config.monochromeLogs)
        enumLength = config.help.enumLength
        colors = getLogColors(config.monochromeLogs)
        paramsMap = paramsLoad(Path.of(getBasePath(session.baseDir.toString(), config.parametersSchema)))
        addHelpParameters()
    }

    String getShortMessage(String param) {
        String helpMessage = ''
        if (param) {
            List<String> paramNames = param.tokenize('.') as List<String>
            Map paramOptions = [:]
            paramsMap.each { String group, Map groupParams ->
                if (groupParams.containsKey(paramNames[0])) {
                    paramOptions = groupParams.get(paramNames[0]) as Map
                }
            }
            if (paramNames.size() > 1) {
                paramNames.remove(0)
                paramNames.each { paramName ->
                    paramOptions = (Map) paramOptions?.properties?[paramName] ?: [:]
                }
            }
            if (!paramOptions) {
                /* groovylint-disable-next-line LineLength */
                throw new SchemaValidationException("Unable to create help message: Specified param '${param}' does not exist in JSON schema.")
            }
            if (paramOptions.containsKey('properties')) {
                paramOptions.properties = removeHidden(paramOptions.properties)
            }
            helpMessage = getDetailedHelpString(param, paramOptions)

            // Check if this parameter has an associated schema file and append schema help
            String schemaPath = assetsHelper.discoverSchemaFile(paramNames[0])
            if (schemaPath) {
                String schemaHelp = assetsHelper.generateSchemaHelp(schemaPath)
                if (schemaHelp) {
                    helpMessage += schemaHelp
                }
            }
        } else {
            helpMessage = groupHelpString
        }
        return helpMessage
    }

    String getFullMessage() {
        return getGroupHelpString(true)
    }

    String getBeforeText() {
        String beforeText = config.help.beforeText
        if (config.help.command) {
            beforeText += 'Typical pipeline command:\n\n'
            beforeText += "  ${colors.cyan}${config.help.command}${colors.reset}\n\n"
        }
        return beforeText
    }

    String getAfterText() {
        String afterText = ''
        if (hiddenParametersCount > 0) {
            /* groovylint-disable-next-line LineLength */
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
        String helpMessage = "${colors.underlined}${colors.bold}--${paramName}${colors.reset}\n"
        Integer optionMaxChars = longestStringLength(
            paramOptions.keySet()
                .collect { paramOption -> paramOption == 'properties' ? 'options' : paramOption } as List<String>
        )
        for (option in paramOptions) {
            String key = option.key
            if (key == 'fa_icon' || (key == 'type' && option.value == 'object')) {
                continue
            }
            if (key == 'properties') {
                Map subParamsOptions = [:]
                flattenNestedSchemaMap(option.value as Map).each { String subParam, Map value ->
                    subParamsOptions.put("${paramName}.${subParam}" as String, value)
                }
                Integer maxChars = longestStringLength(subParamsOptions.keySet() as List<String>) + 1
                String subParamsHelpString = getHelpListParams(subParamsOptions, maxChars, paramName)
                    .collect { helpString ->
                        '      --' + helpString[4..helpString.length() - 1]
                    }
                    .join('\n')
                helpMessage += '    ' + colors.dim + 'options'.padRight(optionMaxChars) + ': ' +
                    colors.reset + '\n' + subParamsHelpString + '\n\n'
                continue
            }
            String value = option.value
            if (value.length() > terminalLength) {
                value = wrapText(value)
            }
            helpMessage += '    ' + colors.dim + key.padRight(optionMaxChars) + ': ' + colors.reset + value + '\n'
        }
        return helpMessage
    }

    //
    // Get the full help message for a grouped params structure in list format
    //
    private String getGroupHelpString(Boolean showNested = false) {
        String helpMessage = ''
        Map<String,Map> visibleParamsMap = paramsMap.collectEntries { key, Map value -> [key, removeHidden(value)] }
        Map<String,Map> parsedParams = showNested ?
            visibleParamsMap.collectEntries { key, Map value -> [key, flattenNestedSchemaMap(value)] } :
            visibleParamsMap
        Integer maxChars = getLongestKeyLength(parsedParams) + 1
        if (parsedParams.containsKey('Other parameters')) {
            Map ungroupedParams = parsedParams['Other parameters']
            parsedParams.remove('Other parameters')
            helpMessage += getHelpListParams(ungroupedParams, maxChars + 2).collect { helpString ->
                helpString[2..helpString.length() - 1]
            }.join('\n') + '\n\n'
        }
        parsedParams.each { String group, Map groupParams ->
            List<String> helpList = getHelpListParams(groupParams, maxChars)
            if (helpList.size() > 0) {
                helpMessage += "${colors.underlined}${colors.bold}${group}${colors.reset}\n" as String
                helpMessage += helpList.join('\n') + '\n\n'
            }
        }
        return helpMessage
    }

    private Map<String,Map> removeHidden(Map<String,Map> map) {
        if (config.help.showHidden) {
            return map
        }
        Map<String,Map> returnMap = [:]
        map.each { String key, Map value ->
            if (!value.hidden) {
                returnMap[key] = value
            } else if (value.containsKey('properties')) {
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
    private List<String> getHelpListParams(Map<String,Map> params, Integer maxChars, String parentParameter = '') {
        List helpMessage = []
        Integer typeMaxChars = longestStringLength(params.collect { key, value ->
            Object type = value.get('type', '')
            return type in String && type.length() > 0 ? "[${type}]" : type as String
        })
        for (String paramName in params.keySet()) {
            Map paramOptions = params.get(paramName) as Map
            Object paramType = paramOptions.get('type', '')
            String type = paramType in String && paramType.length() > 0 ?
                '[' + paramType + ']' :
                paramType as String
            String enumsString = ''
            if (paramOptions.enum != null) {
                List enums = (List) paramOptions.enum
                String chopEnums = enums.join(', ')
                if (chopEnums.length() > enumLength && enumLength >= 0) {
                    chopEnums = chopEnums.substring(0, enumLength)
                    if (chopEnums.contains(',')) {
                        chopEnums = chopEnums.substring(0, chopEnums.lastIndexOf(',')) + ', ...'
                    } else {
                        chopEnums = '...'
                    }
                }
                enumsString = ' (accepted: ' + chopEnums + ') '
            }
            String description = paramOptions.description ? paramOptions.description as String + ' ' : ''
            String defaultValue = paramOptions.default != null ? '[default: ' + paramOptions.default + '] ' : ''
            String nestedParamName = parentParameter ? parentParameter + '.' + paramName : paramName
            String nestedString = paramOptions.properties ?
                "(This parameter has sub-parameters. Use '--help ${nestedParamName}' to see all sub-parameters) " :
                ''
            String descriptionDefault = description + colors.dim + enumsString +
                defaultValue + colors.reset + nestedString
            // Wrap long description texts
            // Loosely based on https://dzone.com/articles/groovy-plain-text-word-wrap
            if (descriptionDefault.length() > terminalLength) {
                descriptionDefault = wrapText(descriptionDefault)
            }
            helpMessage.add(
                '  --' +  paramName.padRight(maxChars) + colors.dim +
                type.padRight(typeMaxChars + 1) + colors.reset + descriptionDefault
            )
        }
        return helpMessage
    }

    //
    // Flattens the schema params map so all nested parameters are shown as their full name
    //
    private Map<String,Map> flattenNestedSchemaMap(Map params) {
        Map returnMap = [:]
        params.each { String key, Map value ->
            if (value.containsKey('properties')) {
                Map flattenedMap = flattenNestedSchemaMap(value.properties)
                flattenedMap.each { String k, Map v ->
                    returnMap.put(key + '.' + k, v)
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
        if (!paramsMap.containsKey('Other parameters')) {
            paramsMap['Other parameters'] = [:]
        }
        paramsMap['Other parameters'][config.help.shortParameter] = [
            'type': ['boolean', 'string'],
            /* groovylint-disable-next-line LineLength */
            'description': "Show the help message for all top level parameters. When a parameter is given to `--${config.help.shortParameter}`, the full help message of that parameter will be printed."
        ]
        paramsMap['Other parameters'][config.help.fullParameter] = [
            'type': 'boolean',
            'description': 'Show the help message for all non-hidden parameters.'
        ]
        paramsMap['Other parameters'][config.help.showHiddenParameter] = [
            'type': 'boolean',
            /* groovylint-disable-next-line LineLength */
            'description': "Show all hidden parameters in the help message. This needs to be used in combination with `--${config.help.shortParameter}` or `--${config.help.fullParameter}`."
        ]
    }

    //
    // Wrap too long text
    //
    private String wrapText(String text) {
        List olines = []
        String oline = ''
        text.split(' ').each { wrd ->
            if ((oline.size() + wrd.size()) <= terminalLength) {
                oline += wrd + ' '
            } else {
                olines += oline
                oline = wrd + ' '
            }
        }
        olines += oline
        return olines.join('\n')
    }

}
