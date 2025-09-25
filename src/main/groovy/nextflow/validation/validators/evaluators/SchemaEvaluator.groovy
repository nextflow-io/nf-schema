package nextflow.validation.validators.evaluators

import org.json.JSONObject
import dev.harrel.jsonschema.Evaluator
import dev.harrel.jsonschema.EvaluationContext
import dev.harrel.jsonschema.JsonNode
import nextflow.Nextflow

import groovy.util.logging.Slf4j
import java.nio.file.Path

import static nextflow.validation.utils.Common.getBasePath
import static nextflow.validation.utils.Files.fileToJson
import nextflow.validation.config.ValidationConfig
import nextflow.validation.validators.JsonSchemaValidator
import nextflow.validation.validators.ValidationResult

/**
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */

@Slf4j
class SchemaEvaluator implements Evaluator {
    // Evaluate the file using the given schema

    private final String schema
    private final String baseDir
    private final ValidationConfig config

    SchemaEvaluator(String schema, String baseDir, ValidationConfig config) {
        this.baseDir = baseDir
        this.schema = schema
        this.config = config
    }

    @Override
    public Evaluator.Result evaluate(EvaluationContext ctx, JsonNode node) {
        // To stay consistent with other keywords, types not applicable to this keyword should succeed
        if (!node.isString()) {
            return Evaluator.Result.success()
        }

        def String value = node.asString()
        
        // Check if this is an Azure storage path early
        def boolean isAzurePath = value.startsWith('az://')

        // Actual validation logic
        def Path file = Nextflow.file(value)

        // Don't validate if the file does not exist
        if(!file.exists()) {
            log.debug("Could not validate the file ${file.toString()} - file does not exist")
            return Evaluator.Result.success()
        }
        
        // For non-Azure paths, skip validation if it's a directory
        if(!isAzurePath && file.isDirectory()) {
            log.debug("Could not validate the file ${file.toString()} - path is a directory")
            return Evaluator.Result.success()
        }

        log.debug("Started validating ${file.toString()}")

        def String schemaFull = getBasePath(this.baseDir, this.schema)
        def Object json = fileToJson(file, Path.of(schemaFull))
        def validator = new JsonSchemaValidator(config)

        def ValidationResult validationResult = validator.validate(json, schemaFull)
        def List<String> validationErrors = validationResult.getErrors((json instanceof JSONObject) ? "parameter" : "field")
        if (validationErrors) {
            def List<String> errors = ["Validation of file failed:"] + validationErrors.collect { "\t${it}" as String}
            return Evaluator.Result.failure(errors.join("\n"))
        }

        log.debug("Validation of file '${value}' passed!")
        return Evaluator.Result.success()
    }

}