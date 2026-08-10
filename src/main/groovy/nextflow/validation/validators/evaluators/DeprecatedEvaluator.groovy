package nextflow.validation.validators.evaluators

import dev.harrel.jsonschema.Evaluator
import dev.harrel.jsonschema.EvaluationContext
import dev.harrel.jsonschema.JsonNode

import groovy.util.logging.Slf4j
import groovy.transform.CompileDynamic

/**
 * Checks if the use of this value is deprecated
 *
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */

@Slf4j
@CompileDynamic
class DeprecatedEvaluator implements Evaluator {

    private final Boolean deprecated

    DeprecatedEvaluator(Boolean deprecated) {
        this.deprecated = deprecated
    }

    @Override
    Evaluator.Result evaluate(EvaluationContext ctx, JsonNode node) {
        // Checks if the value should be deprecated
        if (!this.deprecated) {
            return Evaluator.Result.success()
        }

        return Evaluator.Result.failure('This option is deprecated')
    }

}
