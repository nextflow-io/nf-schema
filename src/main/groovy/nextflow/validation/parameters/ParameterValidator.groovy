package nextflow.validation.parameters

import static nextflow.validation.utils.Colors.getLogColors
import static nextflow.validation.utils.Common.getBasePath
import static nextflow.validation.utils.Common.getValueFromJsonPointer

import java.nio.file.Path
import groovy.json.JsonGenerator
import groovy.util.logging.Slf4j
import groovy.transform.CompileDynamic
import nextflow.Nextflow
import nextflow.util.Duration
import nextflow.util.MemoryUnit
import org.json.JSONObject

import nextflow.validation.config.ValidationConfig
import nextflow.validation.exceptions.SchemaValidationException
import nextflow.validation.validators.JsonSchemaValidator
import nextflow.validation.validators.ValidationResult

/**
 * @author : mirpedrol <mirp.julia@gmail.com>
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 * @author : KevinMenden
 */

@Slf4j
@CompileDynamic
class ParameterValidator {

    final private ValidationConfig config

    ParameterValidator(ValidationConfig config) {
        this.config = config
    }

    final List<String> nextflowOptions = [
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

    final private List<String> errors = []
    final private List<String> warnings = []

    void validateParametersMap(
        Map options = null,
        Map inputParams = [:],
        String baseDir
    ) {
        Map params = initialiseExpectedParams(inputParams)
        String schemaFilename = options?.containsKey('parameters_schema') ?
            options.parameters_schema as String :
            config.parametersSchema
        log.debug 'Starting parameters validation'

        // Clean the parameters
        Map cleanedParams = cleanParameters(params)
        // Convert to JSONObject
        JsonGenerator generator = new JsonGenerator.Options()
            .addConverter(Path) { Path path -> path.toUri().toString() }
            .build()
        JSONObject paramsJSON = new JSONObject(generator.toJson(cleanedParams))

        //=====================================================================//
        // Validate parameters against the schema
        JsonSchemaValidator validator = new JsonSchemaValidator(config)

        // Colors
        Map<String,String> colors = getLogColors(config.monochromeLogs)

        // Validate
        ValidationResult validationResult = validator.validate(paramsJSON, getBasePath(baseDir, schemaFilename))
        List<String> paramErrors = validationResult.getErrors('parameter')
        errors.addAll(paramErrors)

        //=====================================================================//
        // Check for nextflow core params and unexpected params
        //=====================================================================//
        List<String> unexpectedParams = []
        if (paramErrors.size() == 0) {
            validationResult.unevaluated.each { param ->
                String dotParam = param.replaceAll('/', '.')
                if (nextflowOptions.contains(param)) {
                    /* groovylint-disable-next-line LineLength */
                    errors << "You used a core Nextflow option with two hyphens: '--${param}'. Please resubmit with '-${param}'".toString()
                }
                else if (!config.ignoreParams.any { ignoreParam ->
                    dotParam == ignoreParam || dotParam.startsWith(ignoreParam + '.')
                }) {
                    // Check if an ignore param is present
                    /* groovylint-disable-next-line LineLength */
                    unexpectedParams << "* --${param.replaceAll('/', '.')}: ${getValueFromJsonPointer('/' + param, paramsJSON)}".toString()
                }
            }
        }

        if (unexpectedParams.size() > 0) {
            config.logging.unrecognisedParams.log(
                'The following invalid input values have been detected:\n\n' +
                unexpectedParams.join('\n').trim() + '\n\n'
            )
        }

        List<String> modifiedIgnoreParams = config.ignoreParams.collect { param -> "* --${param}" as String }
        List<String> filteredErrors = errors.findAll { error ->
            return modifiedIgnoreParams.find { param -> error.startsWith(param) } == null
        }
        if (filteredErrors.size() > 0) {
            /* groovylint-disable-next-line LineLength */
            String msg = "${colors.red}The following invalid input values have been detected:\n\n" + filteredErrors.join('\n').trim() + "\n${colors.reset}\n"
            log.error('Validation of pipeline parameters failed!')
            throw new SchemaValidationException(msg, this.getErrors())
        }

        log.debug 'Finishing parameters validation'
    }

    private List<String> getErrors() { return errors }

    private List<String> getWarnings() { return warnings }

    //
    // Clean and check parameters relative to Nextflow native classes
    //
    private Map cleanParameters(Map params) {
        Map newParams = (Map) params.getClass().newInstance(params)
        for (p in params) {
            // remove anything evaluating to false
            if (!p['value'] && p['value'] != 0) {
                newParams.remove(p.key)
            }
            // Cast MemoryUnit to String
            else if (p['value'] in MemoryUnit) {
                newParams.replace(p.key, p['value'].toString())
            }
            // Cast Duration to String
            else if (p['value'] in Duration) {
                newParams.replace(p.key, p['value'].toString())
            }
            // Cast LinkedHashMap to String
            else if (p['value'] in LinkedHashMap) {
                newParams.replace(p.key, p['value'].toString())
            }
            // Parsed nested parameters
            else if (p['value'] in Map) {
                newParams.replace(p.key, cleanParameters(p['value'] as Map))
            }
        }
        return newParams
    }

    //
    // Initialise expected params if not present
    //
    private Map initialiseExpectedParams(Map params) {
        expectedParams.each { param ->
            params[param] = false
        }
        return params
    }

    //
    // Add expected params
    //
    private List getExpectedParams() {
        List expectedParams = [
            config.help.shortParameter,
            config.help.fullParameter,
            config.help.showHiddenParameter
        ]

        return expectedParams
    }

}
