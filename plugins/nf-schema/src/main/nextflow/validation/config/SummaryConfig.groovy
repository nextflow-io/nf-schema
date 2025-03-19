package nextflow.validation.config

import groovy.util.logging.Slf4j

import static nextflow.validation.utils.Colors.removeColors

/**
 * This class allows to model a specific configuration, extracting values from a map and converting
 *
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 *
 */

@Slf4j
class SummaryConfig {
    final public String beforeText = ""
    final public String afterText = ""

    final public Set<String> hideParams = []

    SummaryConfig(Map map, Boolean monochromeLogs) {
        def config = map ?: Collections.emptyMap()

        // beforeText
        if(config.containsKey("beforeText")) {
            if(config.beforeText instanceof String) {
                if(monochromeLogs) {
                    beforeText = config.beforeText
                } else {
                    beforeText = removeColors(config.beforeText)
                }
                log.debug("Set `validation.summary.beforeText` to ${beforeText}")
            } else {
                log.warn("Incorrect value detected for `validation.summary.beforeText`, a string is expected. Defaulting to `${beforeText}`")
            }
        }

        // afterText
        if(config.containsKey("afterText")) {
            if(config.afterText instanceof String) {
                if(monochromeLogs) {
                    afterText = config.afterText
                } else {
                    afterText = removeColors(config.afterText)
                }
                log.debug("Set `validation.summary.afterText` to ${afterText}")
            } else {
                log.warn("Incorrect value detected for `validation.summary.afterText`, a string is expected. Defaulting to `${afterText}`")
            }
        }

        // hideParams
        if(config.containsKey("hideParams")) {
            if(config.hideParams instanceof List<String>) {
                hideParams = config.hideParams
                log.debug("Set `validation.summary.hideParams` to ${hideParams}")
            } else {
                log.warn("Incorrect value detected for `validation.summary.hideParams`, a list of strings is expected. Defaulting to `${hideParams}`")
            }
        }
    }
}