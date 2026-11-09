package nextflow.validation.config

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

@CompileStatic
class LoggingConfig implements ConfigScope {

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
        unrecognisedParams = ValidationLogger.of(
            'validation.logging.unrecognisedParams', config.get('unrecognisedParams'), defaultLogger, monochromeLogs
        )
        unrecognisedHeaders = ValidationLogger.of(
            'validation.logging.unrecognisedHeaders', config.get('unrecognisedHeaders'), defaultLogger, monochromeLogs
        )
    }

}
