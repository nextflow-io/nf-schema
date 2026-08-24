package nextflow.validation

import static nextflow.validation.utils.Colors.getLogColors
import static nextflow.validation.utils.Common.getBasePath
import static nextflow.validation.utils.Common.getLongestKeyLength

import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import groovy.transform.CompileStatic

import org.json.JSONObject
import org.json.JSONArray

import java.nio.file.Path

import nextflow.Nextflow
import nextflow.plugin.extension.Function
import nextflow.plugin.extension.PluginExtensionPoint
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
@CompileStatic
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
        Path schemaPath = getBasePath(session.baseDir, schema as String)
        return samplesheetToList(samplesheet, schemaPath)
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
        final Map options = [:]
    ) {
        ParameterValidator validator = new ParameterValidator(config)
        validator.validateParametersMap(
            options,
            session
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
        JSONObject schemaJson = new JSONObject(schema.text)
        ValidationResult result = validator.validate(jsonObj, schemaJson)
        List<String> errors = result.getErrors('object')
        if (exitOnError && errors != []) {
            Map<String, String> colors = getLogColors(config.monochromeLogs)
            String msg = "${colors.red}${errors.join('\n')}${colors.reset}\n"
            throw new SchemaValidationException(msg)
        }
        return errors
    }

    @Function
    List<String> validate(
        final Map options = [:],
        final Object input,
        final String schema
    ) {
        return validate(options, input, getBasePath(session.baseDir, schema))
    }

    //
    // Beautify parameters for --help
    //
    @Function
    String paramsHelp(
        final Map options = [:]
    ) {
        log.debug "Generating help message with options: ${options}"
        Map config = (session.config.navigate('validation') ?: [:]) as Map

        // Adapt config options with function options
        config.parametersSchema = options.get(
            'parameters_schema',
            config.get('parametersSchema', 'nextflow_schema.json')
        ) as String
        Map helpConfig = (config.help ?: [:]) as Map
        helpConfig.enabled = true
        helpConfig.beforeText = options.get('beforeText', helpConfig.get('beforeText', '')) as String
        helpConfig.afterText = options.get('afterText', helpConfig.get('afterText', '')) as String
        helpConfig.command = options.get('command', helpConfig.get('command', '')) as String
        helpConfig.showHidden = options.get('showHidden', false) as Boolean
        config.help = helpConfig

        // Set the parameter if given
        String parameter = options.get('parameter', null) as String

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
        if (!help.endsWith('\n')) {
            help += '\n'
        }
        help += helpCreator.afterText
        log.debug 'Done generating help message'
        return help
    }

    //
    // Groovy Map summarising parameters/workflow options used by the pipeline
    //
    @Function
    Map paramsSummaryMap(
        Map options = [:]
    ) {
        SummaryCreator creator = new SummaryCreator(config)
        return creator.getSummaryMap(
            options,
            session.workflowMetadata,
            session.baseDir,
            session.params
        )
    }

    //
    // Beautify parameters for summary and return as string
    //
    @Function
    String paramsSummaryLog(
        Map options = [:]
    ) {
        String schemaFilename = options.get('parameters_schema') ?: config.parametersSchema
        String beforeText = options?.get('beforeText') as String ?: config.summary.beforeText ?: ''
        String afterText = options?.get('afterText') as String ?: config.summary.afterText ?: ''

        Map<String, String> colors = getLogColors(config.monochromeLogs)
        String output  = ''
        output += beforeText
        Map paramsMap = paramsSummaryMap(parameters_schema: schemaFilename)
        Map coreOptions = paramsMap.get('Core Nextflow options') as Map
        Object containers = coreOptions?.get('container')
        if (containers in Map) {
            Map containerMap = (Map) containers
            log.debug "Containers specified in config:\n${containerMap.collect { key, value ->
                "    ${key}: ${value}" }.join('\n')}"
            paramsMap['Core Nextflow options'] = coreOptions
                .findAll { key, value -> key != 'container' }
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
                    String paramName = param as String
                    output += '  ' +
                        colors.blue +
                        paramName.padRight(maxChars) +
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
