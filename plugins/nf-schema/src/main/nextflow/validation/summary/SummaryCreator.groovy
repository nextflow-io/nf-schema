package nextflow.validation.summary

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
class SummaryCreator {

    final private ValidationConfig config

    SummaryCreator(ValidationConfig config) {
        this.config = config
    }

    public Map createSummaryMap(
        Map options,
        WorkflowMetadata workflow,
        String baseDir,
        Map params
    ) {
        def String schemaFilename = options?.containsKey('parameters_schema') ? options.parameters_schema as String : config.parametersSchema
        
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
        def Map paramsMap = paramsLoad( Path.of(getSchemaPath(baseDir, schemaFilename)) )
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

}
