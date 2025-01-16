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
class HelpConfig {
    final public Boolean enabled
    final public String  shortParameter
    final public String  fullParameter
    final public String  showHiddenParameter
    final public String  beforeText
    final public String  afterText
    final public String  command
    final public Boolean showHidden

    HelpConfig(Map map, Map params, Boolean monochromeLogs, Boolean showHiddenParams) {
        def config = map ?: Collections.emptyMap()
        enabled             = config.enabled                    ?: false
        shortParameter      = config.shortParameter             ?: "help"
        fullParameter       = config.fullParameter              ?: "helpFull"
        showHiddenParameter = config.showHiddenParameter        ?: "showHidden"
        if (monochromeLogs) {
            beforeText  = config.beforeText ? removeColors(config.beforeText): ""
            afterText   = config.afterText  ? removeColors(config.afterText) : ""
            command     = config.command    ? removeColors(config.command)   : ""
        } else {
            beforeText  = config.beforeText ?: ""
            afterText   = config.afterText  ?: ""
            command     = config.command    ?: ""
        }
        showHidden          = params.containsKey(showHiddenParameter) ? params.get(showHiddenParameter) : config.showHidden    ?: showHiddenParams ?: false
    }
}