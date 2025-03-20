package nextflow.validation

import groovy.transform.CompileStatic
import nextflow.plugin.BasePlugin
import org.pf4j.PluginWrapper

/**
 * Implements the Validation plugins entry point
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class ValidationPlugin extends BasePlugin {

    ValidationPlugin(PluginWrapper wrapper) {
        super(wrapper)
    }
}
