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

import nextflow.validation.exceptions.SchemaValidationException

import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.Manifest

/**
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 * @author : jorgeaguileraseqera
 */
class ValidateTest extends Dsl2Spec{

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

    def 'should validate a map - success' () {
        given:
        def SCRIPT = """
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
        def config = ["validation": [
            "monochromeLogs": true
        ]]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('\\') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'should validate a map - failure' () {
        given:
        def SCRIPT = """
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
        def config = ["validation": [
            "monochromeLogs": true
        ]]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('\\') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        error.message == "/this/is/so/deep (a wrong string): Value is [string] but should be [boolean]\n({\"this\":{\"is\":{\"so\":{\"deep\":\"a wrong string\"}}}}): Value does not match against the schemas at indexes [0]\n"
        !stdout
    }

    def 'should validate a list - success' () {
        given:
        def SCRIPT = """
            include { validate } from 'plugin/nf-schema'
            
            def input = ["value"]

            validate(input, 'src/testResources/no_header_schema.json')
        """

        when:
        def config = ["validation": [
            "monochromeLogs": true
        ]]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('\\') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'should validate a list - failure' () {
        given:
        def SCRIPT = """
            include { validate } from 'plugin/nf-schema'
            
            def input = [12]

            validate(input, 'src/testResources/no_header_schema.json')
        """

        when:
        def config = ["validation": [
            "monochromeLogs": true
        ]]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('\\') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        error.message == "/0 (12): Value is [integer] but should be [string]\n"
        !stdout
    }

    def 'should validate a string - success' () {
        given:
        def SCRIPT = """
            include { validate } from 'plugin/nf-schema'
            
            def input = "value"

            validate(input, 'src/testResources/string_schema.json')
        """

        when:
        def config = ["validation": [
            "monochromeLogs": true
        ]]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('\\') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'should validate a string - failure' () {
        given:
        def SCRIPT = """
            include { validate } from 'plugin/nf-schema'
            
            def input = 12

            validate(input, 'src/testResources/string_schema.json')
        """

        when:
        def config = ["validation": [
            "monochromeLogs": true
        ]]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('\\') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        error.message == "(12): Value is [integer] but should be [string]\n"
        !stdout
    }

    def 'should validate a integer - success' () {
        given:
        def SCRIPT = """
            include { validate } from 'plugin/nf-schema'
            
            def input = 12

            validate(input, 'src/testResources/integer_schema.json')
        """

        when:
        def config = ["validation": [
            "monochromeLogs": true
        ]]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('\\') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'should validate a integer - failure' () {
        given:
        def SCRIPT = """
            include { validate } from 'plugin/nf-schema'
            
            def input = "value"

            validate(input, 'src/testResources/integer_schema.json')
        """

        when:
        def config = ["validation": [
            "monochromeLogs": true
        ]]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('\\') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        error.message == "(value): Value is [string] but should be [integer]\n"
        !stdout
    }

    def 'should validate a boolean - success' () {
        given:
        def SCRIPT = """
            include { validate } from 'plugin/nf-schema'
            
            def input = true

            validate(input, 'src/testResources/boolean_schema.json')
        """

        when:
        def config = ["validation": [
            "monochromeLogs": true
        ]]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('\\') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'should validate a boolean - failure' () {
        given:
        def SCRIPT = """
            include { validate } from 'plugin/nf-schema'
            
            def input = "value"

            validate(input, 'src/testResources/boolean_schema.json')
        """

        when:
        def config = ["validation": [
            "monochromeLogs": true
        ]]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('\\') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        error.message == "(value): Value is [string] but should be [boolean]\n"
        !stdout
    }
}