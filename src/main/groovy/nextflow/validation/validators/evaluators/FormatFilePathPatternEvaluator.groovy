package nextflow.validation.validators.evaluators

import dev.harrel.jsonschema.Evaluator
import dev.harrel.jsonschema.EvaluationContext
import dev.harrel.jsonschema.JsonNode
import nextflow.Nextflow

import groovy.util.logging.Slf4j
import groovy.transform.CompileDynamic
import java.nio.file.Path

/**
 * The evaluator to validate a file path pattern format
 *
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */

@Slf4j
@CompileDynamic
class FormatFilePathPatternEvaluator implements Evaluator {

    @Override
    Evaluator.Result evaluate(EvaluationContext ctx, JsonNode node) {
        // To stay consistent with other keywords, types not applicable to this keyword should succeed
        if (!node.string) {
            return Evaluator.Result.success()
        }

        String value = node.asString()
        List<Path> files

        try {
            files = Nextflow.files(value)
            files.each { file ->
                file.exists() // Do an exists check to see if the file can be correctly accessed
            }
        } catch (e) {
            return Evaluator.Result.failure("could not validate file format of '${value}': ${e.message}" as String)
        }
        // Actual validation logic
        List<String> errors = []

        if (files.size() == 0) {
            return Evaluator.Result.failure("No files were found using the glob pattern '${value}'" as String)
        }
        files.each { file ->
            /* groovylint-disable-next-line UnnecessaryGetter */
            if (file.isDirectory()) {
                errors.add("'${file.string}' is not a file, but a directory" as String)
            }
        }
        if (errors.size() > 0) {
            return Evaluator.Result.failure(errors.join('\n'))
        }
        return Evaluator.Result.success()
    }

}
