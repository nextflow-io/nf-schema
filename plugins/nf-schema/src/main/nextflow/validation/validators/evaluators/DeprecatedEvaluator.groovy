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
class DeprecatedEvaluator implements Evaluator {
    // Checks if the use of this value is deprecated

    private final Boolean deprecated

    DeprecatedEvaluator(Boolean deprecated) {
        this.deprecated = deprecated
    }

    @Override
    public Evaluator.Result evaluate(EvaluationContext ctx, JsonNode node) {
        // Checks if the value should be deprecated
        if (!this.deprecated) {
            return Evaluator.Result.success()
        }

        return Evaluator.Result.failure("This option is deprecated")

    }
}