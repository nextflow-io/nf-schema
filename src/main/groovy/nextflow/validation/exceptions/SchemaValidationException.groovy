package nextflow.validation.exceptions

import groovy.transform.CompileStatic
import nextflow.exception.AbortOperationException
/**
 * Exception thrown to notify invalid input schema validation
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@CompileStatic
class SchemaValidationException extends AbortOperationException {

    private List<String> errors

    List<String> getErrors() { errors }

    SchemaValidationException(String message, List<String> errors=[]) {
        super(message)
        this.errors = new ArrayList<>(errors)
    }
}
