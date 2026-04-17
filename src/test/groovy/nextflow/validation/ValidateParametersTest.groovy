/* groovylint-disable ClassSize, LineLength, MethodCount, MethodName, TrailingWhitespace */
// TODO split this test class up
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
 * @author : mirpedrol <mirp.julia@gmail.com>
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 * @author : jorgeaguileraseqera
 */

@CompileDynamic
class ValidateParametersTest extends Dsl2Spec {

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

    void 'should validate when no params'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath()
        String script = """
            include { validateParameters } from 'plugin/nf-schema'
            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = ['validation': [
            'monochromeLogs': true
        ]]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message == '''The following invalid input values have been detected:

* Missing required parameter(s): input, outdir

'''
        !stdout
    }

    void 'should validate a schema with no arguments'() {
        given:
        File schemaSource = new File('src/testResources/nextflow_schema.json')
        File schemaDest   = new File('nextflow_schema.json')
        schemaDest << schemaSource.text

        String script = '''
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            include { validateParameters } from 'plugin/nf-schema'
            workflow {
                validateParameters()
            }
        '''

        when:
        Map config = [:]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        noExceptionThrown()
        !stdout

        cleanup:
        schemaDest.delete()
    }

    void 'should validate a schema - CSV'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath()
        String script = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            include { validateParameters } from 'plugin/nf-schema'
            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = [:]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        noExceptionThrown()
        !stdout
    }

    void 'should validate a schema - TSV'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath()
        String script = """
            params.input = 'src/testResources/correct.tsv'
            params.outdir = 'src/testResources/testDir'
            include { validateParameters } from 'plugin/nf-schema'
            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = [:]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        noExceptionThrown()
        !stdout
    }

    void 'should validate a schema - YAML'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath()
        String script = """
            params.input = 'src/testResources/correct.yaml'
            params.outdir = 'src/testResources/testDir'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = [:]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        noExceptionThrown()
        !stdout
    }

    void 'should validate a schema - JSON'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath()
        String script = """
            params.input = 'src/testResources/correct.json'
            params.outdir = 'src/testResources/testDir'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = [:]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        noExceptionThrown()
        !stdout
    }

    void 'should validate a schema with failures - CSV'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema_with_samplesheet.json').toAbsolutePath()
        String script = """
            params.input = 'src/testResources/wrong.csv'
            params.outdir = 'src/testResources/testDir'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = [:]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        List<String> errorMessages = error.message.readLines()
        errorMessages[0] == 'The following invalid input values have been detected:'
        errorMessages[1] == ''
        errorMessages[2] == '* --input (src/testResources/wrong.csv): Validation of file failed:'
        errorMessages[3] == "\t-> Entry 1: Error for field 'strandedness' (weird): Expected any of [[forward, reverse, unstranded]] (Strandedness must be provided and be one of 'forward', 'reverse' or 'unstranded')"
        errorMessages[4] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): \"test1_fastq2.fasta\" does not match regular expression [^\\S+\\.f(ast)?q\\.gz\$]"
        errorMessages[5] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): \"test1_fastq2.fasta\" is longer than 0 characters"
        errorMessages[6] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): Value does not match against any of the schemas (FastQ file for reads 2 cannot contain spaces and must have extension '.fq.gz' or '.fastq.gz')"
        errorMessages[7] == '\t-> Entry 1: Missing required field(s): sample'
        errorMessages[8] == "\t-> Entry 2: Error for field 'sample' (test 2): \"test 2\" does not match regular expression [^\\S+\$] (Sample name must be provided and cannot contain spaces)"
        !stdout
    }

    void 'should validate a schema with failures - TSV'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema_with_samplesheet.json').toAbsolutePath()
        String script = """
            params.input = 'src/testResources/wrong.tsv'
            params.outdir = 'src/testResources/testDir'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = [:]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        List<String> errorMessages = error.message.readLines()
        errorMessages[0] == 'The following invalid input values have been detected:'
        errorMessages[1] == ''
        errorMessages[2] == '* --input (src/testResources/wrong.tsv): Validation of file failed:'
        errorMessages[3] == "\t-> Entry 1: Error for field 'strandedness' (weird): Expected any of [[forward, reverse, unstranded]] (Strandedness must be provided and be one of 'forward', 'reverse' or 'unstranded')"
        errorMessages[4] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): \"test1_fastq2.fasta\" does not match regular expression [^\\S+\\.f(ast)?q\\.gz\$]"
        errorMessages[5] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): \"test1_fastq2.fasta\" is longer than 0 characters"
        errorMessages[6] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): Value does not match against any of the schemas (FastQ file for reads 2 cannot contain spaces and must have extension '.fq.gz' or '.fastq.gz')"
        errorMessages[7] == '\t-> Entry 1: Missing required field(s): sample'
        errorMessages[8] == "\t-> Entry 2: Error for field 'sample' (test 2): \"test 2\" does not match regular expression [^\\S+\$] (Sample name must be provided and cannot contain spaces)"
        !stdout
    }

    void 'should validate a schema with failures - YAML'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema_with_samplesheet.json').toAbsolutePath()
        String script = """
            params.input = 'src/testResources/wrong.yaml'
            params.outdir = 'src/testResources/testDir'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = [:]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        List<String> errorMessages = error.message.readLines()
        errorMessages[0] == 'The following invalid input values have been detected:'
        errorMessages[1] == ''
        errorMessages[2] == '* --input (src/testResources/wrong.yaml): Validation of file failed:'
        errorMessages[3] == "\t-> Entry 1: Error for field 'strandedness' (weird): Expected any of [[forward, reverse, unstranded]] (Strandedness must be provided and be one of 'forward', 'reverse' or 'unstranded')"
        errorMessages[4] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): \"test1_fastq2.fasta\" does not match regular expression [^\\S+\\.f(ast)?q\\.gz\$]"
        errorMessages[5] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): \"test1_fastq2.fasta\" is longer than 0 characters"
        errorMessages[6] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): Value does not match against any of the schemas (FastQ file for reads 2 cannot contain spaces and must have extension '.fq.gz' or '.fastq.gz')"
        errorMessages[7] == '\t-> Entry 1: Missing required field(s): sample'
        errorMessages[8] == "\t-> Entry 2: Error for field 'sample' (test 2): \"test 2\" does not match regular expression [^\\S+\$] (Sample name must be provided and cannot contain spaces)"
        !stdout
    }

    void 'should validate a schema with failures - JSON'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema_with_samplesheet.json').toAbsolutePath()
        String script = """
            params.input = 'src/testResources/wrong.json'
            params.outdir = 'src/testResources/testDir'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = [:]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        List<String> errorMessages = error.message.readLines()
        errorMessages[0] == 'The following invalid input values have been detected:'
        errorMessages[1] == ''
        errorMessages[2] == '* --input (src/testResources/wrong.json): Validation of file failed:'
        errorMessages[3] == "\t-> Entry 1: Error for field 'strandedness' (weird): Expected any of [[forward, reverse, unstranded]] (Strandedness must be provided and be one of 'forward', 'reverse' or 'unstranded')"
        errorMessages[4] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): \"test1_fastq2.fasta\" does not match regular expression [^\\S+\\.f(ast)?q\\.gz\$]"
        errorMessages[5] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): \"test1_fastq2.fasta\" is longer than 0 characters"
        errorMessages[6] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): Value does not match against any of the schemas (FastQ file for reads 2 cannot contain spaces and must have extension '.fq.gz' or '.fastq.gz')"
        errorMessages[7] == '\t-> Entry 1: Missing required field(s): sample'
        errorMessages[8] == "\t-> Entry 2: Error for field 'sample' (test 2): \"test 2\" does not match regular expression [^\\S+\$] (Sample name must be provided and cannot contain spaces)"
        !stdout
    }

    void 'should find unexpected params'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath()
        String script = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            params.xyz = '/some/path'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """
        when:
        Map config = [:]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        noExceptionThrown()
        stdout.size() >= 1
        stdout.contains('* --xyz: /some/path')
    }

    void 'should not find unexpected params patternProperties'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath()
        String script = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            params.pattern_xyz = 'abc'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = [:]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        noExceptionThrown()
        !stdout
    }

    void 'should ignore unexpected param kebab-case like camelCase'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath()
        String script = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            params.testCamelCase = 'aCamelBug'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = [:]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        noExceptionThrown()
        !stdout
    }

    void 'should find unexpected param kebab-case not like camelCase'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath()
        String script = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                params['test-kebab-bug'] = 'a real kebab bug'
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = [:]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        noExceptionThrown()
        stdout.size() >= 1
        stdout.contains('* --test-kebab-bug: a real kebab bug')
    }

    void 'should ignore unexpected param'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath()
        String script = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            params.xyz = '/some/path'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = ['validation': [
            'ignoreParams': ['xyz']
        ]]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        noExceptionThrown()
        !stdout
    }

    void 'should ignore default unexpected param'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath()
        String script = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            params.xyz = '/some/path'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = ['validation': [
            'defaultIgnoreParams': ['xyz']
        ]]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        noExceptionThrown()
        !stdout
    }

    void 'should ignore nf_test_output param'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath()
        String script = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            params.nf_test_output = '/some/path'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = [:]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        noExceptionThrown()
        !stdout
    }

    void 'should ignore default unexpected params'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath()
        String script = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            params.xyz = '/some/path'
            params.abc = '/some/other/path'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = ['validation': [
            'ignoreParams': ['abc'],
            'defaultIgnoreParams': ['xyz']
        ]]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        noExceptionThrown()
        !stdout
    }

    void 'should ignore wrong expected params'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath()
        String script = """
            params.input = 1
            params.outdir = 2
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = ['validation': [
            'ignoreParams': ['input'],
            'defaultIgnoreParams': ['outdir']
        ]]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        noExceptionThrown()
        !stdout
    }

    void 'should fail for unexpected param'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath()
        String script = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            params.xyz = '/some/path'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = ['validation': [
            'monochromeLogs': true,
            'logging': [
                'unrecognisedParams': 'error'
            ]
        ]]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message == 'The following invalid input values have been detected:\n\n* --xyz: /some/path\n\n'
        !stdout
    }

    void 'should find validation errors'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath()
        String script = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 10
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = ['validation': [
            'monochromeLogs': true
        ]]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message == '''The following invalid input values have been detected:

* --outdir (10): Value is [integer] but should be [string]

'''
        !stdout
    }

    void 'should correctly validate duration and memory objects'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath()
        String script = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            params.max_memory = '10.GB'
            params.max_time = '10.day'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = [:]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        noExceptionThrown()
        !stdout
    }

    void 'correct validation of integers'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath()
        String script = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            params.max_cpus = 12
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = [:]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        noExceptionThrown()
        !stdout
    }

    void 'correct validation of numerics - 0'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema_required_numerics.json').toAbsolutePath()
        String script = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            params.integer = 0
            params.number = 0
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = ['validation': [
            'monochromeLogs': true
        ]]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        noExceptionThrown()
        !stdout
    }

    void 'fail validation of numerics - null'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema_required_numerics.json').toAbsolutePath()
        String script = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = ['validation': [
            'monochromeLogs': true
        ]]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message == '''The following invalid input values have been detected:

* Missing required parameter(s): number, integer

'''
        !stdout
    }

    void 'correct validation of file-path-pattern - glob'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema_file_path_pattern.json').toAbsolutePath()
        String script = """
            params.glob = 'src/testResources/*.csv'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = [:]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        noExceptionThrown()
        !stdout
    }

    void 'correct validation of file-path-pattern - single file'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema_file_path_pattern.json').toAbsolutePath()
        String script = """
            params.glob = 'src/testResources/correct.csv'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = [:]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        noExceptionThrown()
        !stdout
    }

    void 'correct validation of numbers with lenient mode'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath()
        String script = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 1
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = ['validation': [
            'lenientMode': true
        ]]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        noExceptionThrown()
        !stdout
    }

    void 'should fail because of incorrect integer'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath()
        String script = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            params.max_cpus = 1.2
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = ['validation': [
            'monochromeLogs': true
        ]]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message == 'The following invalid input values have been detected:\n\n* --max_cpus (1.2): Value is [number] but should be [integer]\n\n'
        !stdout
    }

    void 'should validate a schema from an input file'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema_with_samplesheet.json').toAbsolutePath()
        String script = """
            params.input = 'src/testResources/samplesheet.csv'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """
        when:
        Map config = [:]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        noExceptionThrown()
        !stdout
    }

    void 'should fail because of wrong file pattern'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema_with_samplesheet.json').toAbsolutePath()
        String script = """
            params.input = 'src/testResources/samplesheet_wrong_pattern.csv'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = ['validation': [
            'monochromeLogs': true
        ]]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        List<String> errorMessage = error.message.tokenize('\n')
        errorMessage[0] == 'The following invalid input values have been detected:'
        errorMessage[1] == '* --input (src/testResources/samplesheet_wrong_pattern.csv): Validation of file failed:'
        errorMessage[2] == "\t-> Entry 1: Error for field 'fastq_1' (test1_fastq1.txt): \"test1_fastq1.txt\" does not match regular expression [^\\S+\\.f(ast)?q\\.gz\$] (FastQ file for reads 1 must be provided, cannot contain spaces and must have extension '.fq.gz' or '.fastq.gz')"
        errorMessage[3] == "\t-> Entry 2: Error for field 'fastq_1' (test2_fastq1.txt): \"test2_fastq1.txt\" does not match regular expression [^\\S+\\.f(ast)?q\\.gz\$] (FastQ file for reads 1 must be provided, cannot contain spaces and must have extension '.fq.gz' or '.fastq.gz')"
        !stdout
    }

    void 'should fail because of missing required value'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema_with_samplesheet.json').toAbsolutePath()
        String script = """
            params.input = 'src/testResources/samplesheet_no_required.csv'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = ['validation': [
            'monochromeLogs': true
        ]]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message == '''The following invalid input values have been detected:

* --input (src/testResources/samplesheet_no_required.csv): Validation of file failed:
\t-> Entry 1: Missing required field(s): sample
\t-> Entry 2: Missing required field(s): strandedness, sample
\t-> Entry 3: Missing required field(s): sample

'''
        !stdout
    }

    void 'should fail because of wrong draft'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema_draft7.json').toAbsolutePath()
        String script = """
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = ['validation': [
            'monochromeLogs': true
        ]]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        thrown(SchemaValidationException)
        !stdout
    }

    void 'should fail because of existing file'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema_with_exists_false.json').toAbsolutePath()
        String script = """
            params.outdir = "src/testResources/"
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = ['validation': [
            'monochromeLogs': true
        ]]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message == '''The following invalid input values have been detected:

* --outdir (src/testResources/): the file or directory 'src/testResources/' should not exist

'''
        !stdout
    }

    void 'should fail because of non-unique entries'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema_with_samplesheet_uniqueEntries.json').toAbsolutePath()
        String script = """
            params.input = "src/testResources/samplesheet_non_unique.csv"
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = ['validation': [
            'monochromeLogs': true
        ]]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message == '''The following invalid input values have been detected:

* --input (src/testResources/samplesheet_non_unique.csv): Validation of file failed:
\t-> Entry 3: Detected duplicate entries: [sample:test_2, fastq_1:test2_fastq1.fastq.gz]

'''
        !stdout
    }

    void 'should not fail because of non-unique empty entries'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema_with_samplesheet_uniqueEntries.json').toAbsolutePath()
        String script = """
            params.input = "src/testResources/samplesheet_non_unique_empty.csv"
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = ['validation': [
            'monochromeLogs': true
        ]]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        noExceptionThrown()
        !stdout
    }

    void 'should validate nested params - pass'() {
        given:
        String script = '''
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: 'src/testResources/nextflow_schema_nested_parameters.json')
            }
        '''

        when:
        Map config = [
            'validation': [
                'monochromeLogs': true
            ],
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
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        noExceptionThrown()
        !stdout
    }

    void 'should validate nested params - fail'() {
        given:
        String script = '''
            params.map.is.so.deep = "this shouldn't be a string"
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: 'src/testResources/nextflow_schema_nested_parameters.json')
            }
        '''

        when:
        Map config = [
            'validation': [
                'monochromeLogs': true
            ],
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
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message == '''The following invalid input values have been detected:

* --map.is.so.deep (this shouldn't be a string): Value is [string] but should be [boolean]

'''
        !stdout
    }

    void 'should validate an email'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath()
        String script = """
            params.input = 'src/testResources/samplesheet.csv'
            params.outdir = 'src/testResources/testDir'
            params.email = "test@domain.com"
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = [:]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        noExceptionThrown()
        !stdout
    }

    void 'should validate an email - failure'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath()
        String script = """
            params.input = 'src/testResources/samplesheet.csv'
            params.outdir = 'src/testResources/testDir'
            params.email = "thisisnotanemail"
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = [:]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message.contains('* --email (thisisnotanemail): \"thisisnotanemail\" is not a valid email address')
        !stdout
    }

    void 'should give an error when a file-path-pattern is used with a file-path format'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath()
        String script = """
            params.input = 'src/testResources/*.csv'
            params.outdir = 'src/testResources/testDir'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = [:]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message.contains("* --input (src/testResources/*.csv): 'src/testResources/*.csv' is not a file, but a file path pattern")
        !stdout
    }

    void 'should give an error when a file-path-pattern is used with a directory-path format'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath()
        String script = """
            params.input = 'src/testResources/samplesheet.csv'
            params.outdir = 'src/testResources/testDi*'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = [:]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message.contains("* --outdir (src/testResources/testDi*): 'src/testResources/testDi*' is not a directory, but a file path pattern")
        !stdout
    }

    void 'should validate a map file - yaml'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema_with_map_file.json').toAbsolutePath()
        String script = """
            params.map_file = 'src/testResources/map_file.yaml'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = [:]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        noExceptionThrown()
        !stdout
    }

    void 'should validate a map file - json'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema_with_map_file.json').toAbsolutePath()
        String script = """
            params.map_file = 'src/testResources/map_file.json'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = [:]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        noExceptionThrown()
        !stdout
    }

    void 'should give an error when a map file is wrong - yaml'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema_with_map_file.json').toAbsolutePath()
        String script = """
            params.map_file = 'src/testResources/map_file_wrong.yaml'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = [:]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message.contains('* --map_file (src/testResources/map_file_wrong.yaml): Validation of file failed:')
        error.message.contains('\t* --this.is.deep (hello): Value is [string] but should be [integer]')
        !stdout
    }

    void 'should give an error when a map file is wrong - json'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema_with_map_file.json').toAbsolutePath()
        String script = """
            params.map_file = 'src/testResources/map_file_wrong.json'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = [:]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message.contains('* --map_file (src/testResources/map_file_wrong.json): Validation of file failed:')
        error.message.contains('\t* --this.is.deep (hello): Value is [string] but should be [integer]')
        !stdout
    }

    void 'should truncate long values in errors'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath()
        String script = """
            params.input = 'src/testResources/wrong_samplesheet_with_a_super_long_name.and_a_weird_extension'
            params.outdir = 'src/testResources/testDir'
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """
        when:
        Map config = ['validation': [
            'maxErrValSize': 20
        ]]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message.contains("* --input (src/testRe..._extension): \"src/testResources/wrong_samplesheet_with_a_super_long_name.and_a_weird_extension\" does not match regular expression [^\\S+\\.(csv|tsv|yaml|json)\$]")
        !stdout
    }

    void 'should correctly detect invalid parameters'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema_no_type.json').toAbsolutePath()
        String script = """
            params.genome = [
                "test": "test"
            ]
            params.genomebutlonger = true
            params.testing = "test"
            include { validateParameters } from 'plugin/nf-schema'

            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        Map config = ['validation': [
            'defaultIgnoreParams': [ 'genome' ]
        ]]
        runScript(script, config)
        List<String> stdout = capture
                .toString()
                .readLines()
                .findResults { line -> line.contains('WARN nextflow.validation.SchemaValidator') || line.startsWith('* --') ? line : null }

        then:
        noExceptionThrown()
        stdout == ['* --testing: test', '* --genomebutlonger: true']
    }

    void 'should correctly validate static types paths'() {
        given:
        String schema = Path.of('src/testResources/nextflow_schema_no_type.json').toAbsolutePath()
        String script = """
            include { validateParameters } from 'plugin/nf-schema'
            params {
                input = file('src/testResources/correct.csv')
                outdir = file('src/testResources/testDir')
            }
            workflow {
                validateParameters(parameters_schema: '${schema}')
            }
        """

        when:
        runScript(script)

        then:
        noExceptionThrown()
    }

}
