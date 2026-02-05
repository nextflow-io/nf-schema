/* groovylint-disable LineLength, MethodName, TrailingWhitespace, UnnecessaryObjectReferences */
package nextflow.validation

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
import test.MockScriptRunner

import nextflow.validation.exceptions.SchemaValidationException

import java.nio.file.Files
import java.util.jar.Manifest

/**
 * @author : mirpedrol <mirp.julia@gmail.com>
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */

@CompileDynamic
class SamplesheetConverterTest extends Dsl2Spec {

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

    void 'should work fine - CSV'() {
        given:
        String scriptText = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            params.input = "src/testResources/correct.csv"
            params.schema = "src/testResources/schema_input.json"

            workflow {
                Channel.fromList(samplesheetToList(params.input, params.schema))
                    .view()
            }
        '''

        when:
        dsl_eval(scriptText)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.startsWith('[[') ? line : null }

        then:
        noExceptionThrown()
        stdout.contains("[[string1:fullField, string2:fullField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25.12, false, ${rootString}/src/testResources/test.txt, ${rootString}/src/testResources/testDir, ${rootString}/src/testResources/test.txt, unique1, 1, itDoesExist]" as String)
        stdout.contains('[[string1:value, string2:value, integer1:0, integer2:0, boolean1:true, boolean2:true], string1, 25.08, false, [], [], [], [], [], itDoesExist]')
        stdout.contains('[[string1:dependentRequired, string2:dependentRequired, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, [], [], [], unique2, 1, itDoesExist]')
        stdout.contains("[[string1:extraField, string2:extraField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${rootString}/src/testResources/test.txt, ${rootString}/src/testResources/testDir, ${rootString}/src/testResources/testDir, unique3, 1, itDoesExist]" as String)
    }

    void 'should work fine - quoted CSV'() {
        given:
        String scriptText = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            params.input = "src/testResources/correct_quoted.csv"
            params.schema = "src/testResources/schema_input.json"

            workflow {
                Channel.fromList(samplesheetToList(params.input, params.schema))
                    .view()
            }
        '''

        when:
        dsl_eval(scriptText)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.startsWith('[[') ? line : null }

        then:
        noExceptionThrown()
        stdout.contains("[[string1:fullField, string2:fullField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25.12, false, ${rootString}/src/testResources/test.txt, ${rootString}/src/testResources/testDir, ${rootString}/src/testResources/test.txt, unique1, 1, itDoesExist]" as String)
        stdout.contains('[[string1:value, string2:value, integer1:0, integer2:0, boolean1:true, boolean2:true], string1, 25.08, false, [], [], [], [], [], itDoesExist]')
        stdout.contains('[[string1:dependentRequired, string2:dependentRequired, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, [], [], [], unique2, 1, itDoesExist]')
        stdout.contains("[[string1:extraField, string2:extraField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${rootString}/src/testResources/test.txt, ${rootString}/src/testResources/testDir, ${rootString}/src/testResources/testDir, unique3, 1, itDoesExist]" as String)
    }

    void 'should work fine - TSV'() {
        given:
        String scriptText = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            params.input = "src/testResources/correct.tsv"
            params.schema = "src/testResources/schema_input.json"

            workflow {
                Channel.fromList(samplesheetToList(params.input, params.schema))
                    .view()
            }
        '''

        when:
        dsl_eval(scriptText)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.startsWith('[[') ? line : null }

        then:
        noExceptionThrown()
        stdout.contains("[[string1:fullField, string2:fullField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25.12, false, ${rootString}/src/testResources/test.txt, ${rootString}/src/testResources/testDir, ${rootString}/src/testResources/test.txt, unique1, 1, itDoesExist]" as String)
        stdout.contains('[[string1:value, string2:value, integer1:0, integer2:0, boolean1:true, boolean2:true], string1, 25.08, false, [], [], [], [], [], itDoesExist]')
        stdout.contains('[[string1:dependentRequired, string2:dependentRequired, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, [], [], [], unique2, 1, itDoesExist]')
        stdout.contains("[[string1:extraField, string2:extraField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${rootString}/src/testResources/test.txt, ${rootString}/src/testResources/testDir, ${rootString}/src/testResources/testDir, unique3, 1, itDoesExist]" as String)
    }

    void 'should work fine - YAML'() {
        given:
        String scriptText = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            params.input = "src/testResources/correct.yaml"
            params.schema = "src/testResources/schema_input.json"

            workflow {
                Channel.fromList(samplesheetToList(params.input, params.schema))
                    .view()
            }
        '''

        when:
        dsl_eval(scriptText)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.startsWith('[[') ? line : null }

        then:
        noExceptionThrown()
        stdout.contains("[[string1:fullField, string2:fullField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25.12, false, ${rootString}/src/testResources/test.txt, ${rootString}/src/testResources/testDir, ${rootString}/src/testResources/test.txt, unique1, 1, itDoesExist]" as String)
        stdout.contains('[[string1:value, string2:value, integer1:0, integer2:0, boolean1:true, boolean2:true], string1, 25.08, false, [], [], [], [], [], itDoesExist]')
        stdout.contains('[[string1:dependentRequired, string2:dependentRequired, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, [], [], [], unique2, 1, itDoesExist]')
        stdout.contains("[[string1:extraField, string2:extraField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${rootString}/src/testResources/test.txt, ${rootString}/src/testResources/testDir, ${rootString}/src/testResources/testDir, unique3, 1, itDoesExist]" as String)
    }

    void 'should work fine - JSON'() {
        given:
        String scriptText = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            params.input = "src/testResources/correct.json"
            params.schema = "src/testResources/schema_input.json"

            workflow {
                Channel.fromList(samplesheetToList(params.input, params.schema))
                    .view()
            }
        '''

        when:
        dsl_eval(scriptText)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.startsWith('[[') ? line : null }

        then:
        noExceptionThrown()
        stdout.contains("[[string1:fullField, string2:fullField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25.12, false, ${rootString}/src/testResources/test.txt, ${rootString}/src/testResources/testDir, ${rootString}/src/testResources/test.txt, unique1, 1, itDoesExist]" as String)
        stdout.contains('[[string1:value, string2:value, integer1:0, integer2:0, boolean1:true, boolean2:true], string1, 25.08, false, [], [], [], [], [], itDoesExist]')
        stdout.contains('[[string1:dependentRequired, string2:dependentRequired, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, [], [], [], unique2, 1, itDoesExist]')
        stdout.contains("[[string1:extraField, string2:extraField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${rootString}/src/testResources/test.txt, ${rootString}/src/testResources/testDir, ${rootString}/src/testResources/testDir, unique3, 1, itDoesExist]" as String)
    }

    void 'arrays should work fine - YAML'() {
        given:
        String scriptText = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            params.input = "src/testResources/correct_arrays.yaml"
            params.schema = "src/testResources/schema_input_with_arrays.json"

            workflow {
                Channel.fromList(samplesheetToList(params.input, params.schema))
                    .view()
            }
        '''

        when:
        dsl_eval(scriptText)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.startsWith('[[') ? line : null }

        then:
        noExceptionThrown()
        stdout.contains("[[array_meta:[]], [${rootString}/src/testResources/testDir/testFile.txt, ${rootString}/src/testResources/testDir2/testFile2.txt], [${rootString}/src/testResources/testDir, ${rootString}/src/testResources/testDir2], [${rootString}/src/testResources/testDir, ${rootString}/src/testResources/testDir2/testFile2.txt], [string1, string2], [25, 26], [25, 26.5], [false, true], [1, 2, 3], [true], [${rootString}/src/testResources/testDir/testFile.txt], [[${rootString}/src/testResources/testDir/testFile.txt]]]" as String)
        stdout.contains("[[array_meta:[look, an, array, in, meta]], [], [], [], [string1, string2], [25, 26], [25, 26.5], [], [1, 2, 3], [false, true, false], [${rootString}/src/testResources/testDir/testFile.txt], [[${rootString}/src/testResources/testDir/testFile.txt]]]" as String)
        stdout.contains("[[array_meta:[]], [], [], [], [string1, string2], [25, 26], [25, 26.5], [], [1, 2, 3], [false, true, false], [${rootString}/src/testResources/testDir/testFile.txt], [[${rootString}/src/testResources/testDir/testFile.txt], [${rootString}/src/testResources/testDir/testFile.txt, ${rootString}/src/testResources/testDir2/testFile2.txt]]]" as String)
    }

    void 'arrays should work fine - JSON'() {
        given:
        String scriptText = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            params.input = "src/testResources/correct_arrays.json"
            params.schema = "src/testResources/schema_input_with_arrays.json"

            workflow {
                Channel.fromList(samplesheetToList(params.input, params.schema))
                    .view()
            }
        '''

        when:
        dsl_eval(scriptText)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.startsWith('[[') ? line : null }

        then:
        noExceptionThrown()
        stdout.contains("[[array_meta:[]], [${rootString}/src/testResources/testDir/testFile.txt, ${rootString}/src/testResources/testDir2/testFile2.txt], [${rootString}/src/testResources/testDir, ${rootString}/src/testResources/testDir2], [${rootString}/src/testResources/testDir, ${rootString}/src/testResources/testDir2/testFile2.txt], [string1, string2], [25, 26], [25, 26.5], [false, true], [1, 2, 3], [true], [${rootString}/src/testResources/testDir/testFile.txt], [[${rootString}/src/testResources/testDir/testFile.txt]]]" as String)
        stdout.contains("[[array_meta:[look, an, array, in, meta]], [], [], [], [string1, string2], [25, 26], [25, 26.5], [], [1, 2, 3], [false, true, false], [${rootString}/src/testResources/testDir/testFile.txt], [[${rootString}/src/testResources/testDir/testFile.txt]]]" as String)
        stdout.contains("[[array_meta:[]], [], [], [], [string1, string2], [25, 26], [25, 26.5], [], [1, 2, 3], [false, true, false], [${rootString}/src/testResources/testDir/testFile.txt], [[${rootString}/src/testResources/testDir/testFile.txt], [${rootString}/src/testResources/testDir/testFile.txt, ${rootString}/src/testResources/testDir2/testFile2.txt]]]" as String)
    }

    void 'no header - CSV'() {
        given:
        String scriptText = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            params.input = "src/testResources/no_header.csv"
            params.schema = "src/testResources/no_header_schema.json"

            workflow {
                Channel.fromList(samplesheetToList(params.input, params.schema))
                    .view()
            }
        '''

        when:
        dsl_eval(scriptText)
        List<String> stdout = capture
                .toString()
                .readLines()

        then:
        noExceptionThrown()
        stdout.contains('test_1')
        stdout.contains('test_2')
    }

    void 'no header - YAML'() {
        given:
        String scriptText = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            params.input = "src/testResources/no_header.yaml"
            params.schema = "src/testResources/no_header_schema.json"

            workflow {
                Channel.fromList(samplesheetToList(params.input, params.schema))
                    .view()
            }
        '''

        when:
        dsl_eval(scriptText)
        List<String> stdout = capture
                .toString()
                .readLines()

        then:
        noExceptionThrown()
        stdout.contains('test_1')
        stdout.contains('test_2')
    }

    void 'no header - JSON'() {
        given:
        String scriptText = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            params.input = "src/testResources/no_header.json"
            params.schema = "src/testResources/no_header_schema.json"

            workflow {
                Channel.fromList(samplesheetToList(params.input, params.schema))
                    .view()
            }
        '''

        when:
        dsl_eval(scriptText)
        List<String> stdout = capture
                .toString()
                .readLines()

        then:
        noExceptionThrown()
        stdout.contains('test_1')
        stdout.contains('test_2')
    }

    void 'extra field'() {
        given:
        String script = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            params.input = "src/testResources/extraFields.csv"
            params.schema = "src/testResources/schema_input.json"

            workflow {
                Channel.fromList(samplesheetToList(params.input, params.schema))
                    .view()
            }
        '''

        when:
        Map config = [
            'validation': [
                'logging': [
                    'unrecognisedHeaders': 'warn'
                ]
            ]
        ]
        new MockScriptRunner(config).setScript(script).execute()
        List<String> stdout = capture
                .toString()
                .readLines()
                .collect { line ->
                    line.split('ValidationLogger -- ')[-1]
                }

        then:
        noExceptionThrown()
        stdout.contains("Found the following unidentified headers in ${rootString}/src/testResources/extraFields.csv:" as String)
        stdout.contains('\t- extraField')
        stdout.contains("[[string1:fullField, string2:fullField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${rootString}/src/testResources/test.txt, ${rootString}/src/testResources/testDir, [], unique1, 1, itDoesExist]" as String)
        stdout.contains('[[string1:value, string2:value, integer1:0, integer2:0, boolean1:true, boolean2:true], string1, 25, false, [], [], [], [], [], itDoesExist]')
        stdout.contains('[[string1:dependentRequired, string2:dependentRequired, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, [], [], [], unique2, 1, itDoesExist]')
        stdout.contains("[[string1:extraField, string2:extraField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${rootString}/src/testResources/test.txt, ${rootString}/src/testResources/testDir, [], unique3, 1, itDoesExist]" as String)
    }

    void 'extra field - fail'() {
        given:
        String script = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            params.input = "src/testResources/extraFields.csv"
            params.schema = "src/testResources/schema_input.json"

            workflow {
                Channel.fromList(samplesheetToList(params.input, params.schema))
                    .view()
            }
        '''

        when:
        Map config = [
            'validation': [
                'monochromeLogs': true,
                'logging': [
                    'unrecognisedHeaders': 'error'
                ]
            ]
        ]
        new MockScriptRunner(config).setScript(script).execute()

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message == "Found the following unidentified headers in ${rootString}/src/testResources/extraFields.csv:\n\t- extraField\n"
    }

    void 'no meta'() {
        given:
        String scriptText = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            params.input = "src/testResources/no_meta.csv"
            params.schema = "src/testResources/no_meta_schema.json"

            workflow {
                Channel.fromList(samplesheetToList(params.input, params.schema))
                    .view()
            }
        '''

        when:
        dsl_eval(scriptText)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.startsWith('[') ? line : null }

        then:
        noExceptionThrown()
        stdout.contains('[test1, test2]')
    }

    void 'deeply nested samplesheet - YAML'() {
        given:
        String scriptText = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            params.input = "src/testResources/deeply_nested.yaml"
            params.schema = "src/testResources/samplesheet_schema_deeply_nested.json"

            workflow {
                Channel.fromList(samplesheetToList(params.input, params.schema))
                    .view()
            }
        '''

        when:
        dsl_eval(scriptText)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.startsWith('[') ? line : null }

        then:
        noExceptionThrown()
        stdout.contains("[[mapMeta:this is in a map, arrayMeta:[metaString45, metaString478], otherArrayMeta:[metaString45, metaString478], meta:metaValue, metaMap:[entry1:entry1String, entry2:12.56]], [[string1, string2], string3, 1, 1, ${rootString}/file1.txt], [string4, string5, string6], [[string7, string8], [string9, string10]], test]" as String)
    }

    void 'deeply nested samplesheet - JSON'() {
        given:
        String scriptText = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            params.input = "src/testResources/deeply_nested.json"
            params.schema = "src/testResources/samplesheet_schema_deeply_nested.json"

            workflow {
                Channel.fromList(samplesheetToList(params.input, params.schema))
                    .view()
            }
        '''

        when:
        dsl_eval(scriptText)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.startsWith('[') ? line : null }

        then:
        noExceptionThrown()
        stdout.contains("[[mapMeta:this is in a map, arrayMeta:[metaString45, metaString478], otherArrayMeta:[metaString45, metaString478], meta:metaValue, metaMap:[entry1:entry1String, entry2:12.56]], [[string1, string2], string3, 1, 1, ${rootString}/file1.txt], [string4, string5, string6], [[string7, string8], [string9, string10]], test]" as String)
    }

    void 'samplesheetToList - String, String'() {
        given:
        String scriptText = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            println(samplesheetToList("src/testResources/correct.csv", "src/testResources/schema_input.json").join("\\n"))
        '''

        when:
        dsl_eval(scriptText)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.startsWith('[[') ? line : null }

        then:
        noExceptionThrown()
        stdout.contains("[[string1:fullField, string2:fullField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25.12, false, ${rootString}/src/testResources/test.txt, ${rootString}/src/testResources/testDir, ${rootString}/src/testResources/test.txt, unique1, 1, itDoesExist]" as String)
        stdout.contains('[[string1:value, string2:value, integer1:0, integer2:0, boolean1:true, boolean2:true], string1, 25.08, false, [], [], [], [], [], itDoesExist]')
        stdout.contains('[[string1:dependentRequired, string2:dependentRequired, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, [], [], [], unique2, 1, itDoesExist]')
        stdout.contains("[[string1:extraField, string2:extraField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${rootString}/src/testResources/test.txt, ${rootString}/src/testResources/testDir, ${rootString}/src/testResources/testDir, unique3, 1, itDoesExist]" as String)
    }

    void 'samplesheetToList - Path, String'() {
        given:
        String scriptText = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            println(samplesheetToList(file("src/testResources/correct.csv", checkIfExists:true), "src/testResources/schema_input.json").join("\\n"))
        '''

        when:
        dsl_eval(scriptText)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.startsWith('[[') ? line : null }

        then:
        noExceptionThrown()
        stdout.contains("[[string1:fullField, string2:fullField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25.12, false, ${rootString}/src/testResources/test.txt, ${rootString}/src/testResources/testDir, ${rootString}/src/testResources/test.txt, unique1, 1, itDoesExist]" as String)
        stdout.contains('[[string1:value, string2:value, integer1:0, integer2:0, boolean1:true, boolean2:true], string1, 25.08, false, [], [], [], [], [], itDoesExist]')
        stdout.contains('[[string1:dependentRequired, string2:dependentRequired, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, [], [], [], unique2, 1, itDoesExist]')
        stdout.contains("[[string1:extraField, string2:extraField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${rootString}/src/testResources/test.txt, ${rootString}/src/testResources/testDir, ${rootString}/src/testResources/testDir, unique3, 1, itDoesExist]" as String)
    }

    void 'samplesheetToList - String, Path'() {
        given:
        String scriptText = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            println(samplesheetToList("src/testResources/correct.csv", file("src/testResources/schema_input.json", checkIfExists:true)).join("\\n"))
        '''

        when:
        dsl_eval(scriptText)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.startsWith('[[') ? line : null }

        then:
        noExceptionThrown()
        stdout.contains("[[string1:fullField, string2:fullField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25.12, false, ${rootString}/src/testResources/test.txt, ${rootString}/src/testResources/testDir, ${rootString}/src/testResources/test.txt, unique1, 1, itDoesExist]" as String)
        stdout.contains('[[string1:value, string2:value, integer1:0, integer2:0, boolean1:true, boolean2:true], string1, 25.08, false, [], [], [], [], [], itDoesExist]')
        stdout.contains('[[string1:dependentRequired, string2:dependentRequired, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, [], [], [], unique2, 1, itDoesExist]')
        stdout.contains("[[string1:extraField, string2:extraField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${rootString}/src/testResources/test.txt, ${rootString}/src/testResources/testDir, ${rootString}/src/testResources/testDir, unique3, 1, itDoesExist]" as String)
    }

    void 'samplesheetToList - Path, Path'() {
        given:
        String scriptText = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            println(samplesheetToList(file("src/testResources/correct.csv", checkIfExists:true), file("src/testResources/schema_input.json", checkIfExists:true)).join("\\n"))
        '''

        when:
        dsl_eval(scriptText)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.startsWith('[[') ? line : null }

        then:
        noExceptionThrown()
        stdout.contains("[[string1:fullField, string2:fullField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25.12, false, ${rootString}/src/testResources/test.txt, ${rootString}/src/testResources/testDir, ${rootString}/src/testResources/test.txt, unique1, 1, itDoesExist]" as String)
        stdout.contains('[[string1:value, string2:value, integer1:0, integer2:0, boolean1:true, boolean2:true], string1, 25.08, false, [], [], [], [], [], itDoesExist]')
        stdout.contains('[[string1:dependentRequired, string2:dependentRequired, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, [], [], [], unique2, 1, itDoesExist]')
        stdout.contains("[[string1:extraField, string2:extraField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${rootString}/src/testResources/test.txt, ${rootString}/src/testResources/testDir, ${rootString}/src/testResources/testDir, unique3, 1, itDoesExist]" as String)
    }

    void 'samplesheetToList - usage in channels'() {
        given:
        /* groovylint-disable-next-line GStringExpressionWithinString */
        String scriptText = '''
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
        dsl_eval(scriptText)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.startsWith('first') || line.startsWith('second') ? line : null }

        then:
        noExceptionThrown()
        stdout.contains("first: [[string1:fullField, string2:fullField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25.12, false, ${rootString}/src/testResources/test.txt, ${rootString}/src/testResources/testDir, ${rootString}/src/testResources/test.txt, unique1, 1, itDoesExist]" as String)
        stdout.contains('first: [[string1:value, string2:value, integer1:0, integer2:0, boolean1:true, boolean2:true], string1, 25.08, false, [], [], [], [], [], itDoesExist]')
        stdout.contains('first: [[string1:dependentRequired, string2:dependentRequired, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, [], [], [], unique2, 1, itDoesExist]')
        stdout.contains("first: [[string1:extraField, string2:extraField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${rootString}/src/testResources/test.txt, ${rootString}/src/testResources/testDir, ${rootString}/src/testResources/testDir, unique3, 1, itDoesExist]" as String)
        stdout.contains("second: [[array_meta:[]], [${rootString}/src/testResources/testDir/testFile.txt, ${rootString}/src/testResources/testDir2/testFile2.txt], [${rootString}/src/testResources/testDir, ${rootString}/src/testResources/testDir2], [${rootString}/src/testResources/testDir, ${rootString}/src/testResources/testDir2/testFile2.txt], [string1, string2], [25, 26], [25, 26.5], [false, true], [1, 2, 3], [true], [${rootString}/src/testResources/testDir/testFile.txt], [[${rootString}/src/testResources/testDir/testFile.txt]]]" as String)
        stdout.contains("second: [[array_meta:[look, an, array, in, meta]], [], [], [], [string1, string2], [25, 26], [25, 26.5], [], [1, 2, 3], [false, true, false], [${rootString}/src/testResources/testDir/testFile.txt], [[${rootString}/src/testResources/testDir/testFile.txt]]]" as String)
        stdout.contains("second: [[array_meta:[]], [], [], [], [string1, string2], [25, 26], [25, 26.5], [], [1, 2, 3], [false, true, false], [${rootString}/src/testResources/testDir/testFile.txt], [[${rootString}/src/testResources/testDir/testFile.txt], [${rootString}/src/testResources/testDir/testFile.txt, ${rootString}/src/testResources/testDir2/testFile2.txt]]]" as String)
    }

    void 'samplesheetToList - nested schema with oneOf/anyOf/allOf'() {
        given:
        String scriptText = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            workflow {
                Channel.fromList(samplesheetToList("src/testResources/deeply_nested.yaml", "src/testResources/samplesheet_schema_deeply_nested_anyof.json")).view()
            }

        '''

        when:
        dsl_eval(scriptText)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.startsWith('[') ? line : null }

        then:
        noExceptionThrown()
        stdout.contains("[[mapMeta:this is in a map, arrayMeta:[metaString45, metaString478], otherArrayMeta:[metaString45, metaString478], meta:metaValue, metaMap:[entry1:entry1String, entry2:12.56]], [[string1, string2], string3, 1, 1, ${rootString}/file1.txt], [string4, string5, string6], [[string7, string8], [string9, string10]], test]" as String)
    }

    void 'samplesheetToList - correctly sanitize empty header columns CSV'() {
        given:
        String scriptText = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            workflow {
                Channel.fromList(samplesheetToList("src/testResources/samplesheet_empty_header_column.csv", "src/testResources/no_meta_schema.json")).view()
            }

        '''

        when:
        dsl_eval(scriptText)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.startsWith('[') ? line : null }

        then:
        noExceptionThrown()
        stdout.contains('[file1.txt, file2.txt]')
    }

    void 'samplesheetToList - correctly sanitize empty header columns TSV'() {
        given:
        String scriptText = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            workflow {
                Channel.fromList(samplesheetToList("src/testResources/samplesheet_empty_header_column.tsv", "src/testResources/no_meta_schema.json")).view()
            }

        '''

        when:
        dsl_eval(scriptText)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.startsWith('[') ? line : null }

        then:
        noExceptionThrown()
        stdout.contains('[file1.txt, file2.txt]')
    }

    void 'samplesheetToList - correctly set defaults'() {
        given:
        String scriptText = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            workflow {
                Channel.fromList(samplesheetToList("src/testResources/samplesheet_defaults.yaml", "src/testResources/schema_input_defaults.json")).view()
            }

        '''

        when:
        dsl_eval(scriptText)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.startsWith('[') ? line : null }

        then:
        noExceptionThrown()
        stdout.contains('[[nullValue:null], 25, defaultString, true, test]')
        stdout.contains('[[nullValue:null], 0, defaultString, true, null]')
    }

    void 'should work fine - CSV from URL'() {
        given:
        String scriptText = '''
            include { samplesheetToList } from 'plugin/nf-schema'

            params.input = "https://github.com/nextflow-io/nf-schema/raw/refs/heads/master/src/testResources/correct.csv"
            params.schema = "src/testResources/schema_input.json"

            workflow {
                Channel.fromList(samplesheetToList(params.input, params.schema))
                    .view()
            }
        '''

        when:
        dsl_eval(scriptText)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.startsWith('[[') ? line : null }

        then:
        noExceptionThrown()
        stdout.contains("[[string1:fullField, string2:fullField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25.12, false, ${rootString}/src/testResources/test.txt, ${rootString}/src/testResources/testDir, ${rootString}/src/testResources/test.txt, unique1, 1, itDoesExist]" as String)
        stdout.contains('[[string1:value, string2:value, integer1:0, integer2:0, boolean1:true, boolean2:true], string1, 25.08, false, [], [], [], [], [], itDoesExist]')
        stdout.contains('[[string1:dependentRequired, string2:dependentRequired, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, [], [], [], unique2, 1, itDoesExist]')
        stdout.contains("[[string1:extraField, string2:extraField, integer1:10, integer2:10, boolean1:true, boolean2:true], string1, 25, false, ${rootString}/src/testResources/test.txt, ${rootString}/src/testResources/testDir, ${rootString}/src/testResources/testDir, unique3, 1, itDoesExist]" as String)
    }

    private String getRootString() { return this.root.toString() }

}
