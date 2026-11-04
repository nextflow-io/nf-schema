/* groovylint-disable GStringExpressionWithinString, MethodName */
package nextflow.validation.utils

import groovy.transform.CompileDynamic
import spock.lang.Specification

/**
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */

@CompileDynamic
class GroovyVariablesTest extends Specification {

    void 'should correctly substitute one variable'() {
        given:
        Binding binding = new Binding(['test': 'value'])
        String expression = '${test}'

        when:
        String result = GroovyVariables.evaluate(expression, binding)

        then:
        noExceptionThrown()
        assert result == 'value'
    }

    void 'should correctly substitute nested variable'() {
        given:
        Binding binding = new Binding(['test': [nested: 'value']])
        String expression = '${test.nested}'

        when:
        String result = GroovyVariables.evaluate(expression, binding)

        then:
        noExceptionThrown()
        assert result == 'value'
    }

    void 'should not substitute missing variable'() {
        given:
        Binding binding = new Binding(['test': 'value'])
        String expression = '${missing}'

        when:
        String result = GroovyVariables.evaluate(expression, binding)

        then:
        noExceptionThrown()
        assert result == '${missing}'
    }

    void 'should correctly substitute multiple variables'() {
        given:
        Binding binding = new Binding(['test1': 'value1', 'test2': 'value2'])
        String expression = '${test1} and ${test2}'

        when:
        String result = GroovyVariables.evaluate(expression, binding)

        then:
        noExceptionThrown()
        assert result == 'value1 and value2'
    }

    void 'should not run method calls'() {
        given:
        Binding binding = new Binding(['test': 'value'])
        String expression = '${test.toUpperCase()}'

        when:
        String result = GroovyVariables.evaluate(expression, binding)

        then:
        noExceptionThrown()
        assert result == '${test.toUpperCase()}'
    }

    void 'should not run constructor calls'() {
        given:
        Binding binding = new Binding(['test': 'value'])
        String expression = '${new String("test")}'

        when:
        String result = GroovyVariables.evaluate(expression, binding)

        then:
        noExceptionThrown()
        assert result == '${new String("test")}'
    }

    void 'should not run static method calls'() {
        given:
        Binding binding = new Binding(['test': 'value'])
        String expression = '${String.valueOf(test)}'

        when:
        String result = GroovyVariables.evaluate(expression, binding)

        then:
        noExceptionThrown()
        assert result == '${String.valueOf(test)}'
    }

    void 'should not run with weird variable names'() {
        given:
        Binding binding = new Binding(['String': ['toUpperCase()': 'value']])
        String expression = '${String.toUpperCase()}'

        when:
        String result = GroovyVariables.evaluate(expression, binding)

        then:
        noExceptionThrown()
        assert result == '${String.toUpperCase()}'
    }

}
