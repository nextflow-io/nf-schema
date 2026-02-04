package nextflow.validation.validators.evaluators

import dev.harrel.jsonschema.Evaluator
import dev.harrel.jsonschema.EvaluationContext
import dev.harrel.jsonschema.JsonNode
import nextflow.Nextflow

import groovy.util.logging.Slf4j
import groovy.transform.CompileDynamic
import java.nio.file.Path

/**
 * Checks whether the file should or should not exist
 *
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */

@Slf4j
@CompileDynamic
class ExistsEvaluator implements Evaluator {

    private final Boolean shouldExist // true if the file should exist, false if it should not

    ExistsEvaluator(Boolean exists) {
        this.shouldExist = exists
    }

    @Override
    Evaluator.Result evaluate(EvaluationContext ctx, JsonNode node) {
        // To stay consistent with other keywords, types not applicable to this keyword should succeed
        if (!node.string) {
            return Evaluator.Result.success()
        }

        String value = node.asString()
        Boolean exists

        try {
            List<Path> files = Nextflow.files(value) as List<Path>
            Integer fileCount = files.size()
            if (fileCount == 0) {
                exists = false
            } else if (fileCount == 1) {
                exists = files[0].exists()
            } else {
                // Always return true if multiple files are found (only happens in patterns)
                exists = true
            }
        } catch (e) {
            return Evaluator.Result.failure("could not check existence of '${value}': ${e.message}" as String)
        }

        if (!exists && this.shouldExist == true) {
            return Evaluator.Result.failure("the file or directory '${value}' does not exist" as String)
        } else if (exists && this.shouldExist == false) {
            return Evaluator.Result.failure("the file or directory '${value}' should not exist" as String)
        }
        return Evaluator.Result.success()
    }

}
