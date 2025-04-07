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
        def Path file

        try {
            file = Nextflow.file(value) as Path
            if (!(file instanceof List)) {
                file.exists() // Do an exists check to see if the file can be correctly accessed
            }
        } catch (Exception e) {
            return Evaluator.Result.failure("could not validate file format of '${value}': ${e.message}" as String)
        }

        // Actual validation logic
        if (file instanceof List) {
            return Evaluator.Result.failure("'${value}' is not a file, but a file path pattern" as String)
        }
        if (file.exists() && file.isDirectory()) {
            return Evaluator.Result.failure("'${value}' is not a file, but a directory" as String)
        }
        return Evaluator.Result.success()
    }
}