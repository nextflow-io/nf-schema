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
class FormatFilePathPatternEvaluator implements Evaluator {
    // The string should be a path pattern
  
    @Override
    public Evaluator.Result evaluate(EvaluationContext ctx, JsonNode node) {
        // To stay consistent with other keywords, types not applicable to this keyword should succeed
        if (!node.isString()) {
            return Evaluator.Result.success()
        }

        def String value = node.asString()

        // Skip validation of S3 paths for now
        if (value.startsWith('s3://') || value.startsWith('az://') || value.startsWith('gs://')) {
            log.debug("S3 paths are not supported by 'FormatFilePathPatternEvaluator': '${value}'")
            return Evaluator.Result.success()
        }

        // Actual validation logic
        def List<Path> files = Nextflow.files(value)
        def List<String> errors = []

        if(files.size() == 0) {
            return Evaluator.Result.failure("No files were found using the glob pattern '${value}'" as String)
        }
        for( file : files ) {
            if (file.isDirectory()) {
                errors.add("'${file.toString()}' is not a file, but a directory" as String)
            }
        }
        if(errors.size() > 0) {
            return Evaluator.Result.failure(errors.join('\n'))
        }
        return Evaluator.Result.success()
    }
}