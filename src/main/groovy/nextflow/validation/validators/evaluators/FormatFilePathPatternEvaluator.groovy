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
class FormatFilePathPatternEvaluator implements Evaluator {
    // The string should be a path pattern

    @Override
    public Evaluator.Result evaluate(EvaluationContext ctx, JsonNode node) {
        // To stay consistent with other keywords, types not applicable to this keyword should succeed
        if (!node.isString()) {
            return Evaluator.Result.success()
        }

        def String value = node.asString()
        def List<Path> files

        try {
            files = Nextflow.files(value)
            files.each { file ->
                file.exists() // Do an exists check to see if the file can be correctly accessed
            }
        } catch (Exception e) {
            // For cloud storage paths, skip validation gracefully if we can't access them
            // (e.g., due to missing credentials or permissions)
            if (isCloudStoragePath(value)) {
                log.debug("Skipping validation for inaccessible cloud storage path '${value}': ${e.message}")
                return Evaluator.Result.success()
            }
            return Evaluator.Result.failure("could not validate file format of '${value}': ${e.message}" as String)
        }
        // Actual validation logic
        def List<String> errors = []

        if(files.size() == 0) {
            // For cloud storage paths, empty results may just mean we can't list the bucket
            if (isCloudStoragePath(value)) {
                log.debug("Skipping validation for cloud storage glob pattern '${value}': no files found (may be due to permissions)")
                return Evaluator.Result.success()
            }
            return Evaluator.Result.failure("No files were found using the glob pattern '${value}'" as String)
        }
        files.each { file ->
            if (file.isDirectory()) {
                errors.add("'${file.toString()}' is not a file, but a directory" as String)
            }
        }
        if(errors.size() > 0) {
            return Evaluator.Result.failure(errors.join('\n'))
        }
        return Evaluator.Result.success()
    }
}