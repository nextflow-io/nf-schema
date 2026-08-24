package nextflow.validation.validators.evaluators

import static nextflow.validation.utils.Common.getBasePath
import static nextflow.validation.utils.FilesHelper.fileToJson
import static nextflow.validation.utils.FilesHelper.fileToObject

import org.json.JSONObject
import dev.harrel.jsonschema.Evaluator
import dev.harrel.jsonschema.EvaluationContext
import dev.harrel.jsonschema.JsonNode
import nextflow.Nextflow

import groovy.util.logging.Slf4j
import groovy.transform.CompileStatic
import java.nio.file.Path

import nextflow.validation.config.ValidationConfig
import nextflow.validation.validators.JsonSchemaValidator
import nextflow.validation.validators.ValidationResult

/**
 * The evaluator to validate a file against a given schema
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */

@Slf4j
@CompileStatic
class SchemaEvaluator implements Evaluator {

    private final String schema
    private final Path baseDir
    private final ValidationConfig config

    SchemaEvaluator(String schema, Path baseDir, ValidationConfig config) {
        this.baseDir = baseDir
        this.schema = schema
        this.config = config
    }

    @Override
    Evaluator.Result evaluate(EvaluationContext ctx, JsonNode node) {
        // To stay consistent with other keywords, types not applicable to this keyword should succeed
        if (!node.string) {
            return Evaluator.Result.success()
        }

        String value = node.asString()

        // Actual validation logic
        Path file = Nextflow.file(value) as Path

        // Don't validate if the file does not exist
        if (!file.exists()) {
            log.debug("Could not validate the file ${file} - file does not exist")
            return Evaluator.Result.success()
        }

        // Skip validation if it's a directory
        /* groovylint-disable-next-line UnnecessaryGetter */
        if (file.isDirectory() || value.startsWith('az://')) {
            log.debug("Could not validate the file ${file} - path is a directory")
            return Evaluator.Result.success()
        }

        log.debug("Started validating ${file}")

        JSONObject schemaJson = new JSONObject(getBasePath(this.baseDir, this.schema).text)
        Object groovyObject = fileToObject(file, schemaJson)
        Object json = fileToJson(groovyObject)
        JsonSchemaValidator validator = new JsonSchemaValidator(config)

        ValidationResult validationResult = validator.validate(json, schemaJson)
        List<String> validationErrors = validationResult.getErrors((json in JSONObject) ? 'parameter' : 'field')
        if (validationErrors) {
            List<String> errors = ['Validation of file failed:'] +
                validationErrors.collect { err -> "\t${err}" as String }
            return Evaluator.Result.failure(errors.join('\n'))
        }

        log.debug("Validation of file '${value}' passed!")
        return Evaluator.Result.success()
    }

}
