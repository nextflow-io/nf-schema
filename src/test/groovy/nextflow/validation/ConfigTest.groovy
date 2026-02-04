/* groovylint-disable LineLength, MethodName, TrailingWhitespace */
package nextflow.validation

import groovy.transform.CompileDynamic

import nextflow.Session
import org.junit.Rule
import test.Dsl2Spec
import test.OutputCapture

import nextflow.validation.config.ValidationConfig

/**
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */

@CompileDynamic
class ConfigTest extends Dsl2Spec {

    @Rule
    final private OutputCapture capture = new OutputCapture()

    final private Session session = Mock(Session)

    void 'test valid config'() {
        given:
        Map config = [
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

        when:
        new ValidationConfig(config, session)
        List<String> stdout = capture
            .toString()
            .readLines()
            .findResults { line -> line.contains('WARN') ? line : null }

        then:
        noExceptionThrown()
        !stdout
    }

    void 'test valid config - GStrings'() {
        given:
        String randomString = 'randomString'
        String errorLevel = 'error'
        Map config = [
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

        when:
        new ValidationConfig(config, session)
        List<String> stdout = capture
            .toString()
            .readLines()
            .findResults { line -> line.contains('WARN') ? line : null }

        then:
        noExceptionThrown()
        !stdout
    }

    void 'test invalid config'() {
        given:
        Map config = [
            lenientMode: 'notABoolean',
            monochromeLogs: 12,
            showHiddenParams: 'notABoolean',
            maxErrValSize: ['notAnInteger'],
            parametersSchema: 42,
            ignoreParams: true,
            help: [
                enabled: 'notABoolean',
                showHidden: 'notABoolean',
                showHiddenParameter: 123456789,
                shortParameter: false,
                fullParameter: [im:'a_map'],
                beforeText: true,
                afterText: ['im', 'a', 'list'],
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

        when:
        new ValidationConfig(config, session)
        List<String> stdout = capture
            .toString()
            .readLines()
            .findResults { line -> line.contains('WARN') ? line : null }

        then:
        noExceptionThrown()
        stdout
    }

}
