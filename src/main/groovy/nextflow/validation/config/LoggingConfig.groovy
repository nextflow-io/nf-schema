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

        // unrecognisedParams
        String unrecognisedParamsOption = config.get('unrecognisedParams')
        String level = unrecognisedParamsOption in CharSequence ? unrecognisedParamsOption : 'warn'
        if (OPTIONS.contains(level)) {
            if (unrecognisedParamsOption) {
                log.debug("Set `validation.unrecognisedParams` to ${level}")
            }
            unrecognisedParams = new ValidationLogger(level, monochromeLogs)
        } else {
            /* groovylint-disable-next-line LineLength */
            log.warn("Incorrect value detected for `validation.unrecognisedParams`, one of (${OPTIONS.join(', ')}) is expected. Defaulting to `warn`")
            unrecognisedParams = new ValidationLogger('warn', monochromeLogs)
        }

        // unrecognisedHeaders
        String unrecognisedHeadersOption = config.get('unrecognisedHeaders')
        level = unrecognisedHeadersOption in CharSequence ? unrecognisedHeadersOption : 'warn'
        if (OPTIONS.contains(level)) {
            if (unrecognisedHeadersOption) {
                log.debug("Set `validation.unrecognisedHeaders` to ${level}")
            }
            unrecognisedHeaders = new ValidationLogger(level, monochromeLogs)
        } else {
            /* groovylint-disable-next-line LineLength */
            log.warn("Incorrect value detected for `validation.unrecognisedHeaders`, one of (${OPTIONS.join(', ')}) is expected. Defaulting to `warn`")
            unrecognisedHeaders = new ValidationLogger('warn', monochromeLogs)
        }
    }

}
