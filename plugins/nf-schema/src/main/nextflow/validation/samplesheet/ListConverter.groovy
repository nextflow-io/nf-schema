package nextflow.validation.samplesheet

import groovy.util.logging.Slf4j
import java.nio.file.Path

import nextflow.validation.config.ValidationConfig

/**
 *
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */

@Slf4j
class ListConverter {

    final private ValidationConfig config

    ListConverter(ValidationConfig config) {
        this.config = config
    }

    public void validateAndConvertToSamplesheet(
        List inputList,
        Path samplesheet,
        Path schema
    ) {
        println("Hi")
    }
}
