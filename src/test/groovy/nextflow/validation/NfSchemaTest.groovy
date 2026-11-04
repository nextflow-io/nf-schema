/* groovylint-disable LineLength, MethodName, TrailingWhitespace */
package nextflow.validation

import static test.ScriptHelper.runScript

import groovy.transform.CompileDynamic

import java.nio.file.Path

import nextflow.plugin.Plugins
import nextflow.plugin.TestPluginDescriptorFinder
import nextflow.plugin.TestPluginManager
import nextflow.plugin.extension.PluginExtensionProvider
import org.junit.Rule
import org.pf4j.PluginDescriptorFinder
import spock.lang.Shared
import test.Dsl2Spec
import test.OutputCapture

import java.nio.file.Files
import java.util.jar.Manifest

/**
 * @author : mirpedrol <mirp.julia@gmail.com>
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 * @author : jorgeaguileraseqera
 */

@CompileDynamic
class NfSchemaTest extends Dsl2Spec {

    @Rule
    final private OutputCapture capture = new OutputCapture()

    @Shared
    private String pluginsMode

    void setup() {
        // reset previous instances
        PluginExtensionProvider.reset()
        // this need to be set *before* the plugin manager class is created
        pluginsMode = System.getProperty('pf4j.mode')
        System.setProperty('pf4j.mode', 'dev')
        // the plugin root should
        Path root = Path.of('.').toAbsolutePath().normalize()
        TestPluginManager manager = new TestPluginManager(root){

            @Override
            protected PluginDescriptorFinder createPluginDescriptorFinder() {
                return new TestPluginDescriptorFinder(){

                    @Override
                    protected Manifest readManifestFromDirectory(Path pluginPath) {
                        Path manifestPath = getManifestPath(pluginPath)
                        InputStream input = Files.newInputStream(manifestPath)
                        return new Manifest(input)
                    }
                    protected Path getManifestPath(Path pluginPath) {
                        return pluginPath.resolve('build/tmp/jar/MANIFEST.MF')
                    }

                }
            }

        }
        Plugins.init(root, 'dev', manager)
    }

    void cleanup() {
        Plugins.stop()
        PluginExtensionProvider.reset()
        pluginsMode ? System.setProperty('pf4j.mode', pluginsMode) : System.clearProperty('pf4j.mode')
    }

    //
    // Params validation tests
    //

    void 'should import functions'() {
        given:
        String scriptText = '''
            include { validateParameters } from 'plugin/nf-schema'
        '''

        when:
        runScript(scriptText)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        noExceptionThrown()
        !stdout
    }

}
