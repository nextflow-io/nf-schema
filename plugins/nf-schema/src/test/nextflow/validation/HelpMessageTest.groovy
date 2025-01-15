package nextflow.validation

import java.nio.file.Path

import nextflow.plugin.Plugins
import nextflow.plugin.TestPluginDescriptorFinder
import nextflow.plugin.TestPluginManager
import nextflow.plugin.extension.PluginExtensionProvider
import org.pf4j.PluginDescriptorFinder
import nextflow.Session
import spock.lang.Specification
import spock.lang.Shared
import org.slf4j.Logger
import org.junit.Rule
import test.Dsl2Spec
import test.OutputCapture

import nextflow.validation.ValidationConfig
import nextflow.validation.help.HelpMessage

/**
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */
class HelpMessageTest extends Specification{

    @Rule
    OutputCapture capture = new OutputCapture()

    @Shared String pluginsMode

    Path root = Path.of('.').toAbsolutePath().normalize()
    Path getRoot() { this.root }
    String getRootString() { this.root.toString() }

    private Session session

    def setup() {
        session = Mock(Session)
        session.getBaseDir() >> getRoot()
    }

    def 'should get a short help message' () {
        given:
        def validationConfig = [
            monochromeLogs: true,
            parametersSchema: 'src/testResources/nextflow_schema.json',
            help: [
                enabled: true
            ]
        ]
        def params = [:]
        def config = new ValidationConfig(validationConfig, params)
        def helpMessage = new HelpMessage(config, session)

        when:
        def help = helpMessage.getShortHelpMessage("")

        then:
        noExceptionThrown()
        def expectedHelp = """--help            [boolean, string] Show the help message for all top level parameters. When a parameter is given to `--help`, the full 
help message of that parameter will be printed. 
--helpFull        [boolean]         Show the help message for all non-hidden parameters. 
--showHidden      [boolean]         Show all hidden parameters in the help message. This needs to be used in combination with `--help` 
or `--helpFull`. 

Input/output options
  --input         [string] Path to comma-separated file containing information about the samples in the experiment. 
  --outdir        [string] The output directory where the results will be saved. You have to use absolute paths to storage on 
Cloud infrastructure. 
  --email         [string] Email address for completion summary. 
  --multiqc_title [string] MultiQC report title. Printed as page header, used for filename if not otherwise specified. 

Reference genome options
  --genome        [string] Name of iGenomes reference. 
  --fasta         [string] Path to FASTA genome file. 

"""
        def resultHelp = help.readLines()
        expectedHelp.readLines().each {
            assert help.contains(it)
            resultHelp.removeElement(it)
        }
        assert resultHelp.size() == 0, "Found extra unexpected lines: ${resultHelp}"
    }

    def 'should get a short help message with hidden params - config' () {
        given:
        def validationConfig = [
            monochromeLogs: true,
            parametersSchema: 'src/testResources/nextflow_schema.json',
            help: [
                enabled: true,
                showHidden: true
            ]
        ]
        def params = [:]
        def config = new ValidationConfig(validationConfig, params)
        def helpMessage = new HelpMessage(config, session)

        when:
        def help = helpMessage.getShortHelpMessage("")

        then:
        noExceptionThrown()
        def expectedHelp = """--help                         [boolean, string] Show the help message for all top level parameters. When a parameter is given to `--help`, the full 
help message of that parameter will be printed. 
--helpFull                     [boolean]         Show the help message for all non-hidden parameters. 
--showHidden                   [boolean]         Show all hidden parameters in the help message. This needs to be used in combination with `--help` 
or `--helpFull`. 

Input/output options
  --input                      [string] Path to comma-separated file containing information about the samples in the experiment. 
  --outdir                     [string] The output directory where the results will be saved. You have to use absolute paths to storage on 
Cloud infrastructure. 
  --email                      [string] Email address for completion summary. 
  --multiqc_title              [string] MultiQC report title. Printed as page header, used for filename if not otherwise specified. 

Reference genome options
  --genome                     [string]  Name of iGenomes reference. 
  --fasta                      [string]  Path to FASTA genome file. 
  --igenomes_base              [string]  Directory / URL base for iGenomes references. [default: s3://ngi-igenomes/igenomes] 
  --igenomes_ignore            [boolean] Do not load the iGenomes reference config. 

Institutional config options
  --custom_config_version      [string] Git commit id for Institutional configs. [default: master] 
  --custom_config_base         [string] Base directory for Institutional configs. [default: 
https://raw.githubusercontent.com/nf-core/configs/master] 
  --config_profile_name        [string] Institutional config name. 
  --config_profile_description [string] Institutional config description. 
  --config_profile_contact     [string] Institutional config contact information. 
  --config_profile_url         [string] Institutional config URL link. 

Max job request options
  --max_cpus                   [integer] Maximum number of CPUs that can be requested for any single job. [default: 16] 
  --max_memory                 [string]  Maximum amount of memory that can be requested for any single job. [default: 128.GB] 
  --max_time                   [string]  Maximum amount of time that can be requested for any single job. [default: 240.h] 

Generic options
  --help                       [string, boolean] Display help text. 
  --publish_dir_mode           [string]          Method used to save pipeline results to output directory.  (accepted: symlink, rellink, link, copy, 
copyNoFollow, move) [default: copy] 
  --email_on_fail              [string]          Email address for completion summary, only when pipeline fails. 
  --plaintext_email            [boolean]         Send plain-text email instead of HTML. 
  --max_multiqc_email_size     [string]          File size limit when attaching MultiQC reports to summary emails. [default: 25.MB] 
  --monochrome_logs            [boolean]         Do not use coloured log outputs. 
  --multiqc_config             [string]          Custom config file to supply to MultiQC. 
  --tracedir                   [string]          Directory to keep pipeline Nextflow logs and reports. [default: \${params.outdir}/pipeline_info] 
  --validate_params            [boolean]         Boolean whether to validate parameters against the schema at runtime [default: true] 
  --validationShowHiddenParams [boolean]         Show all params when using `--help` 
  --enable_conda               [boolean]         Run this workflow with Conda. You can also use '-profile conda' instead of providing this parameter. 

"""
        def resultHelp = help.readLines()
        expectedHelp.readLines().each {
            assert help.contains(it)
            resultHelp.removeElement(it)
        }
        assert resultHelp.size() == 0, "Found extra unexpected lines: ${resultHelp}"
    }

    def 'should get a short help message with hidden params - param' () {
        given:
        def validationConfig = [
            monochromeLogs: true,
            parametersSchema: 'src/testResources/nextflow_schema.json',
            help: [
                enabled: true,
                showHiddenParameter: "showMeThoseHiddenParams"
            ]
        ]
        def params = [
            showMeThoseHiddenParams:true
        ]
        def config = new ValidationConfig(validationConfig, params)
        def helpMessage = new HelpMessage(config, session)

        when:
        def help = helpMessage.getShortHelpMessage("")

        then:
        noExceptionThrown()
        def expectedHelp = """--help                         [boolean, string] Show the help message for all top level parameters. When a parameter is given to `--help`, the full 
help message of that parameter will be printed. 
--helpFull                     [boolean]         Show the help message for all non-hidden parameters. 
--showMeThoseHiddenParams      [boolean]         Show all hidden parameters in the help message. This needs to be used in combination with `--help` 
or `--helpFull`. 

Input/output options
  --input                      [string] Path to comma-separated file containing information about the samples in the experiment. 
  --outdir                     [string] The output directory where the results will be saved. You have to use absolute paths to storage on 
Cloud infrastructure. 
  --email                      [string] Email address for completion summary. 
  --multiqc_title              [string] MultiQC report title. Printed as page header, used for filename if not otherwise specified. 

Reference genome options
  --genome                     [string]  Name of iGenomes reference. 
  --fasta                      [string]  Path to FASTA genome file. 
  --igenomes_base              [string]  Directory / URL base for iGenomes references. [default: s3://ngi-igenomes/igenomes] 
  --igenomes_ignore            [boolean] Do not load the iGenomes reference config. 

Institutional config options
  --custom_config_version      [string] Git commit id for Institutional configs. [default: master] 
  --custom_config_base         [string] Base directory for Institutional configs. [default: 
https://raw.githubusercontent.com/nf-core/configs/master] 
  --config_profile_name        [string] Institutional config name. 
  --config_profile_description [string] Institutional config description. 
  --config_profile_contact     [string] Institutional config contact information. 
  --config_profile_url         [string] Institutional config URL link. 

Max job request options
  --max_cpus                   [integer] Maximum number of CPUs that can be requested for any single job. [default: 16] 
  --max_memory                 [string]  Maximum amount of memory that can be requested for any single job. [default: 128.GB] 
  --max_time                   [string]  Maximum amount of time that can be requested for any single job. [default: 240.h] 

Generic options
  --help                       [string, boolean] Display help text. 
  --publish_dir_mode           [string]          Method used to save pipeline results to output directory.  (accepted: symlink, rellink, link, copy, 
copyNoFollow, move) [default: copy] 
  --email_on_fail              [string]          Email address for completion summary, only when pipeline fails. 
  --plaintext_email            [boolean]         Send plain-text email instead of HTML. 
  --max_multiqc_email_size     [string]          File size limit when attaching MultiQC reports to summary emails. [default: 25.MB] 
  --monochrome_logs            [boolean]         Do not use coloured log outputs. 
  --multiqc_config             [string]          Custom config file to supply to MultiQC. 
  --tracedir                   [string]          Directory to keep pipeline Nextflow logs and reports. [default: \${params.outdir}/pipeline_info] 
  --validate_params            [boolean]         Boolean whether to validate parameters against the schema at runtime [default: true] 
  --validationShowHiddenParams [boolean]         Show all params when using `--help` 
  --enable_conda               [boolean]         Run this workflow with Conda. You can also use '-profile conda' instead of providing this parameter. 

"""
        def resultHelp = help.readLines()
        expectedHelp.readLines().each {
            assert help.contains(it)
            resultHelp.removeElement(it)
        }
        assert resultHelp.size() == 0, "Found extra unexpected lines: ${resultHelp}"
    }

    def 'should get a full help message' () {
        given:
        def validationConfig = [
            monochromeLogs: true,
            parametersSchema: 'src/testResources/nextflow_schema_nested_parameters.json',
            help: [
                enabled: true
            ]
        ]
        def params = [:]
        def config = new ValidationConfig(validationConfig, params)
        def helpMessage = new HelpMessage(config, session)

        when:
        def help = helpMessage.getFullHelpMessage()

        then:
        noExceptionThrown()
        def expectedHelp = """--help              [boolean, string] Show the help message for all top level parameters. When a parameter is given to `--help`, the full 
help message of that parameter will be printed. 
--helpFull          [boolean]         Show the help message for all non-hidden parameters. 
--showHidden        [boolean]         Show all hidden parameters in the help message. This needs to be used in combination with `--help` 
or `--helpFull`. 

Nested Parameters
  --this.is.so.deep [boolean] so deep [default: true] 

"""
        def resultHelp = help.readLines()
        expectedHelp.readLines().each {
            assert help.contains(it)
            resultHelp.removeElement(it)
        }
        assert resultHelp.size() == 0, "Found extra unexpected lines: ${resultHelp}"
    }

    def 'should get a detailed help message - nested' () {
        given:
        def validationConfig = [
            monochromeLogs: true,
            parametersSchema: 'src/testResources/nextflow_schema_nested_parameters.json',
            help: [
                enabled: true
            ]
        ]
        def params = [:]
        def config = new ValidationConfig(validationConfig, params)
        def helpMessage = new HelpMessage(config, session)

        when:
        def help = helpMessage.getShortHelpMessage("this")

        then:
        noExceptionThrown()
        def expectedHelp = """--this
    description: this is this
    options    : 
      --this.is.so.deep [boolean] so deep [default: true] 

"""
        def resultHelp = help.readLines()
        expectedHelp.readLines().each {
            assert help.contains(it)
            resultHelp.removeElement(it)
        }
        assert resultHelp.size() == 0, "Found extra unexpected lines: ${resultHelp}"
    }

    def 'should get a detailed help message - not nested' () {
        given:
        def validationConfig = [
            monochromeLogs: true,
            parametersSchema: 'src/testResources/nextflow_schema.json',
            help: [
                enabled: true
            ]
        ]
        def params = [:]
        def config = new ValidationConfig(validationConfig, params)
        def helpMessage = new HelpMessage(config, session)

        when:
        def help = helpMessage.getShortHelpMessage("input")

        then:
        noExceptionThrown()
        def expectedHelp = """--input
    type       : string
    format     : file-path
    mimetype   : text/csv
    pattern    : ^\\S+\\.(csv|tsv|yaml|json)\$
    description: Path to comma-separated file containing information about the samples in the experiment.
    help_text  : You will need to create a design file with information about the samples in your experiment before 
running the pipeline. Use this parameter to specify its location. It has to be a comma-separated 
file with 3 columns, and a header row. See [usage 
docs](https://nf-co.re/testpipeline/usage#samplesheet-input). 

"""
        def resultHelp = help.readLines()
        expectedHelp.readLines().each {
            assert help.contains(it)
            resultHelp.removeElement(it)
        }
        assert resultHelp.size() == 0, "Found extra unexpected lines: ${resultHelp}"
    }

    def 'should get a before text - with command' () {
        given:
        def validationConfig = [
            monochromeLogs: true,
            parametersSchema: 'src/testResources/nextflow_schema.json',
            help: [
                enabled: true,
                command: "nextflow run test/test --profile test,docker --outdir results",
                beforeText: """Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas condimentum ligula ac metus sollicitudin rutrum. Vestibulum a lectus ipsum. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Cras consequat placerat aliquet. Maecenas et vulputate nibh. Donec luctus, purus ut scelerisque ornare, sem nisl mollis libero, non faucibus nibh nunc ac nulla. Donec et pharetra neque. Etiam id nibh vel turpis ornare efficitur. Cras eu eros mi.
Etiam at nulla ac dui ullamcorper viverra. Donec posuere imperdiet eros nec consequat. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Nullam nec aliquam magna. Quisque nec dapibus velit, id convallis justo. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Suspendisse bibendum ipsum quis nulla fringilla laoreet. Integer dictum, purus et pretium ultrices, nunc nisl vestibulum erat, et tempus ex massa eget nunc. Interdum et malesuada fames ac ante ipsum primis in faucibus. Integer varius aliquam vestibulum. Proin sit amet lobortis ipsum. Vestibulum fermentum lorem ac erat pharetra, eu eleifend sapien hendrerit. Quisque id varius ex. Morbi et dui et libero varius tempus. Ut eu sagittis lorem, sed congue libero.
"""
            ]
        ]
        def params = [:]
        def config = new ValidationConfig(validationConfig, params)
        def helpMessage = new HelpMessage(config, session)

        when:
        def help = helpMessage.getBeforeText()

        then:
        noExceptionThrown()
        def expectedHelp = """Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas condimentum ligula ac metus sollicitudin rutrum. Vestibulum a lectus ipsum. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Cras consequat placerat aliquet. Maecenas et vulputate nibh. Donec luctus, purus ut scelerisque ornare, sem nisl mollis libero, non faucibus nibh nunc ac nulla. Donec et pharetra neque. Etiam id nibh vel turpis ornare efficitur. Cras eu eros mi.
Etiam at nulla ac dui ullamcorper viverra. Donec posuere imperdiet eros nec consequat. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Nullam nec aliquam magna. Quisque nec dapibus velit, id convallis justo. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Suspendisse bibendum ipsum quis nulla fringilla laoreet. Integer dictum, purus et pretium ultrices, nunc nisl vestibulum erat, et tempus ex massa eget nunc. Interdum et malesuada fames ac ante ipsum primis in faucibus. Integer varius aliquam vestibulum. Proin sit amet lobortis ipsum. Vestibulum fermentum lorem ac erat pharetra, eu eleifend sapien hendrerit. Quisque id varius ex. Morbi et dui et libero varius tempus. Ut eu sagittis lorem, sed congue libero.
Typical pipeline command:

  nextflow run test/test --profile test,docker --outdir results

"""
        def resultHelp = help.readLines()
        expectedHelp.readLines().each {
            assert help.contains(it)
            resultHelp.removeElement(it)
        }
        assert resultHelp.size() == 0, "Found extra unexpected lines: ${resultHelp}"
    }

    def 'should get a before text - without command' () {
        given:
        def validationConfig = [
            monochromeLogs: true,
            parametersSchema: 'src/testResources/nextflow_schema.json',
            help: [
                enabled: true,
                beforeText: """Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas condimentum ligula ac metus sollicitudin rutrum. Vestibulum a lectus ipsum. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Cras consequat placerat aliquet. Maecenas et vulputate nibh. Donec luctus, purus ut scelerisque ornare, sem nisl mollis libero, non faucibus nibh nunc ac nulla. Donec et pharetra neque. Etiam id nibh vel turpis ornare efficitur. Cras eu eros mi.
Etiam at nulla ac dui ullamcorper viverra. Donec posuere imperdiet eros nec consequat. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Nullam nec aliquam magna. Quisque nec dapibus velit, id convallis justo. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Suspendisse bibendum ipsum quis nulla fringilla laoreet. Integer dictum, purus et pretium ultrices, nunc nisl vestibulum erat, et tempus ex massa eget nunc. Interdum et malesuada fames ac ante ipsum primis in faucibus. Integer varius aliquam vestibulum. Proin sit amet lobortis ipsum. Vestibulum fermentum lorem ac erat pharetra, eu eleifend sapien hendrerit. Quisque id varius ex. Morbi et dui et libero varius tempus. Ut eu sagittis lorem, sed congue libero.
"""
            ]
        ]
        def params = [:]
        def config = new ValidationConfig(validationConfig, params)
        def helpMessage = new HelpMessage(config, session)

        when:
        def help = helpMessage.getBeforeText()

        then:
        noExceptionThrown()
        def expectedHelp = """Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas condimentum ligula ac metus sollicitudin rutrum. Vestibulum a lectus ipsum. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Cras consequat placerat aliquet. Maecenas et vulputate nibh. Donec luctus, purus ut scelerisque ornare, sem nisl mollis libero, non faucibus nibh nunc ac nulla. Donec et pharetra neque. Etiam id nibh vel turpis ornare efficitur. Cras eu eros mi.
Etiam at nulla ac dui ullamcorper viverra. Donec posuere imperdiet eros nec consequat. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Nullam nec aliquam magna. Quisque nec dapibus velit, id convallis justo. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Suspendisse bibendum ipsum quis nulla fringilla laoreet. Integer dictum, purus et pretium ultrices, nunc nisl vestibulum erat, et tempus ex massa eget nunc. Interdum et malesuada fames ac ante ipsum primis in faucibus. Integer varius aliquam vestibulum. Proin sit amet lobortis ipsum. Vestibulum fermentum lorem ac erat pharetra, eu eleifend sapien hendrerit. Quisque id varius ex. Morbi et dui et libero varius tempus. Ut eu sagittis lorem, sed congue libero.

"""
        def resultHelp = help.readLines()
        expectedHelp.readLines().each {
            assert help.contains(it)
            resultHelp.removeElement(it)
        }
        assert resultHelp.size() == 0, "Found extra unexpected lines: ${resultHelp}"
    }

    def 'should get an after text' () {
        given:
        def validationConfig = [
            monochromeLogs: true,
            parametersSchema: 'src/testResources/nextflow_schema.json',
            help: [
                enabled: true,
                afterText: """Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas condimentum ligula ac metus sollicitudin rutrum. Vestibulum a lectus ipsum. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Cras consequat placerat aliquet. Maecenas et vulputate nibh. Donec luctus, purus ut scelerisque ornare, sem nisl mollis libero, non faucibus nibh nunc ac nulla. Donec et pharetra neque. Etiam id nibh vel turpis ornare efficitur. Cras eu eros mi.
Etiam at nulla ac dui ullamcorper viverra. Donec posuere imperdiet eros nec consequat. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Nullam nec aliquam magna. Quisque nec dapibus velit, id convallis justo. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Suspendisse bibendum ipsum quis nulla fringilla laoreet. Integer dictum, purus et pretium ultrices, nunc nisl vestibulum erat, et tempus ex massa eget nunc. Interdum et malesuada fames ac ante ipsum primis in faucibus. Integer varius aliquam vestibulum. Proin sit amet lobortis ipsum. Vestibulum fermentum lorem ac erat pharetra, eu eleifend sapien hendrerit. Quisque id varius ex. Morbi et dui et libero varius tempus. Ut eu sagittis lorem, sed congue libero.
"""
            ]
        ]
        def params = [:]
        def config = new ValidationConfig(validationConfig, params)
        def helpMessage = new HelpMessage(config, session)

        when:
        def help = helpMessage.getAfterText()

        then:
        noExceptionThrown()
        def expectedHelp = """------------------------------------------------------
Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas condimentum ligula ac metus sollicitudin rutrum. Vestibulum a lectus ipsum. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Cras consequat placerat aliquet. Maecenas et vulputate nibh. Donec luctus, purus ut scelerisque ornare, sem nisl mollis libero, non faucibus nibh nunc ac nulla. Donec et pharetra neque. Etiam id nibh vel turpis ornare efficitur. Cras eu eros mi.
Etiam at nulla ac dui ullamcorper viverra. Donec posuere imperdiet eros nec consequat. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Nullam nec aliquam magna. Quisque nec dapibus velit, id convallis justo. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Suspendisse bibendum ipsum quis nulla fringilla laoreet. Integer dictum, purus et pretium ultrices, nunc nisl vestibulum erat, et tempus ex massa eget nunc. Interdum et malesuada fames ac ante ipsum primis in faucibus. Integer varius aliquam vestibulum. Proin sit amet lobortis ipsum. Vestibulum fermentum lorem ac erat pharetra, eu eleifend sapien hendrerit. Quisque id varius ex. Morbi et dui et libero varius tempus. Ut eu sagittis lorem, sed congue libero.

"""
        def resultHelp = help.readLines()
        expectedHelp.readLines().each {
            assert help.contains(it)
            resultHelp.removeElement(it)
        }
        assert resultHelp.size() == 0, "Found extra unexpected lines: ${resultHelp}"
    }

    def 'should get a short help message with after text' () {
        given:
        def validationConfig = [
            monochromeLogs: true,
            parametersSchema: 'src/testResources/nextflow_schema.json',
            help: [
                enabled: true
            ]
        ]
        def params = [:]
        def config = new ValidationConfig(validationConfig, params)
        def helpMessage = new HelpMessage(config, session)

        when:
        def help = helpMessage.getShortHelpMessage("") + helpMessage.getAfterText()

        then:
        noExceptionThrown()
        def expectedHelp = """--help            [boolean, string] Show the help message for all top level parameters. When a parameter is given to `--help`, the full 
help message of that parameter will be printed. 
--helpFull        [boolean]         Show the help message for all non-hidden parameters. 
--showHidden      [boolean]         Show all hidden parameters in the help message. This needs to be used in combination with `--help` 
or `--helpFull`. 

Input/output options
  --input         [string] Path to comma-separated file containing information about the samples in the experiment. 
  --outdir        [string] The output directory where the results will be saved. You have to use absolute paths to storage on 
Cloud infrastructure. 
  --email         [string] Email address for completion summary. 
  --multiqc_title [string] MultiQC report title. Printed as page header, used for filename if not otherwise specified. 

Reference genome options
  --genome        [string] Name of iGenomes reference. 
  --fasta         [string] Path to FASTA genome file. 

 !! Hiding 22 param(s), use the `--showHidden` parameter to show them !!
------------------------------------------------------

"""
        def resultHelp = help.readLines()
        expectedHelp.readLines().each {
            assert help.contains(it)
            resultHelp.removeElement(it)
        }
        assert resultHelp.size() == 0, "Found extra unexpected lines: ${resultHelp}"
    }

}