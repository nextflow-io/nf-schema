package nextflow.validation.config

import groovy.util.logging.Slf4j

import nextflow.validation.exceptions.SchemaValidationException
import nextflow.validation.logging.ValidationLogger

import nextflow.config.schema.ConfigOption
import nextflow.config.schema.ConfigScope
import nextflow.script.dsl.Description

/**
 * This class is used to define logging of the nf-schema plugin
 *
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 *
 */

@Slf4j
class LoggingConfig implements ConfigScope {

    @ConfigOption
    @Description('Define the logging level of unrecognised parameters. Defaults to `warn`.')
    final public ValidationLogger unrecognisedParams

    @ConfigOption
    @Description('Define the logging level of unrecognised headers that are found in the samplesheets. Defaults to `warn`.')
    final public ValidationLogger unrecognisedHeaders

    final private OPTIONS = ['skip', 'debug', 'info', 'warn', 'error']

    LoggingConfig(Map map, Boolean monochromeLogs = false) {
        def config = map ?: [:]

        // unrecognisedParams
        def String level = config.get("unrecognisedParams") instanceof CharSequence ? config.get("unrecognisedParams") : "warn"
        if(OPTIONS.contains(level)) {
            if(config.get("unrecognisedParams")) {
                log.debug("Set `validation.unrecognisedParams` to ${level}")
            }
            unrecognisedParams = new ValidationLogger(level, monochromeLogs)
        } else {
            log.warn("Incorrect value detected for `validation.unrecognisedParams`, one of (${OPTIONS.join(', ')}) is expected. Defaulting to `warn`")
            unrecognisedParams = new ValidationLogger("warn", monochromeLogs)
        }

        // unrecognisedHeaders
        level = config.get("unrecognisedHeaders") instanceof CharSequence ? config.get("unrecognisedHeaders") : "warn"
        if(OPTIONS.contains(level)) {
            if(config.get("unrecognisedHeaders")) {
                log.debug("Set `validation.unrecognisedHeaders` to ${level}")
            }
            unrecognisedHeaders = new ValidationLogger(level, monochromeLogs)
        } else {
            log.warn("Incorrect value detected for `validation.unrecognisedHeaders`, one of (${OPTIONS.join(', ')}) is expected. Defaulting to `warn`")
            unrecognisedHeaders = new ValidationLogger("warn", monochromeLogs)
        }
    }
}