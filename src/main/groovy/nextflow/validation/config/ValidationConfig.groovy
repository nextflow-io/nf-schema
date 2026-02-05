package nextflow.validation.config

import groovy.util.logging.Slf4j
import groovy.transform.CompileDynamic

import nextflow.Session
import nextflow.config.spec.ConfigOption
import nextflow.config.spec.ConfigScope
import nextflow.config.spec.ScopeName
import nextflow.script.dsl.Description

/**
 * This class is used to read, parse and validate the `validation` config block.
 *
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */

@Slf4j
@CompileDynamic
@ScopeName('validation')
@Description('''
    The `validation` scope allows you to configure the `nf-schema` plugin.
''')
class ValidationConfig implements ConfigScope {

    @ConfigOption
    @Description('''
Toggle lenient mode. In lenient mode, the validation of types will be more lenient
(e.g. an integer will pass as a string type).
''')
    final Boolean lenientMode = false

    @ConfigOption
    @Description('Toggle monochrome logs. In monochrome mode, the logs will not be colored.')
    final Boolean monochromeLogs = false

    @ConfigOption
    @Description('''
Show hidden parameters in the help message.
This is deprecated, please use `validation.help.showHidden` or the `--showHidden` parameter instead.
''')
    final Boolean showHiddenParams = false

    @ConfigOption
    @Description('''
Maximum size of the value shown in the error message.
If the value is larger than this threshold, it will be truncated. Set to -1 to disable truncation.
''')
    final Integer maxErrValSize = 150

    @ConfigOption
    @Description('The JSON schema file to use for parameter validation.')
    final CharSequence  parametersSchema = 'nextflow_schema.json'

    @ConfigOption
    @Description('''
A list of default parameters to ignore during validation. This option should only be used by pipeline developers.
''')
    final Set<CharSequence> defaultIgnoreParams

    @ConfigOption
    @Description('A list of parameters to ignore during validation.')
    // Always ignore the `--nf_test_output` parameter to avoid warnings when running with nf-test
    final Set<CharSequence> ignoreParams = ['nf_test_output']

    @Description('Configuration scope for the help message.')
    final HelpConfig help

    @Description('Configuration scope for the parameter summary.')
    final SummaryConfig summary

    @Description('Configuration scope for the logging of the validation plugin.')
    final LoggingConfig logging

    // Keep the no-arg constructor in order to be able to use the `@ConfigOption` annotation
    ValidationConfig() {
    }

    /* groovylint-disable-next-line MethodSize */
    ValidationConfig(Map map, Session session) {
        Map config = map ?: Collections.emptyMap()
        Map params = (Map)session.params ?: [:]

        // lenientMode
        if (config.containsKey('lenientMode')) {
            if (config.lenientMode in Boolean) {
                lenientMode = config.lenientMode
                log.debug("Set `validation.lenientMode` to ${lenientMode}")
            } else {
                /* groovylint-disable-next-line LineLength */
                log.warn("Incorrect value detected for `validation.lenientMode`, a boolean is expected. Defaulting to `${lenientMode}`")
            }
        }

        // monochromeLogs
        Boolean ansiLog = session?.ansiLog ?: false
        if (config.containsKey('monochromeLogs') || !ansiLog) {
            if (!ansiLog) {
                monochromeLogs = !ansiLog
                log.debug("Set `validation.monochromeLogs` to ${monochromeLogs} due to the ANSI log settings.")
            } else if (config.monochromeLogs in Boolean) {
                monochromeLogs = config.monochromeLogs
                log.debug("Set `validation.monochromeLogs` to ${monochromeLogs}")
            } else {
                /* groovylint-disable-next-line LineLength */
                log.warn("Incorrect value detected for `validation.monochromeLogs`, a boolean is expected. Defaulting to `${monochromeLogs}`")
            }
        }

        // failUnrecognisedParams
        if (config.containsKey('failUnrecognisedParams')) {
            if (config.failUnrecognisedParams in Boolean) {
                failUnrecognisedParams = config.failUnrecognisedParams
                log.debug("Set `validation.failUnrecognisedParams` to ${failUnrecognisedParams}")
            } else {
                /* groovylint-disable-next-line LineLength */
                log.warn("Incorrect value detected for `validation.failUnrecognisedParams`, a boolean is expected. Defaulting to `${failUnrecognisedParams}`")
            }
        }

        // failUnrecognisedHeaders
        if (config.containsKey('failUnrecognisedHeaders')) {
            if (config.failUnrecognisedHeaders in Boolean) {
                failUnrecognisedHeaders = config.failUnrecognisedHeaders
                log.debug("Set `validation.failUnrecognisedHeaders` to ${failUnrecognisedHeaders}")
            } else {
                /* groovylint-disable-next-line LineLength */
                log.warn("Incorrect value detected for `validation.failUnrecognisedHeaders`, a boolean is expected. Defaulting to `${failUnrecognisedHeaders}`")
            }
        }

        // showHiddenParams
        if (config.containsKey('showHiddenParams')) {
            /* groovylint-disable-next-line LineLength */
            log.warn('configuration option `validation.showHiddenParams` is deprecated, please use `validation.help.showHidden` or the `--showHidden` parameter instead')
            if (config.showHiddenParams in Boolean) {
                showHiddenParams = config.showHiddenParams
                log.debug("Set `validation.showHiddenParams` to ${showHiddenParams}")
            } else {
                /* groovylint-disable-next-line LineLength */
                log.warn("Incorrect value detected for `validation.showHiddenParams`, a boolean is expected. Defaulting to `${showHiddenParams}`")
            }
        }

        // maxErrValSize
        if (config.containsKey('maxErrValSize')) {
            if (config.maxErrValSize in Integer && (config.maxErrValSize >= 1 || config.maxErrValSize == -1)) {
                maxErrValSize = config.maxErrValSize
                log.debug("Set `validation.maxErrValSize` to ${maxErrValSize}")
            } else {
                /* groovylint-disable-next-line LineLength */
                log.warn("`validation.maxErrValSize` needs to be a value above 0 or equal to -1. Defaulting to ${maxErrValSize}")
            }
        }

        // parameterSchema
        if (config.containsKey('parametersSchema')) {
            if (config.parametersSchema in CharSequence) {
                parametersSchema = config.parametersSchema
                log.debug("Set `validation.parametersSchema` to ${parametersSchema}")
            } else {
                /* groovylint-disable-next-line LineLength */
                log.warn("Incorrect value detected for `validation.parametersSchema`, a string is expected. Defaulting to `${parametersSchema}`")
            }
        }

        // ignoreParams
        if (config.containsKey('ignoreParams')) {
            if (config.ignoreParams in List<CharSequence>) {
                ignoreParams += config.ignoreParams
                log.debug("Added the following parameters to the ignored parameters: ${config.ignoreParams}")
            } else {
                /* groovylint-disable-next-line LineLength */
                log.warn("Incorrect value detected for `validation.ignoreParams`, a list with string values is expected. Defaulting to `${ignoreParams}`")
            }
        }

        // defaultIgnoreParams
        if (config.containsKey('defaultIgnoreParams')) {
            if (config.defaultIgnoreParams in List<CharSequence>) {
                ignoreParams += config.defaultIgnoreParams
                log.debug("Added the following parameters to the ignored parameters: ${config.defaultIgnoreParams}")
            } else {
                /* groovylint-disable-next-line LineLength */
                log.warn("Incorrect value detected for `validation.defaultIgnoreParams`, a list with string values is expected. Defaulting to `${ignoreParams}`")
            }
        }

        // help
        Map helpConfig = [:]
        if (config.containsKey('help')) {
            if (config.help in Map) {
                helpConfig = config.help
            } else {
                /* groovylint-disable-next-line LineLength */
                log.warn('Incorrect value detected for `validation.help`, a map with key-value pairs is expected. Setting the defaults for all help options.')
            }
        }
        help = new HelpConfig(helpConfig, params, monochromeLogs)

        // summary
        Map summaryConfig = [:]
        if (config.containsKey('summary')) {
            if (config.summary in Map) {
                summaryConfig = config.summary
            } else {
                /* groovylint-disable-next-line LineLength */
                log.warn('Incorrect value detected for `validation.summary`, a map with key-value pairs is expected. Setting the defaults for all summary options.')
            }
        }
        summary = new SummaryConfig(summaryConfig, monochromeLogs)

        // logging
        Map loggingConfig = [:]
        if (config.containsKey('logging')) {
            if (config.logging in Map) {
                loggingConfig = config.logging
            } else {
                /* groovylint-disable-next-line LineLength */
                log.warn('Incorrect value detected for `validation.logging`, a map with key-value pairs is expected. Setting the defaults for all logging options.')
            }
        }
        logging = new LoggingConfig(loggingConfig, monochromeLogs)
    }

}
