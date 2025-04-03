package nextflow.validation.validators.evaluators

import dev.harrel.jsonschema.Evaluator
import dev.harrel.jsonschema.EvaluationContext
import dev.harrel.jsonschema.JsonNode

import groovy.util.logging.Slf4j

/**
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */

@Slf4j
class UniqueEntriesEvaluator implements Evaluator {
    // Combinations of these columns should be unique

    private final List<String> uniqueEntries

    UniqueEntriesEvaluator(List<JsonNode> uniqueEntries) {
        this.uniqueEntries = uniqueEntries.collect { it.asString() }
    }

    @Override
    public Evaluator.Result evaluate(EvaluationContext ctx, JsonNode node) {
        // To stay consistent with other keywords, types not applicable to this keyword should succeed
        if (!node.isArray()) {
            return Evaluator.Result.success()
        }

        def Set<Tuple> uniques = []
        def Integer count = 0
        for(nodeEntry : node.asArray()) {
            count++
            if(!nodeEntry.isObject()) {
                return Evaluator.Result.success()
            }
            def Map filteredNodes = nodeEntry.asObject().subMap(uniqueEntries)
            def Tuple nodeTup = filteredNodes ? Tuple.tuple(*filteredNodes.collect{k, v -> "${k}:${v.asString()}"}) : Tuple.tuple()
            if(nodeTup && nodeTup in uniques) {
                return Evaluator.Result.failure("Entry ${count}: Detected duplicate entries: ${nodeTup}" as String)
            }
            uniques << nodeTup
        }

        return Evaluator.Result.success()
    }
}
