package nextflow.validation

import dev.harrel.jsonschema.Evaluator
import dev.harrel.jsonschema.EvaluationContext
import dev.harrel.jsonschema.JsonNode
import nextflow.Nextflow
import nextflow.Global
import org.json.JSONArray
import org.json.JSONObject

import groovy.util.logging.Slf4j
import java.nio.file.Path
import java.nio.file.Files

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

        // Actual validation logic
        def Path file = Nextflow.file(value)
        // Don't validate if the file does not exist or is a directory
        if(!file.exists() || file.isDirectory()) {
            log.debug("Could not validate the file ${file.toString()}")
            return Evaluator.Result.success()
        }

        log.debug("Started validating ${file.toString()}")

        def String schemaFull = Utils.getSchemaPath(this.baseDir, this.schema)
        def Object json = Utils.fileToJson(file, Path.of(schemaFull))
        def String schemaContents = Files.readString( Path.of(schemaFull) )
        def validator = new JsonSchemaValidator(config)

        def List<String> validationErrors = validator.validate(json, schemaContents)
        if (validationErrors) {
            def List<String> errors = ["Validation of file failed:"] + validationErrors.collect { "\t${it}" as String}
            return Evaluator.Result.failure(errors.join("\n"))
        }

        log.debug("Validation of file '${value}' passed!")
        return Evaluator.Result.success()
    }

}