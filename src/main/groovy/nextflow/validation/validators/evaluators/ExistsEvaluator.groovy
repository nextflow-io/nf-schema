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

    private final Boolean shouldExist // true if the file should exist, false if it should not

    ExistsEvaluator(Boolean exists) {
        this.shouldExist = exists
    }

    @Override
    public Evaluator.Result evaluate(EvaluationContext ctx, JsonNode node) {
        // To stay consistent with other keywords, types not applicable to this keyword should succeed
        if (!node.isString()) {
            return Evaluator.Result.success()
        }

        def String value = node.asString()
        def Boolean exists
        def Path file

        try {
            file = Nextflow.file(value) as Path
            exists = file.exists()
        } catch (Exception e) {
            return Evaluator.Result.failure("could not check existence of '${value}': ${e.message}" as String)
        }

        // Don't evaluate file path patterns
        if (file instanceof List) {
            return Evaluator.Result.success()
        }

        if (!exists && this.shouldExist == true) {
            return Evaluator.Result.failure("the file or directory '${value}' does not exist" as String)
        } else if(exists && this.shouldExist == false) {
            return Evaluator.Result.failure("the file or directory '${value}' should not exist" as String)
        }
        return Evaluator.Result.success()
    }
}