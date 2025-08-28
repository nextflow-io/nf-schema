package nextflow.validation

import groovy.json.JsonBuilder
import org.json.JSONObject
import org.json.JSONArray

import groovy.util.logging.Slf4j
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
import static nextflow.validation.utils.Colors.getLogColors
import static nextflow.validation.utils.Common.getBasePath
import static nextflow.validation.utils.Common.getLongestKeyLength

/**
 * @author : mirpedrol <mirp.julia@gmail.com>
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 * @author : KevinMenden
 */

@Slf4j
class ValidationExtension extends PluginExtensionPoint {

    // The configuration class
    private ValidationConfig config

    // The session
    private Session session

    @Override
    protected void init(Session session) {
        this.session = session
        config = new ValidationConfig(session?.config?.navigate('validation') as Map, session)

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
        def String fullPathSchema = getBasePath(session.baseDir.toString(), schema as String)
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

    /*
    * Function to loop over all parameters defined in schema and check
    * whether the given parameters adhere to the specifications
    */
    @Function
    void validateParameters(
        Map options = null
    ) {
        def ParameterValidator validator = new ParameterValidator(config)
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
        def Boolean exitOnError = options?.containsKey("exitOnError") ? options.exitOnError : true

        def JsonSchemaValidator validator = new JsonSchemaValidator(config)
        def Object jsonObj = null
        if(input instanceof List) {
            jsonObj = new JSONArray(new JsonBuilder(input).toString())
        } else if(input instanceof Map) {
            jsonObj = new JSONObject(new JsonBuilder(input).toString())
        } else {
            jsonObj = input
        }
        def ValidationResult result = validator.validate(jsonObj, getBasePath(session.baseDir.toString(), schema))
        def List<String> errors = result.getErrors('object')
        if(exitOnError && errors != []) {
            def colors = getLogColors(config.monochromeLogs)
            def String msg = "${colors.red}${errors.join('\n')}${colors.reset}\n"
            throw new SchemaValidationException(msg)
        } 
        return errors
    }

    //
    // Beautify parameters for --help
    //
    @Function
    public String paramsHelp(
        final Map options = [:]
    ) {
        return paramsHelp(options, "")
    }

    @Function
    public String paramsHelp(
        final Map options = [:],
        final String parameter
    ) {
        log.debug "Generating help message with options: ${options}"
        def Map config = session.config.navigate("validation") ?: [:]

        // Adapt config options with function options
        config.parametersSchema = options.get('parameters_schema', config.get("parametersSchema", "nextflow_schema.json")) as String
        config.help = config.help ?: [:]
        config.help.enabled = true
        config.help.beforeText = options.get('beforeText', config.help.get("beforeText", "")) as String
        config.help.afterText = options.get('afterText', config.help.get("afterText", "")) as String
        config.help.command = options.get('command', config.help.get("command", "")) as String
        config.help.showHidden = options.get('showHidden', false) as Boolean

        // Get function logic options
        def Boolean fullHelp = options.get('fullHelp') as Boolean ?: false

        // Generate the new help config
        def final ValidationConfig functionConfig = new ValidationConfig(config, session)

        // Create the help message
        def HelpMessageCreator helpCreator = new HelpMessageCreator(functionConfig, session)
        def String help = helpCreator.getBeforeText()
        def String helpBodyLines = fullHelp ? helpCreator.getFullMessage() : helpCreator.getShortMessage(parameter)
        help += helpBodyLines.readLines().findAll {
            // Remove added ungrouped help parameters
            !it.startsWith("--${functionConfig.help.shortParameter}") && 
            !it.startsWith("--${functionConfig.help.fullParameter}") && 
            !it.startsWith("--${functionConfig.help.showHiddenParameter}")
        }.join("\n")
        help += helpCreator.getAfterText()
        log.debug "Done generating help message"
        return help
    }

    //
    // Groovy Map summarising parameters/workflow options used by the pipeline
    //
    @Function
    public Map paramsSummaryMap(
        Map options = null,
        WorkflowMetadata workflow
        ) {
        def SummaryCreator creator = new SummaryCreator(config)
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
    public String paramsSummaryLog(
        Map options = null,
        WorkflowMetadata workflow
    ) {

        def Map params = session.params

        def String schemaFilename = options?.containsKey('parameters_schema') ? options.parameters_schema as String : config.parametersSchema

        def colors = getLogColors(config.monochromeLogs)
        String output  = ''
        output += config.summary.beforeText
        def Map paramsMap = paramsSummaryMap(workflow, parameters_schema: schemaFilename)
        paramsMap.each { key, value ->
            paramsMap[key] = flattenNestedParamsMap(value as Map)
        }
        def maxChars  = getLongestKeyLength(paramsMap)
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
