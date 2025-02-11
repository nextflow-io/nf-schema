package nextflow.validation

import groovy.util.logging.Slf4j
import groovy.transform.PackageScope


/**
 * This class allows to model a specific configuration, extracting values from a map and converting
 *
 * We anotate this class as @PackageScope to restrict the access of their methods only to class in the
 * same package
 *
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 *
 */

@Slf4j
@PackageScope
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
            beforeText  = config.beforeText ? Utils.removeColors(config.beforeText): ""
            afterText   = config.afterText  ? Utils.removeColors(config.afterText) : ""
            command     = config.command    ? Utils.removeColors(config.command)   : ""
        } else {
            beforeText  = config.beforeText ?: ""
            afterText   = config.afterText  ?: ""
            command     = config.command    ?: ""
        }
        showHidden          = params.containsKey(showHiddenParameter) ? params.get(showHiddenParameter) : config.showHidden    ?: showHiddenParams ?: false
    }
}