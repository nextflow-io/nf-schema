/* groovylint-disable LineLength, MethodName */
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

import nextflow.validation.exceptions.SchemaValidationException

import java.nio.file.Files
import java.util.jar.Manifest

/**
 * Tests for retrying `pattern` validation against the resolved real path of values on
 * non-local filesystems (https://github.com/nextflow-io/nf-schema/issues/217).
 *
 * @author : rcannood <rcannood@gmail.com>
 */

@CompileDynamic
class ValidateResolvedPathPatternTest extends Dsl2Spec {

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
        FakeProxyFileSystemProvider.reset()
    }

    void 'should pass when a remote path matches the pattern directly'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema_resolved_path_pattern.json').toAbsolutePath()
        String script = """
            params.input = 'fakeproxy://registry/artifact/data.csv'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map opts = ['config': ['validation': [
            'monochromeLogs': true
        ]]]
        runScript(opts, script)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        noExceptionThrown()
        !stdout
    }

    void 'should pass when the resolved path matches the pattern'() {
        given:
        FakeProxyFileSystemProvider.register(
            'fakeproxy://registry/artifact/abc123',
            Path.of('src/testResources/correct.csv').toAbsolutePath()
        )
        String schema = Path.of('src/testResources/nextflow_schema_resolved_path_pattern.json').toAbsolutePath()
        String script = """
            params.input = 'fakeproxy://registry/artifact/abc123'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map opts = ['config': ['validation': [
            'monochromeLogs': true
        ]]]
        runScript(opts, script)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        noExceptionThrown()
        !stdout
    }

    void 'should fail when neither the value nor the resolved path matches'() {
        given:
        FakeProxyFileSystemProvider.register(
            'fakeproxy://registry/artifact/abc123',
            Path.of('src/testResources/correct.tsv').toAbsolutePath()
        )
        String schema = Path.of('src/testResources/nextflow_schema_resolved_path_pattern.json').toAbsolutePath()
        String script = """
            params.input = 'fakeproxy://registry/artifact/abc123'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map opts = ['config': ['validation': [
            'monochromeLogs': true
        ]]]
        runScript(opts, script)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message.contains('* --input (fakeproxy://registry/artifact/abc123): "fakeproxy://registry/artifact/abc123" does not match regular expression ^\\S+\\.csv$')
        !stdout
    }

    void 'should fail with the standard message when the remote path cannot be resolved'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema_resolved_path_pattern.json').toAbsolutePath()
        String script = """
            params.input = 'fakeproxy://registry/artifact/unresolvable'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map opts = ['config': ['validation': [
            'monochromeLogs': true
        ]]]
        runScript(opts, script)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message.contains('* --input (fakeproxy://registry/artifact/unresolvable): "fakeproxy://registry/artifact/unresolvable" does not match regular expression ^\\S+\\.csv$')
        !stdout
    }

    void 'should keep failing for local paths that do not match the pattern'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema_resolved_path_pattern.json').toAbsolutePath()
        String script = """
            params.input = 'src/testResources/correct.tsv'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map opts = ['config': ['validation': [
            'monochromeLogs': true
        ]]]
        runScript(opts, script)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message.contains('* --input (src/testResources/correct.tsv): "src/testResources/correct.tsv" does not match regular expression ^\\S+\\.csv$')
        !stdout
    }

    void 'should not resolve local symlinks'() {
        given:
        Path tempDir = Files.createTempDirectory('nf-schema-test')
        Path target = Files.createFile(tempDir.resolve('data.csv'))
        Path link = Files.createSymbolicLink(tempDir.resolve('data_link'), target)
        String schema = Path.of('src/testResources/nextflow_schema_resolved_path_pattern.json').toAbsolutePath()
        String script = """
            params.input = '${link}'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map opts = ['config': ['validation': [
            'monochromeLogs': true
        ]]]
        runScript(opts, script)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message.contains("* --input (${link}): \"${link}\" does not match regular expression ^\\S+\\.csv\$")
        !stdout

        cleanup:
        Files.deleteIfExists(link)
        Files.deleteIfExists(target)
        Files.deleteIfExists(tempDir)
    }

    void 'should pass a samplesheet containing remote paths with matching resolved paths'() {
        given:
        FakeProxyFileSystemProvider.register(
            'fakeproxy://registry/artifact/aaa111',
            Path.of('src/testResources/correct.csv').toAbsolutePath()
        )
        String schema = Path.of('src/testResources/nextflow_schema_with_resolved_path_samplesheet.json').toAbsolutePath()
        String script = """
            params.input = 'src/testResources/resolved_path_samplesheet.csv'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map opts = ['config': ['validation': [
            'monochromeLogs': true
        ]]]
        runScript(opts, script)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        noExceptionThrown()
        !stdout
    }

    void 'should fail a samplesheet containing remote paths with non-matching resolved paths'() {
        given:
        FakeProxyFileSystemProvider.register(
            'fakeproxy://registry/artifact/aaa111',
            Path.of('src/testResources/correct.tsv').toAbsolutePath()
        )
        String schema = Path.of('src/testResources/nextflow_schema_with_resolved_path_samplesheet.json').toAbsolutePath()
        String script = """
            params.input = 'src/testResources/resolved_path_samplesheet.csv'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map opts = ['config': ['validation': [
            'monochromeLogs': true
        ]]]
        runScript(opts, script)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        List<String> errorMessage = error.message.tokenize('\n')
        errorMessage[0] == 'The following invalid input values have been detected:'
        errorMessage[1] == '* --input (src/testResources/resolved_path_samplesheet.csv): Validation of file failed:'
        errorMessage[2] == "\t-> Entry 1: Error for field 'data' (fakeproxy://registry/artifact/aaa111): \"fakeproxy://registry/artifact/aaa111\" does not match regular expression ^\\S+\\.csv\$ (Data file must end in '.csv')"
        !stdout
    }

}
