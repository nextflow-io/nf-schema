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
 * Validation of params that hold a dataflow value (a typed `Channel` or `Value` param), where the value
 * the param was created from is validated in place of the dataflow object. Nextflow keeps that value in
 * the params scope of the config, which also holds the values given on the command line and in a params
 * file. The session is mocked so that the config params can be set directly.
 */
@CompileDynamic
@Timeout(60)
class ValidateDataflowParamsTest extends Specification {

    private static final String SCHEMA = 'src/testResources/nextflow_schema.json'
    private static final String NESTED_SCHEMA = 'src/testResources/nextflow_schema_nested_parameters.json'

    void 'should accept a valid value of a dataflow param'() {
        given:
        Session session = mockSession(topLevelParams(new DataflowVariable()), [input: 'src/testResources/correct.csv'])

        when:
        validate(session, SCHEMA)

        then:
        noExceptionThrown()
    }

    void 'should reject an invalid value of a dataflow param'() {
        given:
        Session session = mockSession(topLevelParams(new DataflowVariable()), [input: 'src/testResources/correct.txt'])

        when:
        validate(session, SCHEMA)

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message.contains('--input (src/testResources/correct.txt)')
    }

    void 'should accept a valid value of a Value param'() {
        given:
        Session session = mockSession(topLevelParams(new ValueImpl(new DataflowVariable())), [input: 'src/testResources/correct.csv'])

        when:
        validate(session, SCHEMA)

        then:
        noExceptionThrown()
    }

    void 'should reject an invalid value of a Value param'() {
        given:
        Session session = mockSession(topLevelParams(new ValueImpl(new DataflowVariable())), [input: 'src/testResources/correct.txt'])

        when:
        validate(session, SCHEMA)

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message.contains('--input (src/testResources/correct.txt)')
    }

    void 'should accept a valid value of a Channel param'() {
        given:
        Session session = mockSession(topLevelParams(new ChannelImpl(new DataflowQueue())), [input: 'src/testResources/correct.csv'])

        when:
        validate(session, SCHEMA)

        then:
        noExceptionThrown()
    }

    void 'should reject an invalid value of a Channel param'() {
        given:
        Session session = mockSession(topLevelParams(new ChannelImpl(new DataflowQueue())), [input: 'src/testResources/correct.txt'])

        when:
        validate(session, SCHEMA)

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message.contains('--input (src/testResources/correct.txt)')
    }

    void 'should accept a valid value of a dataflow value nested in a record param'() {
        given:
        Session session = mockSession(
            [map: [is: [so: [deep: new ValueImpl(new DataflowVariable())]]]],
            [map: [is: [so: [deep: true]]]]
        )

        when:
        validate(session, NESTED_SCHEMA)

        then:
        noExceptionThrown()
    }

    void 'should reject an invalid value of a dataflow value nested in a record param'() {
        given:
        Session session = mockSession(
            [map: [is: [so: [deep: new ValueImpl(new DataflowVariable())]]]],
            [map: [is: [so: [deep: 'maybe']]]]
        )

        when:
        validate(session, NESTED_SCHEMA)

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message.contains('--map.is.so.deep')
    }

    private Session mockSession(Map params, Map configParams) {
        Session session = Mock(Session)
        session.params >> params
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
