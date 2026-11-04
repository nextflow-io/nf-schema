package nextflow.validation.config

import static nextflow.validation.utils.Colors.removeColors

import groovy.util.logging.Slf4j
import groovy.transform.CompileStatic

import nextflow.config.spec.ConfigOption
import nextflow.config.spec.ConfigScope
import nextflow.script.dsl.Description

/**
 * This class allows to model a specific configuration, extracting values from a map and converting
 *
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */

@Slf4j
@CompileStatic
class SummaryConfig implements ConfigScope {

    @ConfigOption
    @Description('The text to show before the summary message.')
    final CharSequence beforeText = ''

    @ConfigOption
    @Description('The text to show after the summary message.')
    final CharSequence afterText = ''

    @ConfigOption
    @Description('A list of parameters to hide in the summary message.')
    final Set<CharSequence> hideParams = []

    @ConfigOption
    @Description('Mask value, when replacing bucket names or subpaths. Defaults to [** masked **].')
    CharSequence mask = '[** masked **]'

    @ConfigOption
    @Description('A list of subpaths to mask from path values.')
    final List<CharSequence> maskSubpaths

    SummaryConfig(Map map, Boolean monochromeLogs) {
        Map config = map ?: Collections.emptyMap()

        // beforeText
        if (config.containsKey('beforeText')) {
            if (config.beforeText in CharSequence) {
                if (monochromeLogs) {
                    beforeText = config.beforeText as String
                } else {
                    beforeText = removeColors(config.beforeText as String)
                }
                log.debug("Set `validation.summary.beforeText` to ${beforeText}")
            } else {
                /* groovylint-disable-next-line LineLength */
                log.warn("Incorrect value detected for `validation.summary.beforeText`, a string is expected. Defaulting to `${beforeText}`")
            }
        }

        // afterText
        if (config.containsKey('afterText')) {
            if (config.afterText in CharSequence) {
                if (monochromeLogs) {
                    afterText = config.afterText as String
                } else {
                    afterText = removeColors(config.afterText as String)
                }
                log.debug("Set `validation.summary.afterText` to ${afterText}")
            } else {
                /* groovylint-disable-next-line LineLength */
                log.warn("Incorrect value detected for `validation.summary.afterText`, a string is expected. Defaulting to `${afterText}`")
            }
        }

        // hideParams
        if (config.containsKey('hideParams')) {
            if (config.hideParams in List<CharSequence>) {
                hideParams = config.hideParams as Set<CharSequence>
                log.debug("Set `validation.summary.hideParams` to ${hideParams}")
            } else {
                /* groovylint-disable-next-line LineLength */
                log.warn("Incorrect value detected for `validation.summary.hideParams`, a list of strings is expected. Defaulting to `${hideParams}`")
            }
        }

        // mask
        if (config.containsKey('mask')) {
            if (config.mask in CharSequence) {
                mask = config.mask as CharSequence
                log.debug("Set `validation.summary.mask` to ${mask}")
            } else {
                /* groovylint-disable-next-line LineLength */
                log.warn("Incorrect value detected for `validation.summary.mask`, a string is expected. Defaulting to `${mask}`")
            }
        }

        // maskSubpaths
        if (config.containsKey('maskSubpaths')) {
            if (config.maskSubpaths in List<CharSequence>) {
                maskSubpaths = config.maskSubpaths as List<CharSequence>
                log.debug("Set `maskSubpaths` to ${maskSubpaths}")
            } else {
                /* groovylint-disable-next-line LineLength */
                log.warn('Incorrect value detected for `validation.summary.maskSubpaths`, a list of strings is expected. Defaulting to ``')
            }
        }
    }

}
