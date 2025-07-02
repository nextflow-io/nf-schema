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
    final public ValidationLogger logUnrecognisedParams

    final private OPTIONS = ['skip', 'debug', 'info', 'warn', 'error']

    LoggingConfig(Map map){
        def config = map ?: [:]

        // logUnrecognisedParams
        def String level = config.get("logUnrecognisedParams", "warn")
        if(OPTIONS.contains(level)) {
            if(config.get("logUnrecognisedParams")) {
                log.debug("Set `validation.logUnrecognisedParams` to ${level}")
            }
            logUnrecognisedParams = new ValidationLogger(level)
        } else {
            log.warn("Incorrect value detected for `validation.logUnrecognisedParams`, one of (${OPTIONS.join(', ')}) is expected. Defaulting to `warn`")
            logUnrecognisedParams = new ValidationLogger("warn")
        }
    }
}