package nextflow.validation.validators.evaluators

import dev.harrel.jsonschema.Evaluator
import dev.harrel.jsonschema.EvaluationContext
import dev.harrel.jsonschema.JsonNode
import nextflow.Nextflow

import groovy.util.logging.Slf4j
import groovy.transform.CompileDynamic
import java.nio.file.Path

/**
 * The evaluator to validate a directory path format
 *
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */

@Slf4j
@CompileDynamic
class FormatDirectoryPathEvaluator implements Evaluator {

    @Override
    Evaluator.Result evaluate(EvaluationContext ctx, JsonNode node) {
        // To stay consistent with other keywords, types not applicable to this keyword should succeed
        if (!node.string) {
            return Evaluator.Result.success()
        }

        String value = node.asString()
        Path file

        try {
            file = Nextflow.file(value) as Path
            if (!(file in List)) {
                file.exists() // Do an exists check to see if the file can be correctly accessed
            }
        } catch (e) {
            return Evaluator.Result.failure("could not validate file format of '${value}': ${e.message}" as String)
        }

        // Actual validation logic
        if (file in List) {
            return Evaluator.Result.failure("'${value}' is not a directory, but a file path pattern" as String)
        }
        /* groovylint-disable-next-line UnnecessaryGetter */
        if (file.exists() && !file.isDirectory()) {
            return Evaluator.Result.failure("'${value}' is not a directory, but a file" as String)
        }
        return Evaluator.Result.success()
    }

}
