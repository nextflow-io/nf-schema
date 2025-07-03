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
class ParamsSummaryLogTest extends Dsl2Spec{

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
                        return pluginPath.resolve('build/tmp/jar/MANIFEST.MF')
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

    def 'should print params summary' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def  SCRIPT = """
            params.outdir = "outDir"
            include { paramsSummaryLog } from 'plugin/nf-schema'
            
            def summary_params = paramsSummaryLog(workflow, parameters_schema: '$schema')
            log.info summary_params
        """

        when:
        def config = [:]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('Only displaying parameters that differ from the pipeline defaults') ||
                    it.contains('Core Nextflow options') ||
                    it.contains('runName') ||
                    it.contains('launchDir') ||
                    it.contains('workDir') ||
                    it.contains('projectDir') ||
                    it.contains('userName') ||
                    it.contains('profile') ||
                    it.contains('configFiles') ||
                    it.contains('Input/output options') ||
                    it.contains('outdir') 
                    ? it : null }
        
        then:
        noExceptionThrown()
        stdout.size() == 11
        stdout ==~ /.*outdir     : outDir.*/
    }

    def 'should print params summary - nested parameters' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema_nested_parameters.json').toAbsolutePath().toString()
        def  SCRIPT = """
            params.this.is.so.deep = "changed_value"
            include { paramsSummaryLog } from 'plugin/nf-schema'
            
            def summary_params = paramsSummaryLog(workflow, parameters_schema: '$schema')
            log.info summary_params
        """

        when:
        def config = [
            "params": [
                "this": [
                    "is": [
                        "so": [
                            "deep": true
                        ]
                    ]
                ]
            ]
        ]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('Only displaying parameters that differ from the pipeline defaults') ||
                    it.contains('Core Nextflow options') ||
                    it.contains('runName') ||
                    it.contains('launchDir') ||
                    it.contains('workDir') ||
                    it.contains('projectDir') ||
                    it.contains('userName') ||
                    it.contains('profile') ||
                    it.contains('configFiles') ||
                    it.contains('Nested Parameters') ||
                    it.contains('this.is.so.deep') 
                    ? it : null }
        
        then:
        noExceptionThrown()
        stdout.size() == 11
        stdout ==~ /.*this.is.so.deep: changed_value.*/
    }

    def 'should print params summary - adds before and after text' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def  SCRIPT = """
            params.outdir = "outDir"
            include { paramsSummaryLog } from 'plugin/nf-schema'
            
            def summary_params = paramsSummaryLog(workflow, parameters_schema: '$schema')
            log.info summary_params
        """

        when:
        def config = [
            "validation": [
                "summary": [
                    "beforeText": "This text is printed before \n",
                    "afterText": "\nThis text is printed after",
                ]
            ]
        ]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults { !it.contains("DEBUG") && !it.contains("after]]") ? it : null }
                .findResults {it.contains('Only displaying parameters that differ from the pipeline defaults') ||
                    it.contains('Core Nextflow options') ||
                    it.contains('runName') ||
                    it.contains('launchDir') ||
                    it.contains('workDir') ||
                    it.contains('projectDir') ||
                    it.contains('userName') ||
                    it.contains('profile') ||
                    it.contains('configFiles') ||
                    it.contains('Input/output options') ||
                    it.contains('outdir') ||
                    it.contains('This text is printed before') ||
                    it.contains('This text is printed after')
                    ? it : null }
        
        then:
        noExceptionThrown()
        stdout.size() == 14
        stdout ==~ /.*outdir     : outDir.*/
    }

    def 'should print params summary - nested parameters - hide params' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema_nested_parameters.json').toAbsolutePath().toString()
        def  SCRIPT = """
            params.this.is.so.deep = "changed_value"
            include { paramsSummaryLog } from 'plugin/nf-schema'
            
            def summary_params = paramsSummaryLog(workflow, parameters_schema: '$schema')
            log.info summary_params
        """

        when:
        def config = [
            "params": [
                "this": [
                    "is": [
                        "so": [
                            "deep": true
                        ]
                    ]
                ]
            ],
            "validation": [
                "summary": [
                    "hideParams": ["params.this.is.so.deep"]
                ]
            ]
        ]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('Only displaying parameters that differ from the pipeline defaults') ||
                    it.contains('Core Nextflow options') ||
                    it.contains('runName') ||
                    it.contains('launchDir') ||
                    it.contains('workDir') ||
                    it.contains('projectDir') ||
                    it.contains('userName') ||
                    it.contains('profile') ||
                    it.contains('configFiles') ||
                    it.contains('Nested Parameters') ||
                    it.contains('this.is.so.deep ') 
                    ? it : null }
        
        then:
        noExceptionThrown()
        stdout.size() == 10
        stdout !=~ /.*this.is.so.deep: changed_value.*/
    }

    def 'should print params summary - hide params' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def  SCRIPT = """
            params.outdir = "outDir"
            include { paramsSummaryLog } from 'plugin/nf-schema'
            
            def summary_params = paramsSummaryLog(workflow, parameters_schema: '$schema')
            log.info summary_params
        """

        when:
        def config = [
            "validation": [
                "summary": [
                    "hideParams": ["outdir"]
                ]
            ]
        ]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('Only displaying parameters that differ from the pipeline defaults') ||
                    it.contains('Core Nextflow options') ||
                    it.contains('runName') ||
                    it.contains('launchDir') ||
                    it.contains('workDir') ||
                    it.contains('projectDir') ||
                    it.contains('userName') ||
                    it.contains('profile') ||
                    it.contains('configFiles') ||
                    it.contains('outdir ') 
                    ? it : null }
        
        then:
        noExceptionThrown()
        stdout.size() == 9
        stdout !=~ /.*outdir     : outDir.*/
    }
}