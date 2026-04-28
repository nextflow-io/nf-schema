package nextflow.validation.summary

import static nextflow.validation.utils.FilesHelper.paramsLoad
import static nextflow.validation.utils.Common.getBasePath

import groovy.util.logging.Slf4j
import groovy.transform.CompileDynamic
import java.nio.file.Path
import nextflow.Nextflow
import nextflow.script.WorkflowMetadata

import nextflow.validation.config.ValidationConfig

/**
 * @author : mirpedrol <mirp.julia@gmail.com>
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 * @author : KevinMenden
 */

@Slf4j
@CompileDynamic
class SummaryCreator {

    final private ValidationConfig config

    SummaryCreator(ValidationConfig config) {
        this.config = config
    }

    Map getSummaryMap(
        Map options,
        WorkflowMetadata workflow,
        String baseDir,
        Map params
    ) {
        String schemaFilename = options?.containsKey('parameters_schema') ?
            options.parameters_schema as String :
            config.parametersSchema

        // Get a selection of core Nextflow workflow options
        Map workflowSummary = [:]
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

        workflowSummary['launchDir']    = maybeMask(workflow.launchDir)
        workflowSummary['workDir']      = maybeMask(workflow.workDir)
        workflowSummary['projectDir']   = maybeMask(workflow.projectDir)
        workflowSummary['userName']     = workflow.userName
        workflowSummary['profile']      = workflow.profile
        workflowSummary['configFiles']  = maybeMask(workflow.configFiles ? workflow.configFiles.join(', ') : '')

        // Get pipeline parameters defined in JSON Schema
        Map paramsSummary = [:]
        Map paramsMap = paramsLoad(Path.of(getBasePath(baseDir, schemaFilename)))
        for (group in paramsMap.keySet()) {
            Map groupSummary = getSummaryMapFromParams(params, paramsMap.get(group) as Map)
            config.summary.hideParams.each { hideParam ->
                List<String> hideParamList = hideParam.tokenize('.') as List<String>
                Map nestedSummary = groupSummary
                if (hideParamList.size() >= 2) {
                    hideParamList[0..-2].each { param ->
                        nestedSummary = nestedSummary?.get(param, null)
                    }
                }
                if (nestedSummary != null) {
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
        Map summary = [:]
        for (String param in paramsSchema.keySet()) {
            if (params.containsKey(param)) {
                Map schema = paramsSchema.get(param) as Map
                if (params.get(param) in Map && schema.containsKey('properties')) {
                    summary.put(
                        param,
                        getSummaryMapFromParams(params.get(param) as Map, schema.get('properties') as Map)
                    )
                    continue
                }
                String value = params.get(param)
                String defaultValue = schema.get('default')
                String type = schema.type
                if (defaultValue != null && type == 'string') {
                    // TODO rework this in a more flexible way
                    /* groovylint-disable-next-line GStringExpressionWithinString */
                    if (defaultValue.contains('$projectDir') || defaultValue.contains('${projectDir}')) {
                        /* groovylint-disable-next-line GStringExpressionWithinString */
                        String subString = defaultValue.replace('\$projectDir', '').replace('\${projectDir}', '')
                        if (value.contains(subString)) {
                            defaultValue = value
                        }
                    }
                    /* groovylint-disable-next-line GStringExpressionWithinString */
                    if (defaultValue.contains('$params.outdir') || defaultValue.contains('${params.outdir}')) {
                        String subString = defaultValue.replace('\$params.outdir', '')
                            /* groovylint-disable-next-line GStringExpressionWithinString */
                            .replace('\${params.outdir}', '')
                        if ("${params.outdir}${subString}" == value) {
                            defaultValue = value
                        }
                    }
                }

                // We have a default in the schema, and this isn't it
                if (defaultValue != null && value != defaultValue) {
                    summary.put(param, maybeMask(value))
                }
                // No default in the schema, and this isn't empty or false
                else if (defaultValue == null && value != '' && value != null && value != false && value != 'false') {
                    summary.put(param, maybeMask(value))
                }
            }
        }
        return summary
    }

    private CharSequence maybeMask(Path value) {
        return maybeMask(value.toUriString())
    }

    private CharSequence maybeMask(CharSequence value) {
        return maybeMaskSubpaths(value)
    }

    private CharSequence maybeMaskSubpaths(CharSequence value) {
        if(config.summary.maskSubpaths?.size() > 0) {
            config.summary.maskSubpaths.each { CharSequence toReplace ->
                value = value.replaceAll(toReplace, config.summary.mask)
            }
        }
        return value
    }
}
