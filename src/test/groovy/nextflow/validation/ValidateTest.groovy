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

import nextflow.validation.exceptions.SchemaValidationException

import java.nio.file.Files
import java.util.jar.Manifest

/**
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 * @author : jorgeaguileraseqera
 */

@CompileDynamic
class ValidateTest extends Dsl2Spec {

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

    void 'should validate a map - success'() {
        given:
        String script = """
            include { validate } from 'plugin/nf-schema'

            def input = [
                this: [
                    is: [
                        so: [
                            deep: true
                        ]
                    ]
                ]
            ]

            validate(input, 'src/testResources/nextflow_schema_nested_parameters.json')
        """

        when:
        Map config = ['validation': [
            'monochromeLogs': true
        ]]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('\\') ? line : null }

        then:
        noExceptionThrown()
        !stdout
    }

    void 'should validate a map - failure'() {
        given:
        String script = """
            include { validate } from 'plugin/nf-schema'

            def input = [
                this: [
                    is: [
                        so: [
                            deep: "a wrong string"
                        ]
                    ]
                ]
            ]

            validate(input, 'src/testResources/nextflow_schema_nested_parameters.json')
        """

        when:
        Map config = ['validation': [
            'monochromeLogs': true
        ]]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('\\') ? line : null }

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message == '/this/is/so/deep (a wrong string): Value is [string] but should be [boolean]\n({"this":{"is":{"so":{"deep":"a wrong string"}}}}): Value does not match against the schemas at indexes [0]\n'
        !stdout
    }

    void 'should validate a list - success'() {
        given:
        String script = '''
            include { validate } from 'plugin/nf-schema'

            def input = ["value"]

            validate(input, 'src/testResources/no_header_schema.json')
        '''

        when:
        Map config = ['validation': [
            'monochromeLogs': true
        ]]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('\\') ? line : null }

        then:
        noExceptionThrown()
        !stdout
    }

    void 'should validate a list - failure'() {
        given:
        String script = '''
            include { validate } from 'plugin/nf-schema'

            def input = [12]

            validate(input, 'src/testResources/no_header_schema.json')
        '''

        when:
        Map config = ['validation': [
            'monochromeLogs': true
        ]]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('\\') ? line : null }

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message == '/0 (12): Value is [integer] but should be [string]\n'
        !stdout
    }

    void 'should validate a string - success'() {
        given:
        String script = '''
            include { validate } from 'plugin/nf-schema'

            def input = "value"

            validate(input, 'src/testResources/string_schema.json')
        '''

        when:
        Map config = ['validation': [
            'monochromeLogs': true
        ]]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('\\') ? line : null }

        then:
        noExceptionThrown()
        !stdout
    }

    void 'should validate a string - failure'() {
        given:
        String script = '''
            include { validate } from 'plugin/nf-schema'

            def input = 12

            validate(input, 'src/testResources/string_schema.json')
        '''

        when:
        Map config = ['validation': [
            'monochromeLogs': true
        ]]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('\\') ? line : null }

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message == '(12): Value is [integer] but should be [string]\n'
        !stdout
    }

    void 'should validate a integer - success'() {
        given:
        String script = '''
            include { validate } from 'plugin/nf-schema'

            def input = 12

            validate(input, 'src/testResources/integer_schema.json')
        '''

        when:
        Map config = ['validation': [
            'monochromeLogs': true
        ]]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('\\') ? line : null }

        then:
        noExceptionThrown()
        !stdout
    }

    void 'should validate a integer - failure'() {
        given:
        String script = '''
            include { validate } from 'plugin/nf-schema'

            def input = "value"

            validate(input, 'src/testResources/integer_schema.json')
        '''

        when:
        Map config = ['validation': [
            'monochromeLogs': true
        ]]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('\\') ? line : null }

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message == '(value): Value is [string] but should be [integer]\n'
        !stdout
    }

    void 'should validate a boolean - success'() {
        given:
        String script = '''
            include { validate } from 'plugin/nf-schema'

            def input = true

            validate(input, 'src/testResources/boolean_schema.json')
        '''

        when:
        Map config = ['validation': [
            'monochromeLogs': true
        ]]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('\\') ? line : null }

        then:
        noExceptionThrown()
        !stdout
    }

    void 'should validate a boolean - failure'() {
        given:
        String script = '''
            include { validate } from 'plugin/nf-schema'

            def input = "value"

            validate(input, 'src/testResources/boolean_schema.json')
        '''

        when:
        Map config = ['validation': [
            'monochromeLogs': true
        ]]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('\\') ? line : null }

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message == '(value): Value is [string] but should be [boolean]\n'
        !stdout
    }

}
