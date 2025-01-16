package nextflow.validation.parameters

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
class ParameterValidator {

    private ValidationConfig config

    ParameterValidator(ValidationConfig config) {
        this.config = config
    }

    final List<String> NF_OPTIONS = [
            // Options for base `nextflow` command
            'bg',
            'c',
            'C',
            'config',
            'd',
            'D',
            'dockerize',
            'h',
            'log',
            'q',
            'quiet',
            'syslog',
            'v',

            // Options for `nextflow run` command
            'ansi',
            'ansi-log',
            'bg',
            'bucket-dir',
            'c',
            'cache',
            'config',
            'dsl2',
            'dump-channels',
            'dump-hashes',
            'E',
            'entry',
            'latest',
            'lib',
            'main-script',
            'N',
            'name',
            'offline',
            'params-file',
            'pi',
            'plugins',
            'poll-interval',
            'pool-size',
            'profile',
            'ps',
            'qs',
            'queue-size',
            'r',
            'resume',
            'revision',
            'stdin',
            'stub',
            'stub-run',
            'test',
            'w',
            'with-charliecloud',
            'with-conda',
            'with-dag',
            'with-docker',
            'with-mpi',
            'with-notification',
            'with-podman',
            'with-report',
            'with-singularity',
            'with-timeline',
            'with-tower',
            'with-trace',
            'with-weblog',
            'without-docker',
            'without-podman',
            'work-dir'
    ]

    private List<String> errors = []
    private List<String> warnings = []

    // The amount of parameters hidden (for help messages)
    private Integer hiddenParametersCount = 0

    // The length of the terminal
    private Integer terminalLength = System.getenv("COLUMNS")?.toInteger() ?: 100


    private boolean hasErrors() { errors.size()>0 }
    private List<String> getErrors() { errors }

    private boolean hasWarnings() { warnings.size()>0 }
    private List<String> getWarnings() { warnings }

    public validateParametersMap(
        Map options = null,
        Map params = [:],
        String baseDir,
        List expectedDefaultParams
    ) {
        
        def String schemaFilename = options?.containsKey('parameters_schema') ? options.parameters_schema as String : config.parametersSchema
        log.debug "Starting parameters validation"

        // Clean the parameters
        def cleanedParams = cleanParameters(params)
        // Convert to JSONObject
        def paramsJSON = new JSONObject(new JsonBuilder(cleanedParams).toString())

        //=====================================================================//
        // Check for nextflow core params and unexpected params
        def slurper = new JsonSlurper()
        def Map parsed = (Map) slurper.parse( Path.of(getSchemaPath(baseDir, schemaFilename)) )
        // $defs is the adviced keyword for definitions. Keeping defs in for backwards compatibility
        def Map schemaParams = (Map) (parsed.get('$defs') ?: parsed.get("defs"))
        def specifiedParamKeys = params.keySet()

        // Collect expected parameters from the schema
        def enumsTuple = collectEnums(schemaParams)
        def List expectedParams = (List) enumsTuple[0] + expectedDefaultParams
        def Map enums = (Map) enumsTuple[1]
        // Collect expected parameters from the schema when parameters are specified outside of "$defs"
        if (parsed.containsKey('properties')) {
            def enumsTupleTopLevel = collectEnums(['top_level': ['properties': parsed.get('properties')]])
            expectedParams += (List) enumsTupleTopLevel[0]
            enums += (Map) enumsTupleTopLevel[1]
        }

        //=====================================================================//
        for (String specifiedParam in specifiedParamKeys) {
            // nextflow params
            if (NF_OPTIONS.contains(specifiedParam)) {
                errors << "You used a core Nextflow option with two hyphens: '--${specifiedParam}'. Please resubmit with '-${specifiedParam}'".toString()
            }
            // unexpected params
            def expectedParamsLowerCase = expectedParams.collect{ it -> 
                def String p = it
                p.replace("-", "").toLowerCase() 
            }
            def specifiedParamLowerCase = specifiedParam.replace("-", "").toLowerCase()
            def isCamelCaseBug = (specifiedParam.contains("-") && !expectedParams.contains(specifiedParam) && expectedParamsLowerCase.contains(specifiedParamLowerCase))
            if (!expectedParams.contains(specifiedParam) && !config.ignoreParams.contains(specifiedParam) && !isCamelCaseBug) {
                if (config.failUnrecognisedParams) {
                    errors << "* --${specifiedParam}: ${params[specifiedParam]}".toString()
                } else {
                    warnings << "* --${specifiedParam}: ${params[specifiedParam]}".toString()
                }
            }
        }

        //=====================================================================//
        // Validate parameters against the schema
        def String schema_string = Files.readString( Path.of(getSchemaPath(baseDir, schemaFilename)) )
        def validator = new JsonSchemaValidator(config)

        // check for warnings
        if( this.hasWarnings() ) {
            def msg = "The following invalid input values have been detected:\n\n" + this.getWarnings().join('\n').trim() + "\n\n"
            log.warn(msg)
        }

        // Colors
        def colors = logColors(config.monochromeLogs)

        // Validate
        List<String> validationErrors = validator.validate(paramsJSON, schema_string)
        this.errors.addAll(validationErrors)
        def List<String> modifiedIgnoreParams = config.ignoreParams.collect { param -> "* --${param}" as String }
        def List<String> filteredErrors = errors.findAll { error -> 
            return modifiedIgnoreParams.find { param -> error.startsWith(param) } == null
        }
        if (filteredErrors.size() > 0) {
            def msg = "${colors.red}The following invalid input values have been detected:\n\n" + filteredErrors.join('\n').trim() + "\n${colors.reset}\n"
            log.error("Validation of pipeline parameters failed!")
            throw new SchemaValidationException(msg, this.getErrors())
        }

        log.debug "Finishing parameters validation"
    }

    //
    // Function to collect enums (options) of a parameter and expected parameters (present in the schema)
    //
    private Tuple collectEnums(Map schemaParams) {
        def expectedParams = []
        def enums = [:]
        for (group in schemaParams) {
            def Map properties = (Map) group.value['properties']
            for (p in properties) {
                def String key = (String) p.key
                expectedParams.push(key)
                def Map property = properties[key] as Map
                if (property.containsKey('enum')) {
                    enums[key] = property['enum']
                }
            }
        }
        return new Tuple (expectedParams, enums)
    }

    //
    // Clean and check parameters relative to Nextflow native classes
    //
    private Map cleanParameters(Map params) {
        def Map new_params = (Map) params.getClass().newInstance(params)
        for (p in params) {
            // remove anything evaluating to false
            if (!p['value'] && p['value'] != 0) {
                new_params.remove(p.key)
            }
            // Cast MemoryUnit to String
            if (p['value'] instanceof MemoryUnit) {
                new_params.replace(p.key, p['value'].toString())
            }
            // Cast Duration to String
            if (p['value'] instanceof Duration) {
                new_params.replace(p.key, p['value'].toString())
            }
            // Cast LinkedHashMap to String
            if (p['value'] instanceof LinkedHashMap) {
                new_params.replace(p.key, p['value'].toString())
            }
            // Parsed nested parameters
            if (p['value'] instanceof Map) {
                new_params.replace(p.key, cleanParameters(p['value'] as Map))
            }
        }
        return new_params
    }
}