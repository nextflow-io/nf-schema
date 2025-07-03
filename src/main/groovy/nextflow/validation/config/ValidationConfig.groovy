package nextflow.validation.config

import groovy.util.logging.Slf4j

import nextflow.validation.exceptions.SchemaValidationException

import nextflow.Session
import nextflow.config.schema.ConfigOption
import nextflow.config.schema.ConfigScope
import nextflow.config.schema.ScopeName
import nextflow.script.dsl.Description

/**
 * This class is used to read, parse and validate the `validation` config block.
 *
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 *
 */

@Slf4j
@ScopeName('validation')
@Description('''
    The `validation` scope allows you to configure the `nf-schema` plugin.
''')
class ValidationConfig implements ConfigScope {

    @ConfigOption
    @Description('Toggle lenient mode. In lenient mode, the validation of types will be more lenient (e.g. an integer will pass as a string type).')
    final public Boolean lenientMode = false

    @ConfigOption
    @Description('Toggle monochrome logs. In monochrome mode, the logs will not be colored.')
    final public Boolean monochromeLogs = false

    @ConfigOption
    @Description('Fail if unrecognised parameters are found in the config file. A warning will be given by default.')
    final public Boolean failUnrecognisedParams = false

    @ConfigOption
    @Description('Fail if unrecognised headers are found in the samplesheets. A warning will be given by default.')
    final public Boolean failUnrecognisedHeaders = false

    @ConfigOption
    @Description('Show hidden parameters in the help message. This is deprecated, please use `validation.help.showHidden` or the `--showHidden` parameter instead.')
    final public Boolean showHiddenParams = false

    @ConfigOption
    @Description('Maximum size of the value shown in the error message. If the value is larger than this threshold, it will be truncated. Set to -1 to disable truncation.')
    final public Integer maxErrValSize = 150

    @ConfigOption
    @Description('The JSON schema file to use for parameter validation.')
    final public CharSequence  parametersSchema = "nextflow_schema.json"

    @ConfigOption
    @Description('A list of default parameters to ignore during validation. This option should only be used by pipeline developers.')
    final private Set<CharSequence> defaultIgnoreParams

    @ConfigOption
    @Description('A list of parameters to ignore during validation.')
    final public Set<CharSequence> ignoreParams = ["nf_test_output"] // Always ignore the `--nf_test_output` parameter to avoid warnings when running with nf-test

    @Description('Configuration scope for the help message.')
    final public HelpConfig help

    @Description('Configuration scope for the parameter summary.')
    final public SummaryConfig summary

    // Keep the no-arg constructor in order to be able to use the `@ConfigOption` annotation
    ValidationConfig(){}

    ValidationConfig(Map map, Session session){
        def config = map ?: Collections.emptyMap()
        def params = (Map)session.params ?: [:]

        // lenientMode
        if(config.containsKey("lenientMode")) {
            if(config.lenientMode instanceof Boolean) {
                lenientMode = config.lenientMode
                log.debug("Set `validation.lenientMode` to ${lenientMode}")
            } else {
                log.warn("Incorrect value detected for `validation.lenientMode`, a boolean is expected. Defaulting to `${lenientMode}`")
            }
        }

        // monochromeLogs
        def ansiLog = session?.ansiLog ?: false
        if(config.containsKey("monochromeLogs") || !ansiLog) {
            if(!ansiLog) {
                monochromeLogs = !ansiLog
                log.debug("Set `validation.monochromeLogs` to ${monochromeLogs} due to the ANSI log settings.")
            } else if(config.monochromeLogs instanceof Boolean) {
                monochromeLogs = config.monochromeLogs
                log.debug("Set `validation.monochromeLogs` to ${monochromeLogs}")
            } else {
                log.warn("Incorrect value detected for `validation.monochromeLogs`, a boolean is expected. Defaulting to `${monochromeLogs}`")
            }
        }

        // failUnrecognisedParams
        if(config.containsKey("failUnrecognisedParams")) {
            if(config.failUnrecognisedParams instanceof Boolean) {
                failUnrecognisedParams = config.failUnrecognisedParams
                log.debug("Set `validation.failUnrecognisedParams` to ${failUnrecognisedParams}")
            } else {
                log.warn("Incorrect value detected for `validation.failUnrecognisedParams`, a boolean is expected. Defaulting to `${failUnrecognisedParams}`")
            }
        }

        // failUnrecognisedHeaders
        if(config.containsKey("failUnrecognisedHeaders")) {
            if(config.failUnrecognisedHeaders instanceof Boolean) {
                failUnrecognisedHeaders = config.failUnrecognisedHeaders
                log.debug("Set `validation.failUnrecognisedHeaders` to ${failUnrecognisedHeaders}")
            } else {
                log.warn("Incorrect value detected for `validation.failUnrecognisedHeaders`, a boolean is expected. Defaulting to `${failUnrecognisedHeaders}`")
            }
        }

        // showHiddenParams
        if(config.containsKey("showHiddenParams")) {
            log.warn("configuration option `validation.showHiddenParams` is deprecated, please use `validation.help.showHidden` or the `--showHidden` parameter instead")
            if(config.showHiddenParams instanceof Boolean) {
                showHiddenParams = config.showHiddenParams
                log.debug("Set `validation.showHiddenParams` to ${showHiddenParams}")
            } else {
                log.warn("Incorrect value detected for `validation.showHiddenParams`, a boolean is expected. Defaulting to `${showHiddenParams}`")
            }
        }

        // maxErrValSize
        if(config.containsKey("maxErrValSize")) {
            if(config.maxErrValSize instanceof Integer && (config.maxErrValSize >= 1 || config.maxErrValSize == -1)) {
                maxErrValSize = config.maxErrValSize
                log.debug("Set `validation.maxErrValSize` to ${maxErrValSize}")
            } else {
                log.warn("`validation.maxErrValSize` needs to be a value above 0 or equal to -1. Defaulting to ${maxErrValSize}")
            }
        }

        // parameterSchema
        if(config.containsKey("parametersSchema")) {
            if(config.parametersSchema instanceof CharSequence) {
                parametersSchema = config.parametersSchema
                log.debug("Set `validation.parametersSchema` to ${parametersSchema}")
            } else {
                log.warn("Incorrect value detected for `validation.parametersSchema`, a string is expected. Defaulting to `${parametersSchema}`")
            }
        }

        // ignoreParams
        if(config.containsKey("ignoreParams")) {
            if(config.ignoreParams instanceof List<CharSequence>) {
                ignoreParams += config.ignoreParams
                log.debug("Added the following parameters to the ignored parameters: ${config.ignoreParams}")
            } else {
                log.warn("Incorrect value detected for `validation.ignoreParams`, a list with string values is expected. Defaulting to `${ignoreParams}`")
            }
        }

        // defaultIgnoreParams
        if(config.containsKey("defaultIgnoreParams")) {
            if(config.defaultIgnoreParams instanceof List<CharSequence>) {
                ignoreParams += config.defaultIgnoreParams
                log.debug("Added the following parameters to the ignored parameters: ${config.defaultIgnoreParams}")
            } else {
                log.warn("Incorrect value detected for `validation.defaultIgnoreParams`, a list with string values is expected. Defaulting to `${ignoreParams}`")
            }
        }

        // help
        def Map helpConfig = [:]
        if(config.containsKey("help")) {
            if(config.help instanceof Map) {
                helpConfig = config.help
            } else {
                log.warn("Incorrect value detected for `validation.help`, a map with key-value pairs is expected. Setting the defaults for all help options.")
            }
        }
        help = new HelpConfig(helpConfig, params, monochromeLogs, showHiddenParams)

        // summary
        def Map summaryConfig = [:]
        if(config.containsKey("summary")) {
            if(config.summary instanceof Map) {
                summaryConfig = config.summary
            } else {
                log.warn("Incorrect value detected for `validation.summary`, a map with key-value pairs is expected. Setting the defaults for all summary options.")
            }
        }
        summary = new SummaryConfig(summaryConfig, monochromeLogs)
    }
}