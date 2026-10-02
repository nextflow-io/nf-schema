/* groovylint-disable LineLength, MethodName */
package nextflow.validation

import groovy.transform.CompileDynamic
import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.DataflowVariable
import nextflow.Session
import nextflow.dataflow.ChannelImpl
import nextflow.dataflow.ValueImpl
import nextflow.validation.config.ValidationConfig
import nextflow.validation.exceptions.SchemaValidationException
import nextflow.validation.parameters.ParameterValidator
import spock.lang.Specification
import spock.lang.Timeout

import java.nio.file.Path

/**
 * Validation of params that hold a dataflow value (a typed `Channel` or `Value` param), where the
 * value given on the command line or in the config is validated in place of the dataflow object.
 * The session is mocked so that the command line params can be set directly.
 */
@CompileDynamic
@Timeout(60)
class ValidateDataflowParamsTest extends Specification {

    private static final String SCHEMA = 'src/testResources/nextflow_schema.json'
    private static final String NESTED_SCHEMA = 'src/testResources/nextflow_schema_nested_parameters.json'

    void 'should accept a valid command line value for a dataflow param'() {
        given:
        Session session = mockSession(topLevelParams(new DataflowVariable()), [input: 'src/testResources/correct.csv'], [:])

        when:
        validate(session, SCHEMA)

        then:
        noExceptionThrown()
    }

    void 'should reject an invalid command line value for a dataflow param'() {
        given:
        Session session = mockSession(topLevelParams(new DataflowVariable()), [input: 'src/testResources/correct.txt'], [:])

        when:
        validate(session, SCHEMA)

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message.contains('--input (src/testResources/correct.txt)')
    }

    void 'should validate the command line value over the config value - invalid command line value'() {
        given:
        Session session = mockSession(
            topLevelParams(new DataflowVariable()),
            [input: 'src/testResources/correct.txt'],
            [input: 'src/testResources/correct.csv']
        )

        when:
        validate(session, SCHEMA)

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message.contains('--input (src/testResources/correct.txt)')
    }

    void 'should validate the command line value over the config value - valid command line value'() {
        given:
        Session session = mockSession(
            topLevelParams(new DataflowVariable()),
            [input: 'src/testResources/correct.csv'],
            [input: 'src/testResources/correct.txt']
        )

        when:
        validate(session, SCHEMA)

        then:
        noExceptionThrown()
    }

    void 'should accept a valid command line value for a Value param'() {
        given:
        Session session = mockSession(topLevelParams(new ValueImpl(new DataflowVariable())), [input: 'src/testResources/correct.csv'], [:])

        when:
        validate(session, SCHEMA)

        then:
        noExceptionThrown()
    }

    void 'should reject an invalid command line value for a Value param'() {
        given:
        Session session = mockSession(topLevelParams(new ValueImpl(new DataflowVariable())), [input: 'src/testResources/correct.txt'], [:])

        when:
        validate(session, SCHEMA)

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message.contains('--input (src/testResources/correct.txt)')
    }

    void 'should accept a valid command line value for a Channel param'() {
        given:
        Session session = mockSession(topLevelParams(new ChannelImpl(new DataflowQueue())), [input: 'src/testResources/correct.csv'], [:])

        when:
        validate(session, SCHEMA)

        then:
        noExceptionThrown()
    }

    void 'should reject an invalid command line value for a Channel param'() {
        given:
        Session session = mockSession(topLevelParams(new ChannelImpl(new DataflowQueue())), [input: 'src/testResources/correct.txt'], [:])

        when:
        validate(session, SCHEMA)

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message.contains('--input (src/testResources/correct.txt)')
    }

    void 'should accept a valid command line value for a dataflow value nested in a record param'() {
        given:
        Session session = mockSession(
            [map: [is: [so: [deep: new ValueImpl(new DataflowVariable())]]]],
            [map: [is: [so: [deep: true]]]],
            [:]
        )

        when:
        validate(session, NESTED_SCHEMA)

        then:
        noExceptionThrown()
    }

    void 'should reject an invalid command line value for a dataflow value nested in a record param'() {
        given:
        Session session = mockSession(
            [map: [is: [so: [deep: new ValueImpl(new DataflowVariable())]]]],
            [map: [is: [so: [deep: 'maybe']]]],
            [:]
        )

        when:
        validate(session, NESTED_SCHEMA)

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message.contains('--map.is.so.deep')
    }

    private Session mockSession(Map params, Map cliParams, Map configParams) {
        Session session = Mock(Session)
        session.params >> params
        session.cliParams >> cliParams
        session.config >> [params: configParams]
        session.baseDir >> Path.of('.').toAbsolutePath()
        return session
    }

    private void validate(Session session, String schema) {
        ValidationConfig config = new ValidationConfig([monochromeLogs: true], session)
        new ParameterValidator(config).validateParametersMap(
            [parameters_schema: Path.of(schema).toAbsolutePath().toString()],
            session
        )
    }

    private Map topLevelParams(Object input) {
        return [input: input, outdir: 'src/testResources/testDir']
    }

}
