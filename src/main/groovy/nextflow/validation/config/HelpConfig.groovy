package nextflow.validation.config

import groovy.util.logging.Slf4j

import static nextflow.validation.utils.Colors.removeColors

import nextflow.config.schema.ConfigOption
import nextflow.config.schema.ConfigScope
import nextflow.config.schema.ScopeName
import nextflow.script.dsl.Description

/**
 * This class is used to read, parse and validate the `validation.help` config block.
 *
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 *
 */

@Slf4j
class HelpConfig implements ConfigScope {

    @ConfigOption
    @Description('Enable the help message.')
    final public Boolean enabled = false

    @ConfigOption
    @Description('Show hidden parameters in the help message.')
    final public Boolean showHidden = false

    @ConfigOption
    @Description('The parameter to use to show hidden parameters in the help message.')
    final public String showHiddenParameter = "showHidden"

    @ConfigOption
    @Description('The parameter to use to show the short help message.')
    final public String shortParameter = "help"

    @ConfigOption
    @Description('The parameter to use to show the full help message.')
    final public String fullParameter = "helpFull"

    @ConfigOption
    @Description('The text to show before the help message.')
    final public String beforeText = ""

    @ConfigOption
    @Description('The text to show after the help message.')
    final public String afterText = ""

    @ConfigOption
    @Description('An example command of how to run the pipeline.')
    final public String command = ""

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
            if(config.showHiddenParameter instanceof String) {
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
            if(config.shortParameter instanceof String) {
                shortParameter = config.shortParameter
                log.debug("Set `validation.help.shortParameter` to ${shortParameter}")
            } else {
                log.warn("Incorrect value detected for `validation.help.shortParameter`, a string is expected. Defaulting to `${shortParameter}`")
            }
        }

        // fullParameter
        if(config.containsKey("fullParameter")) {
            if(config.fullParameter instanceof String) {
                fullParameter = config.fullParameter
                log.debug("Set `validation.help.fullParameter` to ${fullParameter}")
            } else {
                log.warn("Incorrect value detected for `validation.help.fullParameter`, a string is expected. Defaulting to `${fullParameter}`")
            }
        }

        // beforeText
        if(config.containsKey("beforeText")) {
            if(config.beforeText instanceof String) {
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
            if(config.afterText instanceof String) {
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
            if(config.command instanceof String) {
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