package nextflow.validation.validators.evaluators

import dev.harrel.jsonschema.Evaluator
import dev.harrel.jsonschema.EvaluationContext
import dev.harrel.jsonschema.JsonNode
import nextflow.Nextflow

import groovy.util.logging.Slf4j
import java.nio.file.Path

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
            return Evaluator.Result.failure("could not validate file format of '${value}': ${e.message}" as String)
        }
        // Actual validation logic
        def List<String> errors = []

        if(files.size() == 0) {
            return Evaluator.Result.failure("No files were found using the glob pattern '${value}'" as String)
        }
        files.each { file ->
            // Check if this is an Azure storage path
            def String scheme = file.scheme
            def boolean isAzurePath = scheme == 'az'
            
            // For Azure paths, skip the directory check as Azure blob storage doesn't have true directories
            if (!isAzurePath && file.isDirectory()) {
                errors.add("'${file.toString()}' is not a file, but a directory" as String)
            }
        }
        if(errors.size() > 0) {
            return Evaluator.Result.failure(errors.join('\n'))
        }
        return Evaluator.Result.success()
    }
}