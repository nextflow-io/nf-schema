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
 * @author : mirpedrol <mirp.julia@gmail.com>
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */
class SamplesheetConverterTest extends Dsl2Spec{

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

    def 'should work fine - CSV' () {
        given:
        def SCRIPT_TEXT = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            params.input = "src/testResources/correct.csv"
            params.schema = "src/testResources/schema_input.json"

            workflow {
                Channel.fromList(samplesheetToList(params.input, params.schema))
                    .view()
            }
        '''

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.startsWith('[[') ? it : null }

        then:
        noExceptionThrown()
        stdout.contains("[[string1:fullField, string2:fullField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25.12, false, ${getRootString()}/src/testResources/test.txt, ${getRootString()}/src/testResources/testDir, ${getRootString()}/src/testResources/test.txt, unique1, 1, itDoesExist]" as String)
        stdout.contains("[[string1:value, string2:value, integer1:0, integer2:0, boolean1:true, boolean2:true], string1, 25.08, false, [], [], [], [], [], itDoesExist]")
        stdout.contains("[[string1:dependentRequired, string2:dependentRequired, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, [], [], [], unique2, 1, itDoesExist]")
        stdout.contains("[[string1:extraField, string2:extraField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${getRootString()}/src/testResources/test.txt, ${getRootString()}/src/testResources/testDir, ${getRootString()}/src/testResources/testDir, unique3, 1, itDoesExist]" as String)
    }

    def 'should work fine - quoted CSV' () {
        given:
        def SCRIPT_TEXT = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            params.input = "src/testResources/correct_quoted.csv"
            params.schema = "src/testResources/schema_input.json"

            workflow {
                Channel.fromList(samplesheetToList(params.input, params.schema))
                    .view()
            }
        '''

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.startsWith('[[') ? it : null }

        then:
        noExceptionThrown()
        stdout.contains("[[string1:fullField, string2:fullField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25.12, false, ${getRootString()}/src/testResources/test.txt, ${getRootString()}/src/testResources/testDir, ${getRootString()}/src/testResources/test.txt, unique1, 1, itDoesExist]" as String)
        stdout.contains("[[string1:value, string2:value, integer1:0, integer2:0, boolean1:true, boolean2:true], string1, 25.08, false, [], [], [], [], [], itDoesExist]")
        stdout.contains("[[string1:dependentRequired, string2:dependentRequired, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, [], [], [], unique2, 1, itDoesExist]")
        stdout.contains("[[string1:extraField, string2:extraField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${getRootString()}/src/testResources/test.txt, ${getRootString()}/src/testResources/testDir, ${getRootString()}/src/testResources/testDir, unique3, 1, itDoesExist]" as String)
    }

    def 'should work fine - TSV' () {
        given:
        def SCRIPT_TEXT = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            params.input = "src/testResources/correct.tsv"
            params.schema = "src/testResources/schema_input.json"

            workflow {
                Channel.fromList(samplesheetToList(params.input, params.schema))
                    .view()
            }
        '''

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.startsWith('[[') ? it : null }

        then:
        noExceptionThrown()
        stdout.contains("[[string1:fullField, string2:fullField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25.12, false, ${getRootString()}/src/testResources/test.txt, ${getRootString()}/src/testResources/testDir, ${getRootString()}/src/testResources/test.txt, unique1, 1, itDoesExist]" as String)
        stdout.contains("[[string1:value, string2:value, integer1:0, integer2:0, boolean1:true, boolean2:true], string1, 25.08, false, [], [], [], [], [], itDoesExist]")
        stdout.contains("[[string1:dependentRequired, string2:dependentRequired, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, [], [], [], unique2, 1, itDoesExist]")
        stdout.contains("[[string1:extraField, string2:extraField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${getRootString()}/src/testResources/test.txt, ${getRootString()}/src/testResources/testDir, ${getRootString()}/src/testResources/testDir, unique3, 1, itDoesExist]" as String)
    }

    def 'should work fine - YAML' () {
        given:
        def SCRIPT_TEXT = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            params.input = "src/testResources/correct.yaml"
            params.schema = "src/testResources/schema_input.json"

            workflow {
                Channel.fromList(samplesheetToList(params.input, params.schema))
                    .view()
            }
        '''

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.startsWith('[[') ? it : null }

        then:
        noExceptionThrown()
        stdout.contains("[[string1:fullField, string2:fullField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25.12, false, ${getRootString()}/src/testResources/test.txt, ${getRootString()}/src/testResources/testDir, ${getRootString()}/src/testResources/test.txt, unique1, 1, itDoesExist]" as String)
        stdout.contains("[[string1:value, string2:value, integer1:0, integer2:0, boolean1:true, boolean2:true], string1, 25.08, false, [], [], [], [], [], itDoesExist]")
        stdout.contains("[[string1:dependentRequired, string2:dependentRequired, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, [], [], [], unique2, 1, itDoesExist]")
        stdout.contains("[[string1:extraField, string2:extraField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${getRootString()}/src/testResources/test.txt, ${getRootString()}/src/testResources/testDir, ${getRootString()}/src/testResources/testDir, unique3, 1, itDoesExist]" as String)
    }

    def 'should work fine - JSON' () {
        given:
        def SCRIPT_TEXT = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            params.input = "src/testResources/correct.json"
            params.schema = "src/testResources/schema_input.json"

            workflow {
                Channel.fromList(samplesheetToList(params.input, params.schema))
                    .view()
            }
        '''

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.startsWith('[[') ? it : null }

        then:
        noExceptionThrown()
        stdout.contains("[[string1:fullField, string2:fullField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25.12, false, ${getRootString()}/src/testResources/test.txt, ${getRootString()}/src/testResources/testDir, ${getRootString()}/src/testResources/test.txt, unique1, 1, itDoesExist]" as String)
        stdout.contains("[[string1:value, string2:value, integer1:0, integer2:0, boolean1:true, boolean2:true], string1, 25.08, false, [], [], [], [], [], itDoesExist]")
        stdout.contains("[[string1:dependentRequired, string2:dependentRequired, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, [], [], [], unique2, 1, itDoesExist]")
        stdout.contains("[[string1:extraField, string2:extraField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${getRootString()}/src/testResources/test.txt, ${getRootString()}/src/testResources/testDir, ${getRootString()}/src/testResources/testDir, unique3, 1, itDoesExist]" as String)
    }

    def 'arrays should work fine - YAML' () {
        given:
        def SCRIPT_TEXT = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            params.input = "src/testResources/correct_arrays.yaml"
            params.schema = "src/testResources/schema_input_with_arrays.json"

            workflow {
                Channel.fromList(samplesheetToList(params.input, params.schema))
                    .view()
            }
        '''

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.startsWith('[[') ? it : null }

        then:
        noExceptionThrown()
        stdout.contains("[[array_meta:[]], [${getRootString()}/src/testResources/testDir/testFile.txt, ${getRootString()}/src/testResources/testDir2/testFile2.txt], [${getRootString()}/src/testResources/testDir, ${getRootString()}/src/testResources/testDir2], [${getRootString()}/src/testResources/testDir, ${getRootString()}/src/testResources/testDir2/testFile2.txt], [string1, string2], [25, 26], [25, 26.5], [false, true], [1, 2, 3], [true], [${getRootString()}/src/testResources/testDir/testFile.txt], [[${getRootString()}/src/testResources/testDir/testFile.txt]]]" as String)
        stdout.contains("[[array_meta:[look, an, array, in, meta]], [], [], [], [string1, string2], [25, 26], [25, 26.5], [], [1, 2, 3], [false, true, false], [${getRootString()}/src/testResources/testDir/testFile.txt], [[${getRootString()}/src/testResources/testDir/testFile.txt]]]" as String)
        stdout.contains("[[array_meta:[]], [], [], [], [string1, string2], [25, 26], [25, 26.5], [], [1, 2, 3], [false, true, false], [${getRootString()}/src/testResources/testDir/testFile.txt], [[${getRootString()}/src/testResources/testDir/testFile.txt], [${getRootString()}/src/testResources/testDir/testFile.txt, ${getRootString()}/src/testResources/testDir2/testFile2.txt]]]" as String)
    }

    def 'arrays should work fine - JSON' () {
        given:
        def SCRIPT_TEXT = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            params.input = "src/testResources/correct_arrays.json"
            params.schema = "src/testResources/schema_input_with_arrays.json"

            workflow {
                Channel.fromList(samplesheetToList(params.input, params.schema))
                    .view()
            }
        '''

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.startsWith('[[') ? it : null }

        then:
        noExceptionThrown()
        stdout.contains("[[array_meta:[]], [${getRootString()}/src/testResources/testDir/testFile.txt, ${getRootString()}/src/testResources/testDir2/testFile2.txt], [${getRootString()}/src/testResources/testDir, ${getRootString()}/src/testResources/testDir2], [${getRootString()}/src/testResources/testDir, ${getRootString()}/src/testResources/testDir2/testFile2.txt], [string1, string2], [25, 26], [25, 26.5], [false, true], [1, 2, 3], [true], [${getRootString()}/src/testResources/testDir/testFile.txt], [[${getRootString()}/src/testResources/testDir/testFile.txt]]]" as String)
        stdout.contains("[[array_meta:[look, an, array, in, meta]], [], [], [], [string1, string2], [25, 26], [25, 26.5], [], [1, 2, 3], [false, true, false], [${getRootString()}/src/testResources/testDir/testFile.txt], [[${getRootString()}/src/testResources/testDir/testFile.txt]]]" as String)
        stdout.contains("[[array_meta:[]], [], [], [], [string1, string2], [25, 26], [25, 26.5], [], [1, 2, 3], [false, true, false], [${getRootString()}/src/testResources/testDir/testFile.txt], [[${getRootString()}/src/testResources/testDir/testFile.txt], [${getRootString()}/src/testResources/testDir/testFile.txt, ${getRootString()}/src/testResources/testDir2/testFile2.txt]]]" as String)
    }

    def 'no header - CSV' () {
        given:
        def SCRIPT_TEXT = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            params.input = "src/testResources/no_header.csv"
            params.schema = "src/testResources/no_header_schema.json"

            workflow {
                Channel.fromList(samplesheetToList(params.input, params.schema))
                    .view()
            }
        '''

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()

        then:
        noExceptionThrown()
        stdout.contains("test_1")
        stdout.contains("test_2")
    }

    def 'no header - YAML' () {
        given:
        def SCRIPT_TEXT = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            params.input = "src/testResources/no_header.yaml"
            params.schema = "src/testResources/no_header_schema.json"

            workflow {
                Channel.fromList(samplesheetToList(params.input, params.schema))
                    .view()
            }
        '''

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()

        then:
        noExceptionThrown()
        stdout.contains("test_1")
        stdout.contains("test_2")
    }

    def 'no header - JSON' () {
        given:
        def SCRIPT_TEXT = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            params.input = "src/testResources/no_header.json"
            params.schema = "src/testResources/no_header_schema.json"

            workflow {
                Channel.fromList(samplesheetToList(params.input, params.schema))
                    .view()
            }
        '''

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()

        then:
        noExceptionThrown()
        stdout.contains("test_1")
        stdout.contains("test_2")
    }

    def 'extra field' () {
        given:
        def SCRIPT = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            params.input = "src/testResources/extraFields.csv"
            params.schema = "src/testResources/schema_input.json"

            workflow {
                Channel.fromList(samplesheetToList(params.input, params.schema))
                    .view()
            }
        '''

        when:
        def config = [
            "validation": [
                "logging": [
                    "unrecognisedHeaders": "warn"
                ]
            ]
        ]
        new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .collect {
                    it.split("ValidationLogger -- ")[-1]
                }

        then:
        noExceptionThrown()
        stdout.contains("Found the following unidentified headers in ${getRootString()}/src/testResources/extraFields.csv:" as String)
        stdout.contains("\t- extraField")
        stdout.contains("[[string1:fullField, string2:fullField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${getRootString()}/src/testResources/test.txt, ${getRootString()}/src/testResources/testDir, [], unique1, 1, itDoesExist]" as String)
        stdout.contains("[[string1:value, string2:value, integer1:0, integer2:0, boolean1:true, boolean2:true], string1, 25, false, [], [], [], [], [], itDoesExist]")
        stdout.contains("[[string1:dependentRequired, string2:dependentRequired, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, [], [], [], unique2, 1, itDoesExist]")
        stdout.contains("[[string1:extraField, string2:extraField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${getRootString()}/src/testResources/test.txt, ${getRootString()}/src/testResources/testDir, [], unique3, 1, itDoesExist]" as String)
    }

    def 'extra field - fail' () {
        given:
        def SCRIPT = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            params.input = "src/testResources/extraFields.csv"
            params.schema = "src/testResources/schema_input.json"

            workflow {
                Channel.fromList(samplesheetToList(params.input, params.schema))
                    .view()
            }
        '''

        when:
        def config = [
            "validation": [
                "monochromeLogs": true,
                "logging": [
                    "unrecognisedHeaders": "error"
                ]
            ]
        ]
        new MockScriptRunner(config).setScript(SCRIPT).execute()

        then:
        def error = thrown(SchemaValidationException)
        error.message == """Found the following unidentified headers in ${getRootString()}/src/testResources/extraFields.csv:
\t- extraField
""" as String
    }

    def 'no meta' () {
        given:
        def SCRIPT_TEXT = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            params.input = "src/testResources/no_meta.csv"
            params.schema = "src/testResources/no_meta_schema.json"

            workflow {
                Channel.fromList(samplesheetToList(params.input, params.schema))
                    .view()
            }
        '''

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.startsWith('[') ? it : null }

        then:
        noExceptionThrown()
        stdout.contains("[test1, test2]")
    }

    def 'deeply nested samplesheet - YAML' () {
        given:
        def SCRIPT_TEXT = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            params.input = "src/testResources/deeply_nested.yaml"
            params.schema = "src/testResources/samplesheet_schema_deeply_nested.json"

            workflow {
                Channel.fromList(samplesheetToList(params.input, params.schema))
                    .view()
            }
        '''

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.startsWith('[') ? it : null }

        then:
        noExceptionThrown()
        stdout.contains("[[mapMeta:this is in a map, arrayMeta:[metaString45, metaString478], otherArrayMeta:[metaString45, metaString478], meta:metaValue, metaMap:[entry1:entry1String, entry2:12.56]], [[string1, string2], string3, 1, 1, ${getRootString()}/file1.txt], [string4, string5, string6], [[string7, string8], [string9, string10]], test]" as String)
    }

    def 'deeply nested samplesheet - JSON' () {
        given:
        def SCRIPT_TEXT = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            params.input = "src/testResources/deeply_nested.json"
            params.schema = "src/testResources/samplesheet_schema_deeply_nested.json"

            workflow {
                Channel.fromList(samplesheetToList(params.input, params.schema))
                    .view()
            }
        '''

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.startsWith('[') ? it : null }

        then:
        noExceptionThrown()
        stdout.contains("[[mapMeta:this is in a map, arrayMeta:[metaString45, metaString478], otherArrayMeta:[metaString45, metaString478], meta:metaValue, metaMap:[entry1:entry1String, entry2:12.56]], [[string1, string2], string3, 1, 1, ${getRootString()}/file1.txt], [string4, string5, string6], [[string7, string8], [string9, string10]], test]" as String)
    }

    def 'samplesheetToList - String, String' () {
        given:
        def SCRIPT_TEXT = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            println(samplesheetToList("src/testResources/correct.csv", "src/testResources/schema_input.json").join("\\n"))
        '''

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.startsWith('[[') ? it : null }

        then:
        noExceptionThrown()
        stdout.contains("[[string1:fullField, string2:fullField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25.12, false, ${getRootString()}/src/testResources/test.txt, ${getRootString()}/src/testResources/testDir, ${getRootString()}/src/testResources/test.txt, unique1, 1, itDoesExist]" as String)
        stdout.contains("[[string1:value, string2:value, integer1:0, integer2:0, boolean1:true, boolean2:true], string1, 25.08, false, [], [], [], [], [], itDoesExist]")
        stdout.contains("[[string1:dependentRequired, string2:dependentRequired, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, [], [], [], unique2, 1, itDoesExist]")
        stdout.contains("[[string1:extraField, string2:extraField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${getRootString()}/src/testResources/test.txt, ${getRootString()}/src/testResources/testDir, ${getRootString()}/src/testResources/testDir, unique3, 1, itDoesExist]" as String)
    }

    def 'samplesheetToList - Path, String' () {
        given:
        def SCRIPT_TEXT = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            println(samplesheetToList(file("src/testResources/correct.csv", checkIfExists:true), "src/testResources/schema_input.json").join("\\n"))
        '''

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.startsWith('[[') ? it : null }

        then:
        noExceptionThrown()
        stdout.contains("[[string1:fullField, string2:fullField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25.12, false, ${getRootString()}/src/testResources/test.txt, ${getRootString()}/src/testResources/testDir, ${getRootString()}/src/testResources/test.txt, unique1, 1, itDoesExist]" as String)
        stdout.contains("[[string1:value, string2:value, integer1:0, integer2:0, boolean1:true, boolean2:true], string1, 25.08, false, [], [], [], [], [], itDoesExist]")
        stdout.contains("[[string1:dependentRequired, string2:dependentRequired, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, [], [], [], unique2, 1, itDoesExist]")
        stdout.contains("[[string1:extraField, string2:extraField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${getRootString()}/src/testResources/test.txt, ${getRootString()}/src/testResources/testDir, ${getRootString()}/src/testResources/testDir, unique3, 1, itDoesExist]" as String)
    }

    def 'samplesheetToList - String, Path' () {
        given:
        def SCRIPT_TEXT = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            println(samplesheetToList("src/testResources/correct.csv", file("src/testResources/schema_input.json", checkIfExists:true)).join("\\n"))
        '''

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.startsWith('[[') ? it : null }

        then:
        noExceptionThrown()
        stdout.contains("[[string1:fullField, string2:fullField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25.12, false, ${getRootString()}/src/testResources/test.txt, ${getRootString()}/src/testResources/testDir, ${getRootString()}/src/testResources/test.txt, unique1, 1, itDoesExist]" as String)
        stdout.contains("[[string1:value, string2:value, integer1:0, integer2:0, boolean1:true, boolean2:true], string1, 25.08, false, [], [], [], [], [], itDoesExist]")
        stdout.contains("[[string1:dependentRequired, string2:dependentRequired, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, [], [], [], unique2, 1, itDoesExist]")
        stdout.contains("[[string1:extraField, string2:extraField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${getRootString()}/src/testResources/test.txt, ${getRootString()}/src/testResources/testDir, ${getRootString()}/src/testResources/testDir, unique3, 1, itDoesExist]" as String)
    }

    def 'samplesheetToList - Path, Path' () {
        given:
        def SCRIPT_TEXT = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            println(samplesheetToList(file("src/testResources/correct.csv", checkIfExists:true), file("src/testResources/schema_input.json", checkIfExists:true)).join("\\n"))
        '''

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.startsWith('[[') ? it : null }

        then:
        noExceptionThrown()
        stdout.contains("[[string1:fullField, string2:fullField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25.12, false, ${getRootString()}/src/testResources/test.txt, ${getRootString()}/src/testResources/testDir, ${getRootString()}/src/testResources/test.txt, unique1, 1, itDoesExist]" as String)
        stdout.contains("[[string1:value, string2:value, integer1:0, integer2:0, boolean1:true, boolean2:true], string1, 25.08, false, [], [], [], [], [], itDoesExist]")
        stdout.contains("[[string1:dependentRequired, string2:dependentRequired, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, [], [], [], unique2, 1, itDoesExist]")
        stdout.contains("[[string1:extraField, string2:extraField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${getRootString()}/src/testResources/test.txt, ${getRootString()}/src/testResources/testDir, ${getRootString()}/src/testResources/testDir, unique3, 1, itDoesExist]" as String)
    }

    def 'samplesheetToList - usage in channels' () {
        given:
        def SCRIPT_TEXT = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            Channel.of("src/testResources/correct.csv")
                .flatMap { it -> 
                    samplesheetToList(it, "src/testResources/schema_input.json")
                }
                .map { it -> println("first: ${it}") }

            Channel.of("src/testResources/correct_arrays.json")
                .flatMap { it -> 
                    samplesheetToList(it, "src/testResources/schema_input_with_arrays.json")
                }
                .map { it -> println("second: ${it}") }

        '''

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.startsWith('first') || it.startsWith('second') ? it : null }

        then:
        noExceptionThrown()
        stdout.contains("first: [[string1:fullField, string2:fullField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25.12, false, ${getRootString()}/src/testResources/test.txt, ${getRootString()}/src/testResources/testDir, ${getRootString()}/src/testResources/test.txt, unique1, 1, itDoesExist]" as String)
        stdout.contains("first: [[string1:value, string2:value, integer1:0, integer2:0, boolean1:true, boolean2:true], string1, 25.08, false, [], [], [], [], [], itDoesExist]")
        stdout.contains("first: [[string1:dependentRequired, string2:dependentRequired, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, [], [], [], unique2, 1, itDoesExist]")
        stdout.contains("first: [[string1:extraField, string2:extraField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${getRootString()}/src/testResources/test.txt, ${getRootString()}/src/testResources/testDir, ${getRootString()}/src/testResources/testDir, unique3, 1, itDoesExist]" as String)
        stdout.contains("second: [[array_meta:[]], [${getRootString()}/src/testResources/testDir/testFile.txt, ${getRootString()}/src/testResources/testDir2/testFile2.txt], [${getRootString()}/src/testResources/testDir, ${getRootString()}/src/testResources/testDir2], [${getRootString()}/src/testResources/testDir, ${getRootString()}/src/testResources/testDir2/testFile2.txt], [string1, string2], [25, 26], [25, 26.5], [false, true], [1, 2, 3], [true], [${getRootString()}/src/testResources/testDir/testFile.txt], [[${getRootString()}/src/testResources/testDir/testFile.txt]]]" as String)
        stdout.contains("second: [[array_meta:[look, an, array, in, meta]], [], [], [], [string1, string2], [25, 26], [25, 26.5], [], [1, 2, 3], [false, true, false], [${getRootString()}/src/testResources/testDir/testFile.txt], [[${getRootString()}/src/testResources/testDir/testFile.txt]]]" as String)
        stdout.contains("second: [[array_meta:[]], [], [], [], [string1, string2], [25, 26], [25, 26.5], [], [1, 2, 3], [false, true, false], [${getRootString()}/src/testResources/testDir/testFile.txt], [[${getRootString()}/src/testResources/testDir/testFile.txt], [${getRootString()}/src/testResources/testDir/testFile.txt, ${getRootString()}/src/testResources/testDir2/testFile2.txt]]]" as String)

    }

    def 'samplesheetToList - nested schema with oneOf/anyOf/allOf' () {
        given:
        def SCRIPT_TEXT = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            workflow {
                Channel.fromList(samplesheetToList("src/testResources/deeply_nested.yaml", "src/testResources/samplesheet_schema_deeply_nested_anyof.json")).view()       
            }

        '''

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.startsWith('[') ? it : null }

        then:
        noExceptionThrown()
        stdout.contains("[[mapMeta:this is in a map, arrayMeta:[metaString45, metaString478], otherArrayMeta:[metaString45, metaString478], meta:metaValue, metaMap:[entry1:entry1String, entry2:12.56]], [[string1, string2], string3, 1, 1, ${getRootString()}/file1.txt], [string4, string5, string6], [[string7, string8], [string9, string10]], test]" as String)

    }

    def 'samplesheetToList - correctly sanitize empty header columns CSV' () {
        given:
        def SCRIPT_TEXT = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            workflow {
                Channel.fromList(samplesheetToList("src/testResources/samplesheet_empty_header_column.csv", "src/testResources/no_meta_schema.json")).view()       
            }

        '''

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.startsWith('[') ? it : null }

        then:
        noExceptionThrown()
        stdout.contains("[file1.txt, file2.txt]")

    }

    def 'samplesheetToList - correctly sanitize empty header columns TSV' () {
        given:
        def SCRIPT_TEXT = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            workflow {
                Channel.fromList(samplesheetToList("src/testResources/samplesheet_empty_header_column.tsv", "src/testResources/no_meta_schema.json")).view()       
            }

        '''

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.startsWith('[') ? it : null }

        then:
        noExceptionThrown()
        stdout.contains("[file1.txt, file2.txt]")

    }

    def 'samplesheetToList - correctly set defaults' () {
        given:
        def SCRIPT_TEXT = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            workflow {
                Channel.fromList(samplesheetToList("src/testResources/samplesheet_defaults.yaml", "src/testResources/schema_input_defaults.json")).view()       
            }

        '''

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.startsWith('[') ? it : null }

        then:
        noExceptionThrown()
        stdout.contains("[[nullValue:null], 25, defaultString, true, test]")
        stdout.contains("[[nullValue:null], 0, defaultString, true, null]")
    }

    def 'should work fine - CSV from URL' () {
        given:
        def SCRIPT_TEXT = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            params.input = "https://github.com/nextflow-io/nf-schema/raw/refs/heads/master/src/testResources/correct.csv"
            params.schema = "src/testResources/schema_input.json"

            workflow {
                Channel.fromList(samplesheetToList(params.input, params.schema))
                    .view()
            }
        '''

        when:
        dsl_eval(SCRIPT_TEXT)
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.startsWith('[[') ? it : null }

        then:
        noExceptionThrown()
        stdout.contains("[[string1:fullField, string2:fullField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25.12, false, ${getRootString()}/src/testResources/test.txt, ${getRootString()}/src/testResources/testDir, ${getRootString()}/src/testResources/test.txt, unique1, 1, itDoesExist]" as String)
        stdout.contains("[[string1:value, string2:value, integer1:0, integer2:0, boolean1:true, boolean2:true], string1, 25.08, false, [], [], [], [], [], itDoesExist]")
        stdout.contains("[[string1:dependentRequired, string2:dependentRequired, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, [], [], [], unique2, 1, itDoesExist]")
        stdout.contains("[[string1:extraField, string2:extraField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${getRootString()}/src/testResources/test.txt, ${getRootString()}/src/testResources/testDir, ${getRootString()}/src/testResources/testDir, unique3, 1, itDoesExist]" as String)
    }
    
}
