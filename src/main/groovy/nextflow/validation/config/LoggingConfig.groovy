package nextflow.validation.config

import groovy.util.logging.Slf4j
import groovy.transform.CompileDynamic

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
@CompileDynamic
class LoggingConfig implements ConfigScope {

    @ConfigOption
    @Description('Define the logging level of unrecognised parameters. Defaults to `warn`.')
    final ValidationLogger unrecognisedParams

    @ConfigOption
    @Description('''
Define the logging level of unrecognised headers that are found in the samplesheets. Defaults to `warn`.
''')
    final ValidationLogger unrecognisedHeaders

    final private List<String> options = ['skip', 'debug', 'info', 'warn', 'error']

    LoggingConfig(Map map, Boolean monochromeLogs = false) {
        Map config = map ?: [:]

        // unrecognisedParams
        String level = config.get('unrecognisedParams') in CharSequence ? config.get('unrecognisedParams') : 'warn'
        if (options.contains(level)) {
            if (config.get('unrecognisedParams')) {
                log.debug("Set `validation.unrecognisedParams` to ${level}")
            }
            unrecognisedParams = new ValidationLogger(level, monochromeLogs)
        } else {
            /* groovylint-disable-next-line LineLength */
            log.warn("Incorrect value detected for `validation.unrecognisedParams`, one of (${options.join(', ')}) is expected. Defaulting to `warn`")
            unrecognisedParams = new ValidationLogger('warn', monochromeLogs)
        }

        // unrecognisedHeaders
        level = config.get('unrecognisedHeaders') in CharSequence ? config.get('unrecognisedHeaders') : 'warn'
        if (options.contains(level)) {
            if (config.get('unrecognisedHeaders')) {
                log.debug("Set `validation.unrecognisedHeaders` to ${level}")
            }
            unrecognisedHeaders = new ValidationLogger(level, monochromeLogs)
        } else {
            /* groovylint-disable-next-line LineLength */
            log.warn("Incorrect value detected for `validation.unrecognisedHeaders`, one of (${options.join(', ')}) is expected. Defaulting to `warn`")
            unrecognisedHeaders = new ValidationLogger('warn', monochromeLogs)
        }
    }

}
