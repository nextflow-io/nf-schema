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
 * @author : KevinMenden
 */

@CompileDynamic
class ParamsHelpTest extends Dsl2Spec {

    @Rule
    final private OutputCapture capture = new OutputCapture()

    @Shared 
    private String pluginsMode

    final private Path root = Path.of('.').toAbsolutePath().normalize()

    void setup() {
        // reset previous instances
        PluginExtensionProvider.reset()
        // this need to be set *before* the plugin manager class is created
        pluginsMode = System.getProperty('pf4j.mode')
        System.setProperty('pf4j.mode', 'dev')
        // the plugin root should
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

    void 'should print a help message'() {
        when:
        String schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath()
        String script = """
            include { paramsHelp } from 'plugin/nf-schema'
            workflow {
                def command = "nextflow run <pipeline> --input samplesheet.csv --outdir <OUTDIR> -profile docker"

                def help_msg = paramsHelp(parameters_schema: '$schema', command: command)
                log.info help_msg
            }
        """

        and:
        runScript(script)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line ->
                    line.contains('Typical pipeline command:') ||
                    line.contains('nextflow run') ||
                    line.contains('Input/output options') ||
                    line.contains('--input') ||
                    line.contains('--outdir') ||
                    line.contains('--email') ||
                    line.contains('--multiqc_title') ||
                    line.contains('Reference genome options') ||
                    line.contains('--genome') ||
                    line.contains('--fasta')
                    ? line : null
                }

        then:
        noExceptionThrown()
        stdout.size() == 12
    }

    void 'should print a help message with argument options'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath()
        String script = """
            include { paramsHelp } from 'plugin/nf-schema'
            workflow {
                def command = "nextflow run <pipeline> --input samplesheet.csv --outdir <OUTDIR> -profile docker"

                def help_msg = paramsHelp(parameters_schema: '$schema', command: command, showHidden: true)
                log.info help_msg
            }
        """

        when:
        runScript(script)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> 
                    line.contains('publish_dir_mode') && line.contains('(accepted: symlink, rellink') ?
                    line : 
                    null
                }

        then:
        noExceptionThrown()
        stdout.size() == 1
    }

    void 'should print a help message of one parameter'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath()
        String script = """
            include { paramsHelp } from 'plugin/nf-schema'
            workflow {
                def command = "nextflow run <pipeline> --input samplesheet.csv --outdir <OUTDIR> -profile docker"

                def help_msg = paramsHelp("publish_dir_mode", parameters_schema: '$schema', command: command)
                log.info help_msg
            }
        """

        when:
        runScript(script, [validation:[monochromeLogs:true]])
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line ->
                    line.startsWith('--publish_dir_mode') ||
                    line.contains('type       :') ||
                    line.contains('default    :') ||
                    line.contains('description:') ||
                    line.contains('help_text  :') ||
                    line.contains('fa_icon    :') || // fa_icon shouldn't be printed
                    line.contains('enum       :') ||
                    line.contains('hidden     :')
                    ? line : null
                }

        then:
        noExceptionThrown()
        stdout.size() == 7
    }

    void 'should fail when help param doesnt exist'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath()
        String script = """
            include { paramsHelp } from 'plugin/nf-schema'
            workflow {
                def command = "nextflow run <pipeline> --input samplesheet.csv --outdir <OUTDIR> -profile docker"

                def help_msg = paramsHelp("no_exist", parameters_schema: '$schema', command: command)
                log.info help_msg
            }
        """

        when:
        runScript(script)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.startsWith('--no_exist') ? line : null }

        then:
        Exception error = thrown(Exception)
        error.message == "Unable to create help message: Specified param 'no_exist' does not exist in JSON schema."
        !stdout
    }

    void 'should print a help message of nested parameter'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema_nested_parameters.json').toAbsolutePath()
        String script = """
            include { paramsHelp } from 'plugin/nf-schema'
            workflow {
                def command = "nextflow run <pipeline> --input samplesheet.csv --outdir <OUTDIR> -profile docker"

                def help_msg = paramsHelp("this.is", parameters_schema: '$schema', command: command)
                log.info help_msg
            }
        """

        when:
        runScript(script, [validation:[monochromeLogs:true]])
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line ->
                    line.startsWith('--this.is') ||
                    line.contains('description:') ||
                    line.contains('options    :') ||
                    line.contains('this.is.so.deep')
                    ? line : null
                }

        then:
        noExceptionThrown()
        stdout.size() == 4
    }

}
