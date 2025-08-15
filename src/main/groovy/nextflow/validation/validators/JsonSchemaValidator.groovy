package nextflow.validation.validators

import groovy.util.logging.Slf4j
import org.json.JSONObject
import org.json.JSONArray
import java.nio.file.Files
import java.nio.file.Path
import dev.harrel.jsonschema.ValidatorFactory
import dev.harrel.jsonschema.Validator
import dev.harrel.jsonschema.EvaluatorFactory
import dev.harrel.jsonschema.FormatEvaluatorFactory
import dev.harrel.jsonschema.JsonNode
import dev.harrel.jsonschema.providers.OrgJsonNode

import nextflow.validation.config.ValidationConfig
import nextflow.validation.exceptions.SchemaValidationException
import nextflow.validation.validators.evaluators.CustomEvaluatorFactory
import nextflow.validation.validators.ValidationResult
import static nextflow.validation.utils.Common.getValueFromJsonPointer

/**
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */

@Slf4j
public class JsonSchemaValidator {

    private ValidatorFactory validator
    private ValidationConfig config

    JsonSchemaValidator(ValidationConfig config) {
        this.validator = new ValidatorFactory()
            .withJsonNodeFactory(new OrgJsonNode.Factory())
            // .withDialect() // TODO define the dialect
            .withEvaluatorFactory(EvaluatorFactory.compose(new CustomEvaluatorFactory(config), new FormatEvaluatorFactory()))
        this.config = config
    }

    private ValidationResult validateObject(JsonNode input, Object rawJson, String schemaFileName) {
        def JSONObject schema
        def String schemaString
        try {
            schemaString = Files.readString(Path.of(schemaFileName))
            schema = new JSONObject(schemaString)
        } catch (org.json.JSONException e) {
            throw new SchemaValidationException("""Failed to load JSON schema (${schemaFileName}):
    ${e.message}

""")
        }

        def String draft = getValueFromJsonPointer("#/\$schema", schema)
        if(draft != "https://json-schema.org/draft/2020-12/schema") {
            log.error("""Failed to load the meta schema:
    The used schema draft (${draft}) is not correct, please use \"https://json-schema.org/draft/2020-12/schema\" instead.
        - If you are a pipeline developer, check our migration guide for more information: https://nextflow-io.github.io/nf-schema/latest/migration_guide/
        - If you are a pipeline user, revert back to nf-validation to avoid this error: https://www.nextflow.io/docs/latest/plugins.html#using-plugins, i.e. set `plugins {
    id 'nf-validation@1.1.3'
}` in your `nextflow.config` file
            """)
            throw new SchemaValidationException("", [])
        }
        def Validator.Result result = this.validator.validate(schema, input)
        return new ValidationResult(result, rawJson, schemaString, config)
    }

    /*
    public ValidationResult validate(JSONArray input, String schemaString) {
        def JsonNode jsonInput = new OrgJsonNode.Factory().wrap(input)
        return this.validateObject(jsonInput, input, schemaString)
    }

    public ValidationResult validate(JSONObject input, String schemaString) {
        def JsonNode jsonInput = new OrgJsonNode.Factory().wrap(input)
        return this.validateObject(jsonInput, input, schemaString)
    }
    */

    public ValidationResult validate(Object input, String schemaFileName) {
        def JsonNode jsonInput = new OrgJsonNode.Factory().wrap(input)
        return this.validateObject(jsonInput, input, schemaFileName)
    }

}