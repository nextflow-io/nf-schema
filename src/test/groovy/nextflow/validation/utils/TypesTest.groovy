/* groovylint-disable LineLength, MethodName */
package nextflow.validation.utils

import static nextflow.validation.utils.Types.castCliValues

import groovy.transform.CompileDynamic
import spock.lang.Specification

/**
 * Casting of the values given on the command line.
 */
@CompileDynamic
class TypesTest extends Specification {

    void 'should cast the values given on the command line'() {
        expect:
        castCliValues(params, cliParams) == expected

        where:
        params                                  | cliParams                      | expected
        [flag: 'true', count: '3']              | [flag: 'true', count: '3']     | [flag: true, count: 3]
        [ratio: '0.5', name: 'abc']             | [ratio: '0.5', name: 'abc']    | [ratio: 0.5, name: 'abc']
        [flag: 'true']                          | [:]                            | [flag: 'true']
        [flag: 'true']                          | null                           | [flag: 'true']
        [flag: 'true', other: 'false']          | [flag: 'true']                 | [flag: true, other: 'false']
        [flag: true, count: 3]                  | [flag: 'true', count: '3']     | [flag: true, count: 3]
    }

    void 'should cast the nested values given on the command line'() {
        expect:
        castCliValues(params, cliParams) == expected

        where:
        params                                      | cliParams                           | expected
        [group: [flag: 'true', count: '3']]         | [group: [flag: 'true', count: '3']] | [group: [flag: true, count: 3]]
        [group: [flag: 'true', other: 'false']]     | [group: [flag: 'true']]             | [group: [flag: true, other: 'false']]
        [group: [is: [so: [deep: 'true']]]]         | [group: [is: [so: [deep: 'true']]]] | [group: [is: [so: [deep: true]]]]
        [group: [flag: 'true']]                     | [:]                                 | [group: [flag: 'true']]
        [group: [flag: 'true']]                     | [other: 'true']                     | [group: [flag: 'true']]
    }

    void 'should not cast a nested value with the name of a top-level command line parameter'() {
        expect:
        castCliValues([flag: 'true', group: [flag: 'true']], [flag: 'true']) == [flag: true, group: [flag: 'true']]
    }

}
