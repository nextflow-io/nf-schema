/* groovylint-disable LineLength, MethodName */
package nextflow.validation

import groovy.transform.CompileDynamic
import nextflow.Session
import nextflow.validation.config.ValidationConfig
import nextflow.validation.exceptions.SchemaValidationException
import nextflow.validation.parameters.ParameterValidator
import spock.lang.Specification

import java.nio.file.Path

/**
 * Validation of the values given on the command line, which are cast to the type they have.
 * The session is mocked so that the command line params can be set directly.
 */
@CompileDynamic
class ValidateCliParamsTest extends Specification {

    private static final String SCHEMA = 'src/testResources/nextflow_schema_nested_parameters.json'

    void 'should accept a nested value given on the command line'() {
        given:
        Session session = mockSession([map: [is: [so: [deep: 'true']]]], [map: [is: [so: [deep: 'true']]]])

        when:
        validate(session)

        then:
        noExceptionThrown()
    }

    void 'should reject a nested value given on the command line that is not of the type of the schema'() {
        given:
        Session session = mockSession([map: [is: [so: [deep: 'maybe']]]], [map: [is: [so: [deep: 'maybe']]]])

        when:
        validate(session)

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message.contains('--map.is.so.deep (maybe): Value is [string] but should be [boolean]')
    }

    void 'should not cast a nested value that was not given on the command line'() {
        given:
        Session session = mockSession([map: [is: [so: [deep: 'true']]]], [:])

        when:
        validate(session)

        then:
        SchemaValidationException error = thrown(SchemaValidationException)
        error.message.contains('--map.is.so.deep (true): Value is [string] but should be [boolean]')
    }

    private Session mockSession(Map params, Map cliParams) {
        Session session = Mock(Session)
        session.params >> params
        session.cliParams >> cliParams
        session.config >> [params: params]
        session.baseDir >> Path.of('.').toAbsolutePath()
        return session
    }

    private void validate(Session session) {
        ValidationConfig config = new ValidationConfig([monochromeLogs: true], session)
        new ParameterValidator(config).validateParametersMap(
            [parameters_schema: Path.of(SCHEMA).toAbsolutePath().toString(), cast_cli_params: true],
            session
        )
    }

}
