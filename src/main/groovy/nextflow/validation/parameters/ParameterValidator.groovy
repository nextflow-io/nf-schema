package nextflow.validation.parameters

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import nextflow.Nextflow
import nextflow.util.Duration
import nextflow.util.MemoryUnit
import org.json.JSONObject

import nextflow.validation.config.ValidationConfig
import nextflow.validation.exceptions.SchemaValidationException
import nextflow.validation.validators.JsonSchemaValidator
import static nextflow.validation.utils.Colors.getLogColors
import static nextflow.validation.utils.Common.getBasePath
import static nextflow.validation.utils.Common.getValueFromJsonPointer

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
        Map inputParams = [:],
        String baseDir
    ) {
        def Map params = initialiseExpectedParams(inputParams)
        def String schemaFilename = options?.containsKey('parameters_schema') ? options.parameters_schema as String : config.parametersSchema
        log.debug "Starting parameters validation"

        // Clean the parameters
        def cleanedParams = cleanParameters(params)
        // Convert to JSONObject
        def paramsJSON = new JSONObject(new JsonBuilder(cleanedParams).toString())

        //=====================================================================//
        // Validate parameters against the schema
        def validator = new JsonSchemaValidator(config)

        // Colors
        def colors = getLogColors(config.monochromeLogs)

        // Validate
        Tuple2<List<String>,List<String>> validationResult = validator.validate(paramsJSON, getBasePath(baseDir, schemaFilename))
        def validationErrors = validationResult[0]
        def unevaluatedParams = validationResult[1]
        this.errors.addAll(validationErrors)

        //=====================================================================//
        // Check for nextflow core params and unexpected params
        //=====================================================================//
        unevaluatedParams.each{ param ->
            def String dotParam = param.replaceAll("/", ".")
            if (NF_OPTIONS.contains(param)) {
                errors << "You used a core Nextflow option with two hyphens: '--${param}'. Please resubmit with '-${param}'".toString()
            }
            else if (!config.ignoreParams.any { dotParam == it || dotParam.startsWith(it + ".") } ) { // Check if an ignore param is present
                def String text = "* --${param.replaceAll("/", ".")}: ${getValueFromJsonPointer("/"+param, paramsJSON)}".toString()
                if(config.failUnrecognisedParams) {
                    errors << text
                } else {
                    warnings << text
                }
            }
        }
        // check for warnings
        if( this.hasWarnings() ) {
            def msg = "The following invalid input values have been detected:\n\n" + this.getWarnings().join('\n').trim() + "\n\n"
            log.warn(msg)
        }

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

    //
    // Initialise expected params if not present
    //
    private Map initialiseExpectedParams(Map params) {
        getExpectedParams().each { param ->
            params[param] = false
        }
        return params
    }

    //
    // Add expected params
    //
    private List getExpectedParams() {
        def List expectedParams = [
            config.help.shortParameter,
            config.help.fullParameter,
            config.help.showHiddenParameter
        ]

        return expectedParams
    }
}