package nextflow.validation.validators.evaluators

import static java.util.Collections.singleton
import static java.util.Collections.unmodifiableList

import dev.harrel.jsonschema.Evaluator
import dev.harrel.jsonschema.EvaluationContext
import dev.harrel.jsonschema.JsonNode
import dev.harrel.jsonschema.SimpleType

import groovy.util.logging.Slf4j
import groovy.transform.CompileDynamic
import java.util.stream.Collectors

/**
 * The evaluator to validate types in a lenient way
 *
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */

@Slf4j
@CompileDynamic
class LenientTypeEvaluator implements Evaluator {

    private final Set<SimpleType> types
    private final List<SimpleType> lenientTypes = [
        SimpleType.STRING,
        SimpleType.INTEGER,
        SimpleType.NUMBER,
        SimpleType.BOOLEAN,
        SimpleType.NULL
    ]

    LenientTypeEvaluator(JsonNode node) {
        if (node.string) {
            this.types = singleton(SimpleType.fromName(node.asString()))
        } else {
            this.types = node.asArray().stream()
                    .map(JsonNode::asString)
                    .map(SimpleType::fromName)
                    .collect(Collectors.toSet())
        }
    }

    @Override
    Result evaluate(EvaluationContext ctx, JsonNode node) {
        SimpleType nodeType = node.nodeType
        if (types.contains(SimpleType.STRING) && lenientTypes.contains(nodeType)) {
            return Result.success()
        }
        if (types.contains(nodeType) || nodeType == SimpleType.INTEGER && types.contains(SimpleType.NUMBER)) {
            return Result.success()
        }
        List<String> typeNames = unmodifiableList(
            types.stream().map(SimpleType::getName).collect(Collectors.toList())
        )
        return Result.failure(String.format('Value is [%s] but should be %s', nodeType.name, typeNames))
    }

}
