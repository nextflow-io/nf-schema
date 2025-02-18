package nextflow.validation

import groovy.json.JsonBuilder
import org.json.JSONObject
import org.json.JSONArray

import groovy.util.logging.Slf4j
import java.nio.file.Files
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
        def String schemaString = Files.readString( Path.of(getBasePath(session.baseDir.toString(), schema)) )
        def List<String> errors = validator.validateObj(jsonObj, schemaString)[0]
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
        def HelpMessageCreator helpCreator = new HelpMessageCreator(copyConfig, session)
        def String help = helpCreator.getBeforeText()
        def List<String> helpBodyLines = helpCreator.getShortMessage(params.help && params.help instanceof String ? params.help : "").readLines()
        help += helpBodyLines.findAll {
            // Remove added ungrouped help parameters
            !it.startsWith("--${copyConfig.help.shortParameter}") && 
            !it.startsWith("--${copyConfig.help.fullParameter}") && 
            !it.startsWith("--${copyConfig.help.showHiddenParameter}")
        }.join("\n")
        help += helpCreator.getAfterText()
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
