package nextflow.validation

import static nextflow.validation.utils.Colors.getLogColors
import static nextflow.validation.utils.Common.getBasePath
import static nextflow.validation.utils.Common.getLongestKeyLength

import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import groovy.transform.CompileDynamic

import org.json.JSONObject
import org.json.JSONArray

import java.nio.file.Path

import nextflow.Nextflow
import nextflow.plugin.extension.Function
import nextflow.plugin.extension.PluginExtensionPoint
import nextflow.script.WorkflowMetadata
import nextflow.Session

import nextflow.validation.config.ValidationConfig
import nextflow.validation.exceptions.SchemaValidationException
import nextflow.validation.help.HelpMessageCreator
import nextflow.validation.samplesheet.SamplesheetConverter
import nextflow.validation.summary.SummaryCreator
import nextflow.validation.parameters.ParameterValidator
import nextflow.validation.validators.JsonSchemaValidator
import nextflow.validation.validators.ValidationResult

/**
 * @author : mirpedrol <mirp.julia@gmail.com>
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 * @author : KevinMenden
 */

@Slf4j
@CompileDynamic
class ValidationExtension extends PluginExtensionPoint {

    // The configuration class
    private ValidationConfig config

    // The session
    private Session session

    @Function
    List samplesheetToList(
        final CharSequence samplesheet,
        final CharSequence schema
    ) {
        Path samplesheetFile = Nextflow.file(samplesheet) as Path
        return samplesheetToList(samplesheetFile, schema)
    }

    @Function
    List samplesheetToList(
        final Path samplesheet,
        final CharSequence schema
    ) {
        String fullPathSchema = getBasePath(session.baseDir.toString(), schema as String)
        Path schemaFile = Nextflow.file(fullPathSchema) as Path
        return samplesheetToList(samplesheet, schemaFile)
    }

    @Function
    List samplesheetToList(
        final CharSequence samplesheet,
        final Path schema
    ) {
        Path samplesheetFile = Nextflow.file(samplesheet) as Path
        return samplesheetToList(samplesheetFile, schema)
    }

    @Function
    List samplesheetToList(
        final Path samplesheet,
        final Path schema
    ) {
        SamplesheetConverter converter = new SamplesheetConverter(config)
        List output = converter.validateAndConvertToList(samplesheet, schema)
        return output
    }

    /*
    * Function to loop over all parameters defined in schema and check
    * whether the given parameters adhere to the specifications
    */
    @Function
    void validateParameters(
        Map options = null
    ) {
        ParameterValidator validator = new ParameterValidator(config)
        validator.validateParametersMap(
            options,
            session.params,
            session.baseDir.toString()
        )
    }

    /*
    * Function to validate any value against a schema
    */
    @Function
    List<String> validate(
        final Map options = [:],
        final Object input,
        final Path schema
    ) {
        return validate(input, schema.toUri(), options)
    }

    @Function
    List<String> validate(
        final Map options = [:],
        final Object input,
        final String schema
    ) {
        Boolean exitOnError = options?.containsKey('exitOnError') ? options.exitOnError : true

        JsonSchemaValidator validator = new JsonSchemaValidator(config)
        Object jsonObj = null
        if (input in List) {
            jsonObj = new JSONArray(new JsonBuilder(input).toString())
        } else if (input in Map) {
            jsonObj = new JSONObject(new JsonBuilder(input).toString())
        } else {
            jsonObj = input
        }
        ValidationResult result = validator.validate(jsonObj, getBasePath(session.baseDir.toString(), schema))
        List<String> errors = result.getErrors('object')
        if (exitOnError && errors != []) {
            Map<String, String> colors = getLogColors(config.monochromeLogs)
            String msg = "${colors.red}${errors.join('\n')}${colors.reset}\n"
            throw new SchemaValidationException(msg)
        }
        return errors
    }

    //
    // Beautify parameters for --help
    //
    @Function
    String paramsHelp(
        final Map options = [:]
    ) {
        return paramsHelp(options, '')
    }

    @Function
    String paramsHelp(
        final Map options = [:],
        final String parameter
    ) {
        log.debug "Generating help message with options: ${options}"
        Map config = session.config.navigate('validation') ?: [:]

        // Adapt config options with function options
        config.parametersSchema = options.get(
            'parameters_schema',
            config.get('parametersSchema', 'nextflow_schema.json')
        ) as String
        config.help = config.help ?: [:]
        config.help.enabled = true
        config.help.beforeText = options.get('beforeText', config.help.get('beforeText', '')) as String
        config.help.afterText = options.get('afterText', config.help.get('afterText', '')) as String
        config.help.command = options.get('command', config.help.get('command', '')) as String
        config.help.showHidden = options.get('showHidden', false) as Boolean

        // Get function logic options
        Boolean fullHelp = options.get('fullHelp') as Boolean ?: false

        // Generate the new help config
        final ValidationConfig functionConfig = new ValidationConfig(config, session)

        // Create the help message
        HelpMessageCreator helpCreator = new HelpMessageCreator(functionConfig, session)
        String help = helpCreator.beforeText
        String helpBodyLines = fullHelp ? helpCreator.fullMessage : helpCreator.getShortMessage(parameter)
        help += helpBodyLines.readLines().findAll { line ->
            // Remove added ungrouped help parameters
            !line.startsWith("--${functionConfig.help.shortParameter}") &&
            !line.startsWith("--${functionConfig.help.fullParameter}") &&
            !line.startsWith("--${functionConfig.help.showHiddenParameter}")
        }.join('\n')
        help += helpCreator.afterText
        log.debug 'Done generating help message'
        return help
    }

    //
    // Groovy Map summarising parameters/workflow options used by the pipeline
    //
    @Function
    Map paramsSummaryMap(
        Map options = null,
        WorkflowMetadata workflow
        ) {
        SummaryCreator creator = new SummaryCreator(config)
        return creator.getSummaryMap(
            options,
            workflow,
            session.baseDir.toString(),
            session.params
        )
    }

    //
    // Beautify parameters for summary and return as string
    //
    @Function
    String paramsSummaryLog(
        Map options = [:],
        WorkflowMetadata workflow
    ) {
        String schemaFilename = options.get('parameters_schema') ?: config.parametersSchema
        String beforeText = options?.get('beforeText') as String ?: config.summary.beforeText ?: ''
        String afterText = options?.get('afterText') as String ?: config.summary.afterText ?: ''

        Map<String, String> colors = getLogColors(config.monochromeLogs)
        String output  = ''
        output += beforeText
        Map paramsMap = paramsSummaryMap(workflow, parameters_schema: schemaFilename)
        Map containers = paramsMap.get('Core Nextflow options')?.get('container')
        if (containers) {
            log.debug "Containers specified in config:\n${containers.collect { entry ->
                "    ${entry.key}: ${entry.value}" }.join('\n')}"
            paramsMap['Core Nextflow options'] = paramsMap['Core Nextflow options']
                .findAll { entry -> entry.key != 'container' }
        }

        paramsMap.each { key, value ->
            paramsMap[key] = flattenNestedParamsMap(value as Map)
        }
        Integer maxChars  = getLongestKeyLength(paramsMap)
        for (group in paramsMap.keySet()) {
            Map groupParams = paramsMap.get(group) as Map // This gets the parameters of that particular group
            if (groupParams) {
                output += "$colors.bold$group$colors.reset\n"
                groupParams.keySet().each { param ->
                    output += '  ' +
                        colors.blue +
                        param.padRight(maxChars) +
                        ': ' +
                        colors.green +
                        groupParams.get(param) +
                        colors.reset +
                        '\n'
                }
                output += '\n'
            }
        }
        output += '!! Only displaying parameters that differ from the pipeline defaults !!\n'
        output += "-${colors.dim}----------------------------------------------------${colors.reset}-"
        output += afterText
        return output
    }

    @Override
    protected void init(Session session) {
        this.session = session
        config = new ValidationConfig(session?.config?.navigate('validation') as Map, session)
    }

    private Map flattenNestedParamsMap(Map paramsMap) {
        Map returnMap = [:]
        paramsMap.each { param, value ->
            def String key = param as String
            if (value in Map) {
                def Map flatMap = flattenNestedParamsMap(value as Map)
                flatMap.each { flatParam, flatValue ->
                    returnMap.put(key + '.' + flatParam, flatValue)
                }
            } else {
                returnMap.put(key, value)
            }
        }
        return returnMap
    }

}
