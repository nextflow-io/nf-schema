package nextflow.validation.validators.evaluators

import dev.harrel.jsonschema.Evaluator
import dev.harrel.jsonschema.EvaluationContext
import dev.harrel.jsonschema.JsonNode
import nextflow.Nextflow

import groovy.util.logging.Slf4j
import groovy.transform.CompileStatic
import java.nio.file.FileSystems
import java.nio.file.Path
import java.util.regex.Pattern

/**
 * The evaluator to validate a pattern on path values. The pattern is first matched against
 * the value itself. If that fails and the value points to a non-local filesystem (e.g. a
 * proxy filesystem that resolves to an underlying storage location), the pattern is retried
 * against the resolved real path of the value.
 *
 * @author : rcannood <rcannood@gmail.com>
 */

@Slf4j
@CompileStatic
class ResolvedPathPatternEvaluator implements Evaluator {

    private final Pattern pattern

    ResolvedPathPatternEvaluator(String patternString) {
        this.pattern = Pattern.compile(patternString)
    }

    @Override
    Evaluator.Result evaluate(EvaluationContext ctx, JsonNode node) {
        // To stay consistent with other keywords, types not applicable to this keyword should succeed
        if (!node.string) {
            return Evaluator.Result.success()
        }

        String value = node.asString()
        if (pattern.matcher(value).find()) {
            return Evaluator.Result.success()
        }

        // Retry the pattern against the resolved real path for non-local paths
        try {
            Path file = Nextflow.file(value) as Path
            if (!(file in List) && file.fileSystem != FileSystems.default) {
                String resolved = file.toRealPath().toUriString()
                if (pattern.matcher(resolved).find()) {
                    return Evaluator.Result.success()
                }
            }
        } catch (e) {
            log.debug("Could not resolve the real path of '${value}': ${e.message}")
        }

        return Evaluator.Result.failure("\"${value}\" does not match regular expression ${pattern}" as String)
    }

}
