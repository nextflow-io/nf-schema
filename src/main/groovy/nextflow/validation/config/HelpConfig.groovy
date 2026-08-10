package nextflow.validation.config

import static nextflow.validation.utils.Colors.removeColors

import nextflow.config.spec.ConfigOption
import nextflow.config.spec.ConfigScope
import nextflow.script.dsl.Description

import groovy.util.logging.Slf4j
import groovy.transform.CompileDynamic

/**
 * This class is used to read, parse and validate the `validation.help` config block.
 *
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */

@Slf4j
@CompileDynamic
class HelpConfig implements ConfigScope {

    @ConfigOption
    @Description('Enable the help message.')
    final Boolean enabled = false

    @ConfigOption
    @Description('Show hidden parameters in the help message.')
    final Boolean showHidden = false

    @ConfigOption
    @Description('The parameter to use to show hidden parameters in the help message.')
    final CharSequence showHiddenParameter = 'showHidden'

    @ConfigOption
    @Description('The parameter to use to show the short help message.')
    final CharSequence shortParameter = 'help'

    @ConfigOption
    @Description('The parameter to use to show the full help message.')
    final CharSequence fullParameter = 'helpFull'

    @ConfigOption
    @Description('The text to show before the help message.')
    final CharSequence beforeText = ''

    @ConfigOption
    @Description('The text to show after the help message.')
    final CharSequence afterText = ''

    @ConfigOption
    @Description('An example command of how to run the pipeline.')
    final CharSequence command = ''

    @ConfigOption
    @Description('''
    The maximum length in characters of the enum preview in the help message.
    This defaults to the length of the terminal window specified by the `COLUMNS` environment variable
    or 100 characters if this variable hasn't been set.
    If the enum preview exceeds this length, it will be truncated.
    Set this option to -1 to disable the enum preview truncation.
    ''')
    final Integer enumLength = System.getenv('COLUMNS')?.toInteger() ?: 100

    /* groovylint-disable-next-line MethodSize */
    HelpConfig(Map map, Map params, Boolean monochromeLogs) {
        Map config = map ?: Collections.emptyMap()

        // enabled
        if (config.containsKey('enabled')) {
            if (config.enabled in Boolean) {
                enabled = config.enabled
                log.debug("Set `validation.help.enabled` to ${enabled}")
            } else {
                /* groovylint-disable-next-line LineLength */
                log.warn("Incorrect value detected for `validation.help.enabled`, a boolean is expected. Defaulting to `${enabled}`")
            }
        }

        // showHiddenParameter
        if (config.containsKey('showHiddenParameter')) {
            if (config.showHiddenParameter in CharSequence) {
                showHiddenParameter = config.showHiddenParameter
                log.debug("Set `validation.help.showHiddenParameter` to ${showHiddenParameter}")
            } else {
                /* groovylint-disable-next-line LineLength */
                log.warn("Incorrect value detected for `validation.help.showHiddenParameter`, a string is expected. Defaulting to `${showHiddenParameter}`")
            }
        }

        // showHidden
        if (params.containsKey(showHiddenParameter) || config.containsKey('showHidden')) {
            if (params.containsKey(showHiddenParameter) && params.get(showHiddenParameter) in Boolean) {
                showHidden = params.get(showHiddenParameter)
                log.debug("Set `validation.help.showHidden` to ${showHidden} (Due to --${showHiddenParameter})")
            } else if (config.showHidden in Boolean) {
                showHidden = config.showHidden
                log.debug("Set `validation.help.showHidden` to ${showHidden}")
            } else {
                /* groovylint-disable-next-line LineLength */
                log.warn("Incorrect value detected for `validation.help.showHidden` or `--${showHiddenParameter}`, a boolean is expected. Defaulting to `${showHidden}`")
            }
        }

        // shortParameter
        if (config.containsKey('shortParameter')) {
            if (config.shortParameter in CharSequence) {
                shortParameter = config.shortParameter
                log.debug("Set `validation.help.shortParameter` to ${shortParameter}")
            } else {
                /* groovylint-disable-next-line LineLength */
                log.warn("Incorrect value detected for `validation.help.shortParameter`, a string is expected. Defaulting to `${shortParameter}`")
            }
        }

        // fullParameter
        if (config.containsKey('fullParameter')) {
            if (config.fullParameter in CharSequence) {
                fullParameter = config.fullParameter
                log.debug("Set `validation.help.fullParameter` to ${fullParameter}")
            } else {
                /* groovylint-disable-next-line LineLength */
                log.warn("Incorrect value detected for `validation.help.fullParameter`, a string is expected. Defaulting to `${fullParameter}`")
            }
        }

        // beforeText
        if (config.containsKey('beforeText')) {
            if (config.beforeText in CharSequence) {
                if (monochromeLogs) {
                    beforeText = removeColors(config.beforeText)
                } else {
                    beforeText = config.beforeText
                }
                log.debug("Set `validation.help.beforeText` to ${beforeText}")
            } else {
                /* groovylint-disable-next-line LineLength */
                log.warn("Incorrect value detected for `validation.help.beforeText`, a string is expected. Defaulting to `${beforeText}`")
            }
        }

        // afterText
        if (config.containsKey('afterText')) {
            if (config.afterText in CharSequence) {
                if (monochromeLogs) {
                    afterText = removeColors(config.afterText)
                } else {
                    afterText = config.afterText
                }
                log.debug("Set `validation.help.afterText` to ${afterText}")
            } else {
                /* groovylint-disable-next-line LineLength */
                log.warn("Incorrect value detected for `validation.help.afterText`, a string is expected. Defaulting to `${afterText}`")
            }
        }

        // command
        if (config.containsKey('command')) {
            if (config.command in CharSequence) {
                if (monochromeLogs) {
                    command = config.command
                } else {
                    command = removeColors(config.command)
                }
                log.debug("Set `validation.help.command` to ${command}")
            } else {
                /* groovylint-disable-next-line LineLength */
                log.warn("Incorrect value detected for `validation.help.command`, a string is expected. Defaulting to `${command}`")
            }
        }

        // enumLength
        if (config.containsKey('enumLength')) {
            if (config.enumLength in Integer) {
                enumLength = config.enumLength
                log.debug("Set `validation.help.enumLength` to ${enumLength}")
            } else {
                /* groovylint-disable-next-line LineLength */
                log.warn("Incorrect value detected for `validation.help.enumLength`, an integer is expected. Defaulting to `${enumLength}`")
            }
        }
    }

}
