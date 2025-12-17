package nextflow.validation.validators.evaluators

import dev.harrel.jsonschema.Evaluator
import dev.harrel.jsonschema.EvaluationContext
import dev.harrel.jsonschema.JsonNode
import nextflow.Nextflow

import groovy.util.logging.Slf4j
import java.nio.file.Path

import static nextflow.validation.utils.Common.isCloudStoragePath

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

        try {
            def List<Path> files = Nextflow.files(value) as List<Path>

            def Integer fileCount = files.size()
            if (fileCount == 0) {
                exists = false
            } else if (fileCount == 1) {
                exists = files[0].exists()
            } else {
                // Always return true if multiple files are found (only happens in patterns)
                exists = true
            }

        } catch (Exception e) {
            // For cloud storage paths, skip validation gracefully if we can't access them
            // (e.g., due to missing credentials or permissions)
            if (isCloudStoragePath(value)) {
                log.debug("Skipping existence validation for cloud storage path '${value}' due to exception: ${e.message}")
                return Evaluator.Result.success()
            }
            return Evaluator.Result.failure("could not check existence of '${value}': ${e.message}" as String)
        }

        if (!exists && this.shouldExist == true) {
            return Evaluator.Result.failure("the file or directory '${value}' does not exist" as String)
        } else if(exists && this.shouldExist == false) {
            return Evaluator.Result.failure("the file or directory '${value}' should not exist" as String)
        }
        return Evaluator.Result.success()
    }
}