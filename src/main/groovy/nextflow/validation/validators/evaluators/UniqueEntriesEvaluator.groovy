package nextflow.validation.validators.evaluators

import dev.harrel.jsonschema.Evaluator
import dev.harrel.jsonschema.EvaluationContext
import dev.harrel.jsonschema.JsonNode

import groovy.util.logging.Slf4j
import groovy.transform.CompileDynamic

/**
 * The evaluator to validate that entries in an array are unique based on specified fields
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */

@Slf4j
@CompileDynamic
class UniqueEntriesEvaluator implements Evaluator {

    final private List<String> uniqueEntries

    UniqueEntriesEvaluator(List<JsonNode> uniqueEntries) {
        this.uniqueEntries = uniqueEntries*.asString()
    }

    @Override
    Evaluator.Result evaluate(EvaluationContext ctx, JsonNode node) {
        // To stay consistent with other keywords, types not applicable to this keyword should succeed
        if (!node.array) {
            return Evaluator.Result.success()
        }

        Set<Tuple> uniques = []
        Integer count = 0
        Evaluator.Result result = Evaluator.Result.success()
        node.asArray().any { nodeEntry ->
            count++
            if (!nodeEntry.object) {
                return true
            }
            Map filteredNodes = nodeEntry.asObject().subMap(uniqueEntries)
            Tuple nodeTup = filteredNodes ?
                Tuple.tuple(*filteredNodes.collect { k, v -> "${k}:${v.asString()}" }) :
                Tuple.tuple()
            if (nodeTup && nodeTup in uniques) {
                result = Evaluator.Result.failure("Entry ${count}: Detected duplicate entries: ${nodeTup}" as String)
                return true
            }
            uniques << nodeTup
            return false
        }
        return result
    }
}
