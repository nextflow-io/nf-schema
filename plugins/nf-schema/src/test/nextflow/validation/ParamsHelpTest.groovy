package nextflow.validation

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
import test.MockScriptRunner

import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.Manifest

/**
 * @author : mirpedrol <mirp.julia@gmail.com>
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 * @author : KevinMenden
 */
class ParamsHelpTest extends Dsl2Spec{

    @Rule
    OutputCapture capture = new OutputCapture()


    @Shared String pluginsMode

    Path root = Path.of('.').toAbsolutePath().normalize()
    Path getRoot() { this.root }
    String getRootString() { this.root.toString() }

    def setup() {
        // reset previous instances
        PluginExtensionProvider.reset()
        // this need to be set *before* the plugin manager class is created
        pluginsMode = System.getProperty('pf4j.mode')
        System.setProperty('pf4j.mode', 'dev')
        // the plugin root should
        def root = this.getRoot()
        def manager = new TestPluginManager(root){
            @Override
            protected PluginDescriptorFinder createPluginDescriptorFinder() {
                return new TestPluginDescriptorFinder(){
                    @Override
                    protected Manifest readManifestFromDirectory(Path pluginPath) {
                        def manifestPath = getManifestPath(pluginPath)
                        final input = Files.newInputStream(manifestPath)
                        return new Manifest(input)
                    }
                    protected Path getManifestPath(Path pluginPath) {
                        return pluginPath.resolve('build/resources/main/META-INF/MANIFEST.MF')
                    }
                }
            }
        }
        Plugins.init(root, 'dev', manager)
    }

    def cleanup() {
        Plugins.stop()
        PluginExtensionProvider.reset()
        pluginsMode ? System.setProperty('pf4j.mode',pluginsMode) : System.clearProperty('pf4j.mode')
    }

    def 'should print a help message' () {
        when:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def SCRIPT = """
            include { paramsHelp } from 'plugin/nf-schema'

            def command = "nextflow run <pipeline> --input samplesheet.csv --outdir <OUTDIR> -profile docker"
            
            def help_msg = paramsHelp(command, parameters_schema: '$schema')
            log.info help_msg
        """

        and:
        def result = new MockScriptRunner([:]).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('Typical pipeline command:') ||
                    it.contains('nextflow run') ||
                    it.contains('Input/output options') ||
                    it.contains('--input') ||
                    it.contains('--outdir') ||
                    it.contains('--email') ||
                    it.contains('--multiqc_title') ||
                    it.contains('Reference genome options') ||
                    it.contains('--genome') ||
                    it.contains('--fasta') 
                    ? it : null }

        then:
        noExceptionThrown()
        stdout.size() == 10
    }

    def 'should print a help message with argument options' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def SCRIPT = """
            include { paramsHelp } from 'plugin/nf-schema'
            def command = "nextflow run <pipeline> --input samplesheet.csv --outdir <OUTDIR> -profile docker"
            
            def help_msg = paramsHelp(command, parameters_schema: '$schema')
            log.info help_msg
        """

        when:
        def config = ["validation": [
            "help": [
                "showHidden": true
            ]
        ]]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('publish_dir_mode') && 
                    it.contains('(accepted: symlink, rellink') 
                    ? it : null }

        then:
        noExceptionThrown()
        stdout.size() == 1
    }

    def 'should print a help message of one parameter' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def SCRIPT = """
            include { paramsHelp } from 'plugin/nf-schema'
            params.help = 'publish_dir_mode'

            def command = "nextflow run <pipeline> --input samplesheet.csv --outdir <OUTDIR> -profile docker"
            
            def help_msg = paramsHelp(command, parameters_schema: '$schema')
            log.info help_msg
        """

        when:
        def result = new MockScriptRunner([validation:[monochromeLogs:true]]).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.startsWith('--publish_dir_mode') ||
                    it.contains('type       :') ||
                    it.contains('default    :') ||
                    it.contains('description:') ||
                    it.contains('help_text  :') ||
                    it.contains('fa_icon    :') || // fa_icon shouldn't be printed
                    it.contains('enum       :') ||
                    it.contains('hidden     :') 
                    ? it : null }

        then:
        noExceptionThrown()
        stdout.size() == 7
    }

    def 'should fail when help param doesnt exist' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def SCRIPT = """
            include { paramsHelp } from 'plugin/nf-schema'
            params.help = 'no_exist'

            def command = "nextflow run <pipeline> --input samplesheet.csv --outdir <OUTDIR> -profile docker"
            
            def help_msg = paramsHelp(command, parameters_schema: '$schema')
            log.info help_msg
        """

        when:
        def result = new MockScriptRunner([:]).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.startsWith('--no_exist') ? it : null }

        then:
        def error = thrown(Exception)
        error.message == "Unable to create help message: Specified param 'no_exist' does not exist in JSON schema."
        !stdout
    }

    def 'should print a help message of nested parameter' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema_nested_parameters.json').toAbsolutePath().toString()
        def SCRIPT = """
            include { paramsHelp } from 'plugin/nf-schema'
            params.help = 'this.is'

            def command = "nextflow run <pipeline> --input samplesheet.csv --outdir <OUTDIR> -profile docker"
            
            def help_msg = paramsHelp(command, parameters_schema: '$schema')
            log.info help_msg
        """

        when:
        def result = new MockScriptRunner([validation:[monochromeLogs:true]]).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.startsWith('--this.is') ||
                    it.contains('description:') ||
                    it.contains('options    :') ||
                    it.contains('this.is.so.deep')
                    ? it : null }

        then:
        noExceptionThrown()
        stdout.size() == 4
    }
}