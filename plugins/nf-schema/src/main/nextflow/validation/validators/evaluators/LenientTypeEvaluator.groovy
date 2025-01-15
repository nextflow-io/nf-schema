package nextflow.validation.validators.evaluators

import dev.harrel.jsonschema.Evaluator
import dev.harrel.jsonschema.EvaluationContext
import dev.harrel.jsonschema.JsonNode
import dev.harrel.jsonschema.SimpleType
import nextflow.Nextflow

import groovy.util.logging.Slf4j
import java.nio.file.Path
import java.util.stream.Collectors
import static java.util.Collections.singleton
import static java.util.Collections.unmodifiableList

/**
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */

@Slf4j
class LenientTypeEvaluator implements Evaluator {
    // Validate against the type

    private final Set<SimpleType> types
    private final List<SimpleType> lenientTypes = [
        SimpleType.STRING, 
        SimpleType.INTEGER, 
        SimpleType.NUMBER, 
        SimpleType.BOOLEAN,
        SimpleType.NULL
    ]

    LenientTypeEvaluator(JsonNode node) {
        if (node.isString()) {
            this.types = singleton(SimpleType.fromName(node.asString()))
        } else {
            this.types = node.asArray().stream()
                    .map(JsonNode::asString)
                    .map(SimpleType::fromName)
                    .collect(Collectors.toSet())
        }
    }

    @Override
    public Result evaluate(EvaluationContext ctx, JsonNode node) {
        def SimpleType nodeType = node.getNodeType()
        if (types.contains(SimpleType.STRING) && lenientTypes.contains(nodeType)) {
            return Result.success()
        }
        if (types.contains(nodeType) || nodeType == SimpleType.INTEGER && types.contains(SimpleType.NUMBER)) {
            return Result.success()
        } else {
            def List<String> typeNames = unmodifiableList(types.stream().map(SimpleType::getName).collect(Collectors.toList()))
            return Result.failure(String.format("Value is [%s] but should be %s", nodeType.getName(), typeNames))
        }
    }
}
