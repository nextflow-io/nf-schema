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
class FormatDirectoryPathEvaluator implements Evaluator {
    // The string should be a directory

    @Override
    public Evaluator.Result evaluate(EvaluationContext ctx, JsonNode node) {
        // To stay consistent with other keywords, types not applicable to this keyword should succeed
        if (!node.isString()) {
            return Evaluator.Result.success()
        }

        def String value = node.asString()
        
        // Check if this is an Azure storage path early
        def boolean isAzurePath = value.startsWith('az://')
        
        def Path file
        try {
            file = Nextflow.file(value) as Path
            if (!(file instanceof List) && !isAzurePath) {
                file.exists() // Do an exists check to see if the file can be correctly accessed (skip for Azure paths)
            }
        } catch (Exception e) {
            // For Azure paths, if the plugin is missing, just validate the format
            if (isAzurePath && e.message.contains("Missing plugin 'nf-azure'")) {
                return Evaluator.Result.success()
            }
            return Evaluator.Result.failure("could not validate directory format of '${value}': ${e.message}" as String)
        }

        // Actual validation logic
        if (file instanceof List) {
            return Evaluator.Result.failure("'${value}' is not a directory, but a file path pattern" as String)
        }
        
        // For Azure paths, skip the directory check as Azure blob storage doesn't have true directories
        if (!isAzurePath && file.exists() && !file.isDirectory()) {
            return Evaluator.Result.failure("'${value}' is not a directory, but a file" as String)
        }
        return Evaluator.Result.success()
    }
}
