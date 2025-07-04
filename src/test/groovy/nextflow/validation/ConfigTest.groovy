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

import nextflow.validation.config.ValidationConfig

/**
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */
class ConfigTest extends Dsl2Spec{

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

    def 'test valid config' () {
        given:
        def config = [
            lenientMode: true,
            monochromeLogs: true,
            maxErrValSize: 20,
            parametersSchema: 'src/testResources/nextflow_schema.json',
            ignoreParams: ['some_random_param'],
            help: [
                enabled: true,
                showHidden: true,
                showHiddenParameter: 'stopHiding',
                shortParameter: 'short',
                fullParameter: 'full',
                beforeText: 'before',
                afterText: 'after',
                command: 'command'
            ],
            summary: [
                beforeText: 'before',
                afterText: 'after',
                hideParams: ['some_random_param'],
            ],
            logging: [
                unrecognisedParams: 'error',
                unrecognisedHeaders: 'error'
            ]
        ]
        def params = [:]

        when:
        new ValidationConfig(config, params)
        def stdout = capture
            .toString()
            .readLines()
            .findResults { it.contains('WARN') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'test valid config - GStrings' () {
        given:
        def randomString = 'randomString'
        def errorLevel = 'error'
        def config = [
            lenientMode: true,
            monochromeLogs: true,
            maxErrValSize: 20,
            parametersSchema: "${randomString}",
            ignoreParams: ["${randomString}"],
            help: [
                enabled: true,
                showHidden: true,
                showHiddenParameter: "${randomString}",
                shortParameter: "${randomString}",
                fullParameter: "${randomString}",
                beforeText: "${randomString}",
                afterText: "${randomString}",
                command: "${randomString}"
            ],
            summary: [
                beforeText: "${randomString}",
                afterText: "${randomString}",
                hideParams: ["${randomString}"],
            ],
            logging: [
                unrecognisedParams: "${errorLevel}",
                unrecognisedHeaders: "${errorLevel}"
            ]
        ]
        def params = [:]

        when:
        new ValidationConfig(config, params)
        def stdout = capture
            .toString()
            .readLines()
            .findResults { it.contains('WARN') ? it : null }

        then:
        noExceptionThrown()
        !stdout
    }

    def 'test invalid config' () {
        given:
        def config = [
            lenientMode: 'notABoolean',
            monochromeLogs: 12,
            showHiddenParams: 'notABoolean',
            maxErrValSize: ["notAnInteger"],
            parametersSchema: 42,
            ignoreParams: true,
            help: [
                enabled: 'notABoolean',
                showHidden: 'notABoolean',
                showHiddenParameter: 123456789,
                shortParameter: false,
                fullParameter: [im:'a_map'],
                beforeText: true,
                afterText: ['im','a','list'],
                command: 0
            ],
            summary: [
                beforeText: 63,
                afterText: false,
                hideParams: 'randomString',
            ],
            logging: [
                unrecognisedParams: true,
                unrecognisedHeaders: 589654
            ]
        ]
        def params = [:]

        when:
        new ValidationConfig(config, params)
        def stdout = capture
            .toString()
            .readLines()
            .findResults { it.contains('WARN') ? it : null }

        then:
        noExceptionThrown()
        stdout
    }
}
