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
 * @author : jorgeaguileraseqera
 */
class ValidateParametersTest extends Dsl2Spec{

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

    def 'should validate when no params' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def SCRIPT = """
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: 'src/testResources/nextflow_schema.json')
        """

        when:
        def config = ["validation": [
            "monochromeLogs": true
        ]]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        error.message == """The following invalid input values have been detected:

* Missing required parameter(s): input, outdir

"""
        !stdout
    }

    def 'should validate a schema with no arguments' () {
        given:
        def schema_source = new File('src/testResources/nextflow_schema.json')
        def schema_dest   = new File('nextflow_schema.json')
        schema_dest << schema_source.text

        def SCRIPT = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters()
        """

        when:
        def config = [:]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        !stdout

        cleanup:
        schema_dest.delete()
    }

    def 'should validate a schema - CSV' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = [:]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'should validate a schema - TSV' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.input = 'src/testResources/correct.tsv'
            params.outdir = 'src/testResources/testDir'
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = [:]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'should validate a schema - YAML' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.input = 'src/testResources/correct.yaml'
            params.outdir = 'src/testResources/testDir'
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = [:]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'should validate a schema - JSON' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.input = 'src/testResources/correct.json'
            params.outdir = 'src/testResources/testDir'
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = [:]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'should validate a schema with failures - CSV' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema_with_samplesheet.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.input = 'src/testResources/wrong.csv'
            params.outdir = 'src/testResources/testDir'
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = [:]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        def errorMessages = error.message.readLines()
        errorMessages[0] == "\033[0;31mThe following invalid input values have been detected:"
        errorMessages[1] == ""
        errorMessages[2] == "* --input (src/testResources/wrong.csv): Validation of file failed:"
        errorMessages[3] == "\t-> Entry 1: Error for field 'strandedness' (weird): Expected any of [[forward, reverse, unstranded]] (Strandedness must be provided and be one of 'forward', 'reverse' or 'unstranded')"
        errorMessages[4] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): \"test1_fastq2.fasta\" does not match regular expression [^\\S+\\.f(ast)?q\\.gz\$]"
        errorMessages[5] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): \"test1_fastq2.fasta\" is longer than 0 characters"
        errorMessages[6] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): Value does not match against any of the schemas (FastQ file for reads 2 cannot contain spaces and must have extension '.fq.gz' or '.fastq.gz')"
        errorMessages[7] == "\t-> Entry 1: Missing required field(s): sample"
        errorMessages[8] == "\t-> Entry 2: Error for field 'sample' (test 2): \"test 2\" does not match regular expression [^\\S+\$] (Sample name must be provided and cannot contain spaces)"
        !stdout
    }

    def 'should validate a schema with failures - TSV' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema_with_samplesheet.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.input = 'src/testResources/wrong.tsv'
            params.outdir = 'src/testResources/testDir'
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = [:]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        def errorMessages = error.message.readLines()
        errorMessages[0] == "\033[0;31mThe following invalid input values have been detected:"
        errorMessages[1] == ""
        errorMessages[2] == "* --input (src/testResources/wrong.tsv): Validation of file failed:"
        errorMessages[3] == "\t-> Entry 1: Error for field 'strandedness' (weird): Expected any of [[forward, reverse, unstranded]] (Strandedness must be provided and be one of 'forward', 'reverse' or 'unstranded')"
        errorMessages[4] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): \"test1_fastq2.fasta\" does not match regular expression [^\\S+\\.f(ast)?q\\.gz\$]"
        errorMessages[5] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): \"test1_fastq2.fasta\" is longer than 0 characters"
        errorMessages[6] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): Value does not match against any of the schemas (FastQ file for reads 2 cannot contain spaces and must have extension '.fq.gz' or '.fastq.gz')"
        errorMessages[7] == "\t-> Entry 1: Missing required field(s): sample"
        errorMessages[8] == "\t-> Entry 2: Error for field 'sample' (test 2): \"test 2\" does not match regular expression [^\\S+\$] (Sample name must be provided and cannot contain spaces)"
        !stdout
    }

    def 'should validate a schema with failures - YAML' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema_with_samplesheet.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.input = 'src/testResources/wrong.yaml'
            params.outdir = 'src/testResources/testDir'
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = [:]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        def errorMessages = error.message.readLines()
        errorMessages[0] == "\033[0;31mThe following invalid input values have been detected:"
        errorMessages[1] == ""
        errorMessages[2] == "* --input (src/testResources/wrong.yaml): Validation of file failed:"
        errorMessages[3] == "\t-> Entry 1: Error for field 'strandedness' (weird): Expected any of [[forward, reverse, unstranded]] (Strandedness must be provided and be one of 'forward', 'reverse' or 'unstranded')"
        errorMessages[4] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): \"test1_fastq2.fasta\" does not match regular expression [^\\S+\\.f(ast)?q\\.gz\$]"
        errorMessages[5] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): \"test1_fastq2.fasta\" is longer than 0 characters"
        errorMessages[6] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): Value does not match against any of the schemas (FastQ file for reads 2 cannot contain spaces and must have extension '.fq.gz' or '.fastq.gz')"
        errorMessages[7] == "\t-> Entry 1: Missing required field(s): sample"
        errorMessages[8] == "\t-> Entry 2: Error for field 'sample' (test 2): \"test 2\" does not match regular expression [^\\S+\$] (Sample name must be provided and cannot contain spaces)"
        !stdout
    }

    def 'should validate a schema with failures - JSON' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema_with_samplesheet.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.input = 'src/testResources/wrong.json'
            params.outdir = 'src/testResources/testDir'
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = [:]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        def errorMessages = error.message.readLines()
        errorMessages[0] == "\033[0;31mThe following invalid input values have been detected:"
        errorMessages[1] == ""
        errorMessages[2] == "* --input (src/testResources/wrong.json): Validation of file failed:"
        errorMessages[3] == "\t-> Entry 1: Error for field 'strandedness' (weird): Expected any of [[forward, reverse, unstranded]] (Strandedness must be provided and be one of 'forward', 'reverse' or 'unstranded')"
        errorMessages[4] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): \"test1_fastq2.fasta\" does not match regular expression [^\\S+\\.f(ast)?q\\.gz\$]"
        errorMessages[5] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): \"test1_fastq2.fasta\" is longer than 0 characters"
        errorMessages[6] == "\t-> Entry 1: Error for field 'fastq_2' (test1_fastq2.fasta): Value does not match against any of the schemas (FastQ file for reads 2 cannot contain spaces and must have extension '.fq.gz' or '.fastq.gz')"
        errorMessages[7] == "\t-> Entry 1: Missing required field(s): sample"
        errorMessages[8] == "\t-> Entry 2: Error for field 'sample' (test 2): \"test 2\" does not match regular expression [^\\S+\$] (Sample name must be provided and cannot contain spaces)"
        !stdout
    }

    def 'should find unexpected params' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            params.xyz = '/some/path'
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = [:]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        stdout.size() >= 1
        stdout.contains("* --xyz: /some/path")
    }

    def 'should not find unexpected params patternProperties' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            params.pattern_xyz = 'abc'
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = [:]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'should ignore unexpected param kebab-case like camelCase' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            params.testCamelCase = 'aCamelBug'
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = [:]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'should find unexpected param kebab-case not like camelCase' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            params['test-kebab-bug'] = 'a real kebab bug'
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = [:]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        stdout.size() >= 1
        stdout.contains("* --test-kebab-bug: a real kebab bug")
    }

    def 'should ignore unexpected param' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            params.xyz = '/some/path'
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = ["validation": [
            "ignoreParams": ['xyz']
        ]]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'should ignore default unexpected param' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            params.xyz = '/some/path'
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = ["validation": [
            "defaultIgnoreParams": ['xyz']
        ]]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'should ignore nf_test_output param' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            params.nf_test_output = '/some/path'
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = [:]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'should ignore default unexpected param' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            params.xyz = '/some/path'
            params.abc = '/some/other/path'
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = ["validation": [
            "ignoreParams": ['abc'],
            "defaultIgnoreParams": ['xyz']
        ]]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'should ignore wrong expected params' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.input = 1
            params.outdir = 2
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = ["validation": [
            "ignoreParams": ['input'],
            "defaultIgnoreParams": ['outdir']
        ]]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'should fail for unexpected param' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            params.xyz = '/some/path'
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = ["validation": [
            "failUnrecognisedParams": true,
            "monochromeLogs": true
        ]]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        error.message == "The following invalid input values have been detected:\n\n* --xyz: /some/path\n\n"
        !stdout
    }

    def 'should find validation errors' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 10
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = ["validation": [
            "monochromeLogs": true
        ]]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        error.message == """The following invalid input values have been detected:

* --outdir (10): Value is [integer] but should be [string]

"""
        !stdout
    }

    def 'should correctly validate duration and memory objects' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def  SCRIPT = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            params.max_memory = '10.GB'
            params.max_time = '10.day'
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = [:]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'correct validation of integers' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            params.max_cpus = 12
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = [:]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'correct validation of numerics - 0' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema_required_numerics.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            params.integer = 0
            params.number = 0
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = ["validation": [
            "monochromeLogs": true
        ]]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'fail validation of numerics - null' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema_required_numerics.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = ["validation": [
            "monochromeLogs": true
        ]]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        error.message == """The following invalid input values have been detected:

* Missing required parameter(s): number, integer

"""
        !stdout
    }

    def 'correct validation of file-path-pattern - glob' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema_file_path_pattern.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.glob = 'src/testResources/*.csv'
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = [:]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'correct validation of file-path-pattern - single file' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema_file_path_pattern.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.glob = 'src/testResources/correct.csv'
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = [:]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'ignore validation of aws s3' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema_file_path_pattern.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.glob = 's3://src/testResources/correct.csv'
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = [:]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'ignore validation of azure blob storage' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema_file_path_pattern.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.glob = 'az://src/testResources/correct.csv'
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = [:]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'ignore validation of gcp cloud storage' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema_file_path_pattern.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.glob = 'gs://src/testResources/correct.csv'
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = [:]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'correct validation of numbers with lenient mode' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 1
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = ["validation": [
            "lenientMode": true
        ]]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'should fail because of incorrect integer' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.input = 'src/testResources/correct.csv'
            params.outdir = 'src/testResources/testDir'
            params.max_cpus = 1.2
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = ["validation": [
            "monochromeLogs": true
        ]]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        error.message == "The following invalid input values have been detected:\n\n* --max_cpus (1.2): Value is [number] but should be [integer]\n\n"
        !stdout
    }


    def 'should validate a schema from an input file' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema_with_samplesheet.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.input = 'src/testResources/samplesheet.csv'
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = [:]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'should fail because of wrong file pattern' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema_with_samplesheet.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.input = 'src/testResources/samplesheet_wrong_pattern.csv'
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = ["validation": [
            "monochromeLogs": true
        ]]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        def errorMessage = error.message.tokenize("\n")
        errorMessage[0] == "The following invalid input values have been detected:"
        errorMessage[1] == "* --input (src/testResources/samplesheet_wrong_pattern.csv): Validation of file failed:"
        errorMessage[2] == "\t-> Entry 1: Error for field 'fastq_1' (test1_fastq1.txt): \"test1_fastq1.txt\" does not match regular expression [^\\S+\\.f(ast)?q\\.gz\$] (FastQ file for reads 1 must be provided, cannot contain spaces and must have extension '.fq.gz' or '.fastq.gz')"
        errorMessage[3] == "\t-> Entry 2: Error for field 'fastq_1' (test2_fastq1.txt): \"test2_fastq1.txt\" does not match regular expression [^\\S+\\.f(ast)?q\\.gz\$] (FastQ file for reads 1 must be provided, cannot contain spaces and must have extension '.fq.gz' or '.fastq.gz')"
        !stdout
    }

    def 'should fail because of missing required value' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema_with_samplesheet.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.input = 'src/testResources/samplesheet_no_required.csv'
            include { validateParameters } from 'plugin/nf-schema'

            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = ["validation": [
            "monochromeLogs": true
        ]]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        error.message == '''The following invalid input values have been detected:

* --input (src/testResources/samplesheet_no_required.csv): Validation of file failed:
\t-> Entry 1: Missing required field(s): sample
\t-> Entry 2: Missing required field(s): strandedness, sample
\t-> Entry 3: Missing required field(s): sample

'''
        !stdout
    }

    def 'should fail because of wrong draft' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema_draft7.json').toAbsolutePath().toString()
        def SCRIPT = """
            include { validateParameters } from 'plugin/nf-schema'

            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = ["validation": [
            "monochromeLogs": true
        ]]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        !stdout
    }

    def 'should fail because of existing file' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema_with_exists_false.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.outdir = "src/testResources/"
            include { validateParameters } from 'plugin/nf-schema'

            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = ["validation": [
            "monochromeLogs": true
        ]]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        error.message == '''The following invalid input values have been detected:

* --outdir (src/testResources/): the file or directory 'src/testResources/' should not exist

'''
        !stdout
    }

    def 'should fail because of non-unique entries' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema_with_samplesheet_uniqueEntries.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.input = "src/testResources/samplesheet_non_unique.csv"
            include { validateParameters } from 'plugin/nf-schema'

            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = ["validation": [
            "monochromeLogs": true
        ]]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        error.message == '''The following invalid input values have been detected:

* --input (src/testResources/samplesheet_non_unique.csv): Validation of file failed:
	-> Entry 3: Detected duplicate entries: [sample:test_2, fastq_1:test2_fastq1.fastq.gz]

'''
        !stdout
    }

    def 'should not fail because of non-unique empty entries' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema_with_samplesheet_uniqueEntries.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.input = "src/testResources/samplesheet_non_unique_empty.csv"
            include { validateParameters } from 'plugin/nf-schema'

            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = ["validation": [
            "monochromeLogs": true
        ]]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'should validate nested params - pass' () {
        given:
        def SCRIPT = """
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: 'src/testResources/nextflow_schema_nested_parameters.json')
        """

        when:
        def config = [
            "validation": [
                "monochromeLogs": true
            ],
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
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'should validate nested params - fail' () {
        given:
        def SCRIPT = """
            params.this.is.so.deep = "this shouldn't be a string"
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: 'src/testResources/nextflow_schema_nested_parameters.json')
        """

        when:
        def config = [
            "validation": [
                "monochromeLogs": true
            ],
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
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        error.message == """The following invalid input values have been detected:

* --this.is.so.deep (this shouldn't be a string): Value is [string] but should be [boolean]

"""
        !stdout
    }

    def 'should validate an email' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.input = 'src/testResources/samplesheet.csv'
            params.outdir = 'src/testResources/testDir'
            params.email = "test@domain.com"
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = [:]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'should validate an email - failure' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.input = 'src/testResources/samplesheet.csv'
            params.outdir = 'src/testResources/testDir'
            params.email = "thisisnotanemail"
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = [:]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }


        then:
        def error = thrown(SchemaValidationException)
        error.message.contains("* --email (thisisnotanemail): \"thisisnotanemail\" is not a valid email address")
        !stdout
    }

    def 'should give an error when a file-path-pattern is used with a file-path format' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.input = 'src/testResources/*.csv'
            params.outdir = 'src/testResources/testDir'
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = [:]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }


        then:
        def error = thrown(SchemaValidationException)
        error.message.contains("* --input (src/testResources/*.csv): 'src/testResources/*.csv' is not a file, but a file path pattern")
        !stdout
    }

    def 'should give an error when a file-path-pattern is used with a directory-path format' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.input = 'src/testResources/samplesheet.csv'
            params.outdir = 'src/testResources/testDi*'
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = [:]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }


        then:
        def error = thrown(SchemaValidationException)
        error.message.contains("* --outdir (src/testResources/testDi*): 'src/testResources/testDi*' is not a directory, but a file path pattern")
        !stdout
    }

    def 'should validate a map file - yaml' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema_with_map_file.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.map_file = 'src/testResources/map_file.yaml'
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = [:]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'should validate a map file - json' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema_with_map_file.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.map_file = 'src/testResources/map_file.json'
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = [:]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'should give an error when a map file is wrong - yaml' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema_with_map_file.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.map_file = 'src/testResources/map_file_wrong.yaml'
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = [:]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }


        then:
        def error = thrown(SchemaValidationException)
        error.message.contains("* --map_file (src/testResources/map_file_wrong.yaml): Validation of file failed:")
        error.message.contains("	* --this.is.deep (hello): Value is [string] but should be [integer]")
        !stdout
    }

    def 'should give an error when a map file is wrong - json' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema_with_map_file.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.map_file = 'src/testResources/map_file_wrong.json'
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = [:]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }


        then:
        def error = thrown(SchemaValidationException)
        error.message.contains("* --map_file (src/testResources/map_file_wrong.json): Validation of file failed:")
        error.message.contains("	* --this.is.deep (hello): Value is [string] but should be [integer]")
        !stdout
    }

    def 'should truncate long values in errors' () {
        given:
        def schema = Path.of('src/testResources/nextflow_schema.json').toAbsolutePath().toString()
        def SCRIPT = """
            params.input = 'src/testResources/wrong_samplesheet_with_a_super_long_name.and_a_weird_extension'
            params.outdir = 'src/testResources/testDir'
            include { validateParameters } from 'plugin/nf-schema'
            
            validateParameters(parameters_schema: '$schema')
        """

        when:
        def config = ["validation": [
            "maxErrValSize": 20
        ]]
        def result = new MockScriptRunner(config).setScript(SCRIPT).execute()
        def stdout = capture
                .toString()
                .readLines()
                .findResults {it.contains('WARN nextflow.validation.SchemaValidator') || it.startsWith('* --') ? it : null }

        then:
        def error = thrown(SchemaValidationException)
        error.message.contains("* --input (src/testRe..._extension): \"src/testResources/wrong_samplesheet_with_a_super_long_name.and_a_weird_extension\" does not match regular expression [^\\S+\\.(csv|tsv|yaml|json)\$]")
        !stdout
    }
}