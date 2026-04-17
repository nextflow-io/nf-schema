/* groovylint-disable LineLength, TrailingWhitespace, MethodName, UnnecessaryGString */
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
class ParamsSummaryLogTest extends Dsl2Spec {

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

    void 'should print params summary'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath()
        String script = """
            params.outdir = "outDir"
            include { paramsSummaryLog } from 'plugin/nf-schema'
            workflow {
                def summary_params = paramsSummaryLog(workflow, parameters_schema: '$schema')
                log.info summary_params
            }
        """

        when:
        Map config = [:]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line ->
                    line.contains('Only displaying parameters that differ from the pipeline defaults') ||
                    line.contains('Core Nextflow options') ||
                    line.contains('runName') ||
                    line.contains('launchDir') ||
                    line.contains('workDir') ||
                    line.contains('projectDir') ||
                    line.contains('userName') ||
                    line.contains('profile') ||
                    line.contains('configFiles') ||
                    line.contains('Input/output options') ||
                    line.contains('outdir')
                    ? line : null
                }

        then:
        noExceptionThrown()
        stdout.size() == 11
        stdout ==~ /.*outdir     : outDir.*/
    }

    void 'should print params summary - nested parameters'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema_nested_parameters.json').toAbsolutePath()
        String script = """
            params.map.is.so.deep = "changed_value"
            include { paramsSummaryLog } from 'plugin/nf-schema'
            workflow {
                def summary_params = paramsSummaryLog(workflow, parameters_schema: '$schema')
                log.info summary_params
            }
        """

        when:
        Map config = [
            'params': [
                'map': [
                    'is': [
                        'so': [
                            'deep': true
                        ]
                    ]
                ]
            ]
        ]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line ->
                    line.contains('Only displaying parameters that differ from the pipeline defaults') ||
                    line.contains('Core Nextflow options') ||
                    line.contains('runName') ||
                    line.contains('launchDir') ||
                    line.contains('workDir') ||
                    line.contains('projectDir') ||
                    line.contains('userName') ||
                    line.contains('profile') ||
                    line.contains('configFiles') ||
                    line.contains('Nested Parameters') ||
                    line.contains('map.is.so.deep')
                    ? line : null
                }

        then:
        noExceptionThrown()
        stdout.size() == 11
        stdout ==~ /.*map.is.so.deep: changed_value.*/
    }

    void 'should print params summary - adds before and after text'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath()
        String script = """
            params.outdir = "outDir"
            include { paramsSummaryLog } from 'plugin/nf-schema'
            workflow {
                def summary_params = paramsSummaryLog(workflow, parameters_schema: '$schema')
                log.info summary_params
            }
        """

        when:
        Map config = [
            'validation': [
                'summary': [
                    'beforeText': "This text is printed before \n",
                    'afterText': "\nThis text is printed after",
                ]
            ]
        ]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> !line.contains('DEBUG') && !line.contains('after]]') ? line : null }
                .findResults { line ->
                    line.contains('Only displaying parameters that differ from the pipeline defaults') ||
                    line.contains('Core Nextflow options') ||
                    line.contains('runName') ||
                    line.contains('launchDir') ||
                    line.contains('workDir') ||
                    line.contains('projectDir') ||
                    line.contains('userName') ||
                    line.contains('profile') ||
                    line.contains('configFiles') ||
                    line.contains('Input/output options') ||
                    line.contains('outdir') ||
                    line.contains('This text is printed before') ||
                    line.contains('This text is printed after')
                    ? line : null
                }
        then:
        noExceptionThrown()
        stdout.size() == 14
        stdout ==~ /.*outdir     : outDir.*/
    }

    void 'should print params summary - adds before and after text via arguments'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath()
        String script = """
            params.outdir = 'outDir'
            include { paramsSummaryLog } from 'plugin/nf-schema'
            workflow {
                def summary_params = paramsSummaryLog(
                    workflow,
                    parameters_schema: '${schema}',
                    beforeText: "This text is printed before \\n",
                    afterText: "\\nThis text is printed after"
                )
                log.info summary_params
            }
        """

        when:
        Map config = [:]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> !line.contains('DEBUG') && !line.contains('after]]') ? line : null }
                .findResults { line ->
                    line.contains('Only displaying parameters that differ from the pipeline defaults') ||
                    line.contains('Core Nextflow options') ||
                    line.contains('runName') ||
                    line.contains('launchDir') ||
                    line.contains('workDir') ||
                    line.contains('projectDir') ||
                    line.contains('userName') ||
                    line.contains('profile') ||
                    line.contains('configFiles') ||
                    line.contains('Input/output options') ||
                    line.contains('outdir') ||
                    line.contains('This text is printed before') ||
                    line.contains('This text is printed after')
                    ? line : null 
                }

        then:
        noExceptionThrown()
        stdout.size() == 13
        stdout ==~ /.*outdir     : outDir.*/
    }

    void 'should print params summary - nested parameters - hide params'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema_nested_parameters.json').toAbsolutePath()
        String script = """
            params.map.is.so.deep = "changed_value"
            include { paramsSummaryLog } from 'plugin/nf-schema'
            workflow {
                def summary_params = paramsSummaryLog(workflow, parameters_schema: '$schema')
                log.info summary_params
            }
        """

        when:
        Map config = [
            'params': [
                'map': [
                    'is': [
                        'so': [
                            'deep': true
                        ]
                    ]
                ]
            ],
            'validation': [
                'summary': [
                    'hideParams': ['params.map.is.so.deep']
                ]
            ]
        ]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> 
                    line.contains('Only displaying parameters that differ from the pipeline defaults') ||
                    line.contains('Core Nextflow options') ||
                    line.contains('runName') ||
                    line.contains('launchDir') ||
                    line.contains('workDir') ||
                    line.contains('projectDir') ||
                    line.contains('userName') ||
                    line.contains('profile') ||
                    line.contains('configFiles') ||
                    line.contains('Nested Parameters') ||
                    line.contains('map.is.so.deep ')
                    ? line : null
                }

        then:
        noExceptionThrown()
        stdout.size() == 10
        stdout != ~ /.*map.is.so.deep: changed_value.*/
    }

    void 'should print params summary - hide params'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath()
        String script = """
            params.outdir = "outDir"
            include { paramsSummaryLog } from 'plugin/nf-schema'
            workflow {
                def summary_params = paramsSummaryLog(workflow, parameters_schema: '$schema')
                log.info summary_params
            }
        """

        when:
        Map config = [
            'validation': [
                'summary': [
                    'hideParams': ['outdir']
                ]
            ]
        ]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line ->
                    line.contains('Only displaying parameters that differ from the pipeline defaults') ||
                    line.contains('Core Nextflow options') ||
                    line.contains('runName') ||
                    line.contains('launchDir') ||
                    line.contains('workDir') ||
                    line.contains('projectDir') ||
                    line.contains('userName') ||
                    line.contains('profile') ||
                    line.contains('configFiles') ||
                    line.contains('outdir ')
                    ? line : null
                }

        then:
        noExceptionThrown()
        stdout.size() == 9
        stdout != ~ /.*outdir     : outDir.*/
    }

}
