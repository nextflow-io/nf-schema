package nextflow.validation.config

import groovy.util.logging.Slf4j
import groovy.transform.CompileStatic

import nextflow.validation.logging.ValidationLogger

import nextflow.config.spec.ConfigOption
import nextflow.config.spec.ConfigScope
import nextflow.script.dsl.Description

/**
 * This class is used to define logging of the nf-schema plugin
 *
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */

@Slf4j
@CompileStatic
class LoggingConfig implements ConfigScope {

    final private static List<String> OPTIONS = ['skip', 'debug', 'info', 'warn', 'error']

    @ConfigOption
    @Description('Define the logging level of unrecognised parameters. Defaults to `warn`.')
    final ValidationLogger unrecognisedParams

    @ConfigOption
    @Description('''
Define the logging level of unrecognised headers that are found in the samplesheets. Defaults to `warn`.
''')
    final ValidationLogger unrecognisedHeaders

    LoggingConfig(Map map, Boolean monochromeLogs = false) {
        Map config = map ?: [:]
        ValidationLogger defaultLogger = new ValidationLogger('warn', monochromeLogs)
        unrecognisedParams = createLogger(
            'validation.logging.unrecognisedParams', config.get('unrecognisedParams'), defaultLogger, monochromeLogs
        )
        unrecognisedHeaders = createLogger(
            'validation.logging.unrecognisedHeaders', config.get('unrecognisedHeaders'), defaultLogger, monochromeLogs
        )
    }

    static ValidationLogger createLogger(String label, Object level, ValidationLogger fallback, Boolean monochromeLogs) {
        if (level == null) {
            return fallback
        }
        if (OPTIONS.contains(level.toString())) {
            log.debug("Set `${label}` to ${level}")
            return new ValidationLogger(level.toString(), monochromeLogs)
        }
        /* groovylint-disable-next-line LineLength */
        log.warn("Incorrect value detected for `${label}`, one of (${OPTIONS.join(', ')}) is expected. Defaulting to `${fallback.level}`")
        return fallback
    }

}
