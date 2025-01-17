package nextflow.validation.exceptions

import groovy.transform.CompileStatic
import nextflow.exception.AbortOperationException
/**
 * Exception thrown to notify issues with the samplesheet creation
 *
 * @author Nicolas Vannieuwkerke <nicolas.vannieuwkerke@gmail.com>
 */
@CompileStatic
class SamplesheetCreationException extends AbortOperationException {

    SamplesheetCreationException(String message) {
        super(message)
    }
}
