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
class FormatFilePathEvaluator implements Evaluator {
    // The string should be a file
  
    @Override
    public Evaluator.Result evaluate(EvaluationContext ctx, JsonNode node) {
        // To stay consistent with other keywords, types not applicable to this keyword should succeed
        if (!node.isString()) {
            return Evaluator.Result.success()
        }

        def String value = node.asString()

        // Skip validation of S3 paths for now
        if (value.startsWith('s3://') || value.startsWith('az://') || value.startsWith('gs://')) {
            log.debug("S3 paths are not supported by 'FormatFilePathEvaluator': '${value}'")
            return Evaluator.Result.success()
        }
       
        // Actual validation logic
        def Path file = Nextflow.file(value) as Path
        if (file instanceof List) {
            return Evaluator.Result.failure("'${value}' is not a file, but a file path pattern" as String)
        }
        if (file.exists() && file.isDirectory()) {
            return Evaluator.Result.failure("'${value}' is not a file, but a directory" as String)
        }
        return Evaluator.Result.success()
    }
}