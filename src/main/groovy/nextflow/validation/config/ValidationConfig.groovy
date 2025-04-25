package nextflow.validation.config

import groovy.util.logging.Slf4j

import nextflow.validation.exceptions.SchemaValidationException

/**
 * This class allows model an specific configuration, extracting values from a map and converting
 *
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 *
 */

@Slf4j
class ValidationConfig {

    final public Boolean lenientMode = false
    final public Boolean monochromeLogs = false
    final public Boolean failUnrecognisedParams = false
    final public Boolean failUnrecognisedHeaders = false
    final public Boolean showHiddenParams = false

    final public Integer maxErrValSize = 150

    final public CharSequence  parametersSchema = "nextflow_schema.json"

    final public Set<CharSequence> ignoreParams = ["nf_test_output"] // Always ignore the `--nf_test_output` parameter to avoid warnings when running with nf-test

    final public HelpConfig help

    final public SummaryConfig summary

    ValidationConfig(Map map, Map params){
        def config = map ?: Collections.emptyMap()

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
        if(config.containsKey("monochromeLogs")) {
            if(config.monochromeLogs instanceof Boolean) {
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