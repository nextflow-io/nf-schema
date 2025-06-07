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
    final public Boolean enabled = false
    final public Boolean showHidden = false

    final public CharSequence showHiddenParameter = "showHidden"
    final public CharSequence shortParameter = "help"
    final public CharSequence fullParameter = "helpFull"
    final public CharSequence showAssetsParameter = "showAssets"
    final public CharSequence beforeText = ""
    final public CharSequence afterText = ""
    final public CharSequence command = ""

    HelpConfig(Map map, Map params, Boolean monochromeLogs, Boolean showHiddenParams) {
        def config = map ?: Collections.emptyMap()

        // enabled
        if(config.containsKey("enabled")) {
            if(config.enabled instanceof Boolean) {
                enabled = config.enabled
                log.debug("Set `validation.help.enabled` to ${enabled}")
            } else {
                log.warn("Incorrect value detected for `validation.help.enabled`, a boolean is expected. Defaulting to `${enabled}`")
            }
        }

        // showHiddenParameter
        if(config.containsKey("showHiddenParameter")) {
            if(config.showHiddenParameter instanceof CharSequence) {
                showHiddenParameter = config.showHiddenParameter
                log.debug("Set `validation.help.showHiddenParameter` to ${showHiddenParameter}")
            } else {
                log.warn("Incorrect value detected for `validation.help.showHiddenParameter`, a string is expected. Defaulting to `${showHiddenParameter}`")
            }
        }

        // showHidden
        if(params.containsKey(showHiddenParameter) || config.containsKey("showHidden")) {
            if(params.get(showHiddenParameter) instanceof Boolean) {
                showHidden = params.get(showHiddenParameter)
                log.debug("Set `validation.help.showHidden` to ${showHidden} (Due to --${showHiddenParameter})")
            } else if(config.showHidden instanceof Boolean) {
                showHidden = config.showHidden
                log.debug("Set `validation.help.showHidden` to ${showHidden}")
            } else {
                log.warn("Incorrect value detected for `validation.help.showHidden` or `--${showHiddenParameter}`, a boolean is expected. Defaulting to `${showHidden}`")
            }
        }

        // shortParameter
        if(config.containsKey("shortParameter")) {
            if(config.shortParameter instanceof CharSequence) {
                shortParameter = config.shortParameter
                log.debug("Set `validation.help.shortParameter` to ${shortParameter}")
            } else {
                log.warn("Incorrect value detected for `validation.help.shortParameter`, a string is expected. Defaulting to `${shortParameter}`")
            }
        }

        // fullParameter
        if(config.containsKey("fullParameter")) {
            if(config.fullParameter instanceof CharSequence) {
                fullParameter = config.fullParameter
                log.debug("Set `validation.help.fullParameter` to ${fullParameter}")
            } else {
                log.warn("Incorrect value detected for `validation.help.fullParameter`, a string is expected. Defaulting to `${fullParameter}`")
            }
        }

        // sampleSheetParameter
        if(config.containsKey("showAssetsParameter")) {
            if(config.showAssetsParameter instanceof CharSequence) {
                showAssetsParameter = config.showAssetsParameter
                log.debug("Set `validation.help.showAssetsParameter` to ${showAssetsParameter}")
            } else {
                log.warn("Incorrect value detected for `validation.help.showAssetsParameter`, a string is expected. Defaulting to `${showAssetsParameter}`")
            }
        }

        // beforeText
        if(config.containsKey("beforeText")) {
            if(config.beforeText instanceof CharSequence) {
                if(monochromeLogs) {
                    beforeText = config.beforeText
                } else {
                    beforeText = removeColors(config.beforeText)
                }
                log.debug("Set `validation.help.beforeText` to ${beforeText}")
            } else {
                log.warn("Incorrect value detected for `validation.help.beforeText`, a string is expected. Defaulting to `${beforeText}`")
            }
        }

        // afterText
        if(config.containsKey("afterText")) {
            if(config.afterText instanceof CharSequence) {
                if(monochromeLogs) {
                    afterText = config.afterText
                } else {
                    afterText = removeColors(config.afterText)
                }
                log.debug("Set `validation.help.afterText` to ${afterText}")
            } else {
                log.warn("Incorrect value detected for `validation.help.afterText`, a string is expected. Defaulting to `${afterText}`")
            }
        }

        // command
        if(config.containsKey("command")) {
            if(config.command instanceof CharSequence) {
                if(monochromeLogs) {
                    command = config.command
                } else {
                    command = removeColors(config.command)
                }
                log.debug("Set `validation.help.command` to ${command}")
            } else {
                log.warn("Incorrect value detected for `validation.help.command`, a string is expected. Defaulting to `${command}`")
            }
        }
    }
}
