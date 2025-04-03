package nextflow.validation.validators.evaluators

import dev.harrel.jsonschema.Evaluator
import dev.harrel.jsonschema.EvaluationContext
import dev.harrel.jsonschema.JsonNode
import nextflow.Nextflow

import groovy.util.logging.Slf4j
import java.nio.file.Path

/**
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */

@Slf4j
class ExistsEvaluator implements Evaluator {
    // The file should or should not exist

    private final Boolean exists

    ExistsEvaluator(Boolean exists) {
        this.exists = exists
    }

    @Override
    public Evaluator.Result evaluate(EvaluationContext ctx, JsonNode node) {
        // To stay consistent with other keywords, types not applicable to this keyword should succeed
        if (!node.isString()) {
            return Evaluator.Result.success()
        }

        def String value = node.asString()

        // Skip validation of S3 paths for now
        if (value.startsWith('s3://') || value.startsWith('az://') || value.startsWith('gs://')) {
            log.debug("S3 paths are not supported by 'ExistsEvaluator': '${value}'")
            return Evaluator.Result.success()
        }
       
        // Actual validation logic
        def Path file = Nextflow.file(value) as Path

        // Don't evaluate file path patterns
        if (file instanceof List) {
            return Evaluator.Result.success()
        }

        if (!file.exists() && this.exists == true) {
            return Evaluator.Result.failure("the file or directory '${value}' does not exist" as String)
        } else if(file.exists() && this.exists == false) {
            return Evaluator.Result.failure("the file or directory '${value}' should not exist" as String)
        }
        return Evaluator.Result.success()
    }
}