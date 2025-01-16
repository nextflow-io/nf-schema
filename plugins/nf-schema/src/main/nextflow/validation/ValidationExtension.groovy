package nextflow.validation

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.json.JsonGenerator
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import groovyx.gpars.dataflow.DataflowWriteChannel
import groovyx.gpars.dataflow.DataflowReadChannel
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Matcher
import java.util.regex.Pattern
import nextflow.extension.CH
import nextflow.extension.DataflowHelper
import nextflow.Channel
import nextflow.Global
import nextflow.Nextflow
import nextflow.plugin.extension.Operator
import nextflow.plugin.extension.Function
import nextflow.plugin.extension.PluginExtensionPoint
import nextflow.script.WorkflowMetadata
import nextflow.Session
import nextflow.util.Duration
import nextflow.util.MemoryUnit
import nextflow.config.ConfigMap
import org.json.JSONException
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import org.yaml.snakeyaml.Yaml

import nextflow.validation.config.ValidationConfig
import nextflow.validation.exceptions.SchemaValidationException
import nextflow.validation.help.HelpMessage
import nextflow.validation.validators.JsonSchemaValidator
import nextflow.validation.samplesheet.SamplesheetConverter
import nextflow.validation.parameters.ParameterValidator
import static nextflow.validation.utils.Colors.logColors
import static nextflow.validation.utils.Files.paramsLoad
import static nextflow.validation.utils.Common.getSchemaPath
import static nextflow.validation.utils.Common.paramsMaxChars
import static nextflow.validation.utils.Common.findDeep

/**
 * @author : mirpedrol <mirp.julia@gmail.com>
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 * @author : KevinMenden
 */

@Slf4j
@CompileStatic
class ValidationExtension extends PluginExtensionPoint {

    // The configuration class
    private ValidationConfig config

    // The session
    private Session session

    @Override
    protected void init(Session session) {
        def plugins = session?.config?.navigate("plugins") as ArrayList
        // TODO move the warning to an observer
        if(plugins?.contains("nf-schema")) {
            log.warn("""
!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
!                                                                 !
!                            WARNING!                             !
!                                                                 !
!                You just entered the danger zone!                !
!         Please pin the nf-schema version in your config!        !
!   Not pinning your version can't guarantee the reproducibility  !
!       and the functionality of this pipeline in the future      !
!                                                                 !
!                    plugins {                                    !
!                        id "nf-schema@<version>"                 !
!                    }                                            !
!                                                                 !
!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            """)
        }

        this.session = session

        // Help message logic
        def Map params = (Map)session.params ?: [:]
        config = new ValidationConfig(session?.config?.navigate('validation') as Map, params)

    }

    @Function
    public List samplesheetToList(
        final CharSequence samplesheet,
        final CharSequence schema,
        final Map options = null
    ) {
        def Path samplesheetFile = Nextflow.file(samplesheet) as Path
        return samplesheetToList(samplesheetFile, schema, options)
    }

    @Function
    public List samplesheetToList(
        final Path samplesheet,
        final CharSequence schema,
        final Map options = null
    ) {
        def String fullPathSchema = getSchemaPath(session.baseDir.toString(), schema as String)
        def Path schemaFile = Nextflow.file(fullPathSchema) as Path
        return samplesheetToList(samplesheet, schemaFile, options)
    }

    @Function
    public List samplesheetToList(
        final CharSequence samplesheet,
        final Path schema,
        final Map options = null
    ) {
        def Path samplesheetFile = Nextflow.file(samplesheet) as Path
        return samplesheetToList(samplesheetFile, schema, options)
    }

    @Function
    public List samplesheetToList(
        final Path samplesheet,
        final Path schema,
        final Map options = null
    ) {
        def SamplesheetConverter converter = new SamplesheetConverter(config)
        def List output = converter.validateAndConvertToList(samplesheet, schema, options)
        return output
    }

    //
    // Initialise expected params if not present
    //
    private Map initialiseExpectedParams(Map params) {
        addExpectedParams().each { param ->
            params[param] = false
        }
        return params
    }

    //
    // Add expected params
    //
    private List addExpectedParams() {
        def List expectedParams = [
            config.help.shortParameter,
            config.help.fullParameter,
            config.help.showHiddenParameter
        ]

        return expectedParams
    }

    /*
    * Function to loop over all parameters defined in schema and check
    * whether the given parameters adhere to the specifications
    */
    @Function
    void validateParameters(
        Map options = null
    ) {
        def Map params = initialiseExpectedParams(session.params)
        def validator = new ParameterValidator(config)
        validator.validateParametersMap(
            options,
            params,
            session.baseDir.toString(),
            addExpectedParams()
        )
    }

    //
    // Beautify parameters for --help
    //
    @Function
    public String paramsHelp(
        Map options = [:],
        String command
    ) {
        if (!options.containsKey("hideWarning") || options.hideWarning == false) {
            log.warn("""
Using `paramsHelp()` is not recommended. Check out the help message migration guide: https://nextflow-io.github.io/nf-schema/latest/migration_guide/#updating-the-help-message
If you intended to use this function, please add the following option to the input of the function:
    `hideWarning: true`

Please contact the pipeline maintainer(s) if you see this warning as a user.
            """)
        }

        def Map params = session.params
        def Map validationConfig = (Map)session.config.navigate("validation") ?: [:]
        validationConfig.parametersSchema = options.containsKey('parameters_schema') ? options.parameters_schema as String : validationConfig.parametersSchema
        validationConfig.help = (Map)(validationConfig.help ?: [:]) + [command: command, beforeText: "", afterText: ""]
        def ValidationConfig copyConfig = new ValidationConfig(validationConfig, params)
        def HelpMessage helpMessage = new HelpMessage(copyConfig, session)
        def String help = helpMessage.getBeforeText()
        def List<String> helpBodyLines = helpMessage.getShortHelpMessage(params.help && params.help instanceof String ? params.help : "").readLines()
        help += helpBodyLines.findAll {
            // Remove added ungrouped help parameters
            !it.startsWith("--${copyConfig.help.shortParameter}") && 
            !it.startsWith("--${copyConfig.help.fullParameter}") && 
            !it.startsWith("--${copyConfig.help.showHiddenParameter}")
        }.join("\n")
        help += helpMessage.getAfterText()
        return help
    }

    //
    // Groovy Map summarising parameters/workflow options used by the pipeline
    //
    @Function
    public LinkedHashMap paramsSummaryMap(
        Map options = null,
        WorkflowMetadata workflow
        ) {
        
        def String schemaFilename = options?.containsKey('parameters_schema') ? options.parameters_schema as String : config.parametersSchema
        def Map params = session.params
        
        // Get a selection of core Nextflow workflow options
        def Map workflowSummary = [:]
        if (workflow.revision) {
            workflowSummary['revision'] = workflow.revision
        }
        workflowSummary['runName']      = workflow.runName
        if (workflow.containerEngine) {
            workflowSummary['containerEngine'] = workflow.containerEngine
        }
        if (workflow.container) {
            workflowSummary['container'] = workflow.container
        }

        workflowSummary['launchDir']    = workflow.launchDir
        workflowSummary['workDir']      = workflow.workDir
        workflowSummary['projectDir']   = workflow.projectDir
        workflowSummary['userName']     = workflow.userName
        workflowSummary['profile']      = workflow.profile
        workflowSummary['configFiles']  = workflow.configFiles ? workflow.configFiles.join(', ') : ''

        // Get pipeline parameters defined in JSON Schema
        def Map paramsSummary = [:]
        def Map paramsMap = paramsLoad( Path.of(getSchemaPath(session.baseDir.toString(), schemaFilename)) )
        for (group in paramsMap.keySet()) {
            def Map groupSummary = getSummaryMapFromParams(params, paramsMap.get(group) as Map)
            config.summary.hideParams.each { hideParam ->
                def List<String> hideParamList = hideParam.tokenize(".") as List<String>
                def Integer indexCounter = 0
                def Map nestedSummary = groupSummary
                if(hideParamList.size() >= 2 ) {
                    hideParamList[0..-2].each { it ->
                        nestedSummary = nestedSummary?.get(it, null)
                    }
                }
                if(nestedSummary != null ) {
                    nestedSummary.remove(hideParamList[-1])
                }
            }
            paramsSummary.put(group, groupSummary)
        }
        paramsSummary.put('Core Nextflow options', workflowSummary)
        return paramsSummary
    }


    //
    // Create a summary map for the given parameters
    //
    private Map getSummaryMapFromParams(Map params, Map paramsSchema) {
        def Map summary = [:]
        for (String param in paramsSchema.keySet()) {
            if (params.containsKey(param)) {
                def Map schema = paramsSchema.get(param) as Map 
                if (params.get(param) instanceof Map && schema.containsKey("properties")) {
                    summary.put(param, getSummaryMapFromParams(params.get(param) as Map, schema.get("properties") as Map))
                    continue
                }
                def String value = params.get(param)
                def String defaultValue = schema.get("default")
                def String type = schema.type
                if (defaultValue != null) {
                    if (type == 'string') {
                        // TODO rework this in a more flexible way
                        if (defaultValue.contains('$projectDir') || defaultValue.contains('${projectDir}')) {
                            def sub_string = defaultValue.replace('\$projectDir', '')
                            sub_string     = sub_string.replace('\${projectDir}', '')
                            if (value.contains(sub_string)) {
                                defaultValue = value
                            }
                        }
                        if (defaultValue.contains('$params.outdir') || defaultValue.contains('${params.outdir}')) {
                            def sub_string = defaultValue.replace('\$params.outdir', '')
                            sub_string     = sub_string.replace('\${params.outdir}', '')
                            if ("${params.outdir}${sub_string}" == value) {
                                defaultValue = value
                            }
                        }
                    }
                }

                // We have a default in the schema, and this isn't it
                if (defaultValue != null && value != defaultValue) {
                    summary.put(param, value)
                }
                // No default in the schema, and this isn't empty or false
                else if (defaultValue == null && value != "" && value != null && value != false && value != 'false') {
                    summary.put(param, value)
                }
            }
        }
        return summary
    }

    //
    // Beautify parameters for summary and return as string
    //
    @Function
    public String paramsSummaryLog(
        Map options = null,
        WorkflowMetadata workflow
    ) {

        def Map params = session.params

        def String schemaFilename = options?.containsKey('parameters_schema') ? options.parameters_schema as String : config.parametersSchema

        def colors = logColors(config.monochromeLogs)
        String output  = ''
        output += config.summary.beforeText
        def Map paramsMap = paramsSummaryMap(workflow, parameters_schema: schemaFilename)
        paramsMap.each { key, value ->
            paramsMap[key] = flattenNestedParamsMap(value as Map)
        }
        def maxChars  = paramsMaxChars(paramsMap)
        for (group in paramsMap.keySet()) {
            def Map group_params = paramsMap.get(group) as Map // This gets the parameters of that particular group
            if (group_params) {
                output += "$colors.bold$group$colors.reset\n"
                for (String param in group_params.keySet()) {
                    output += "  " + colors.blue + param.padRight(maxChars) + ": " + colors.green +  group_params.get(param) + colors.reset + '\n'
                }
                output += '\n'
            }
        }
        output += "!! Only displaying parameters that differ from the pipeline defaults !!\n"
        output += "-${colors.dim}----------------------------------------------------${colors.reset}-"
        output += config.summary.afterText
        return output
    }

    private Map flattenNestedParamsMap(Map paramsMap) {
        def Map returnMap = [:]
        paramsMap.each { param, value ->
            def String key = param as String
            if (value instanceof Map) {
                def Map flatMap = flattenNestedParamsMap(value as Map)
                flatMap.each { flatParam, flatValue ->
                    returnMap.put(key + "." + flatParam, flatValue)
                }
            } else {
                returnMap.put(key, value)
            }
        }
        return returnMap
    }
}
