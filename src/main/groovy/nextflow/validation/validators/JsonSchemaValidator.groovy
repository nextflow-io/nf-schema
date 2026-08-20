/* groovylint-disable LineLength */
package nextflow.validation.validators

import static nextflow.validation.utils.Common.getValueFromJsonPointer

import groovy.util.logging.Slf4j
import groovy.transform.CompileDynamic
import org.json.JSONObject
import dev.harrel.jsonschema.ValidatorFactory
import dev.harrel.jsonschema.Validator
import dev.harrel.jsonschema.EvaluatorFactory
import dev.harrel.jsonschema.FormatEvaluatorFactory
import dev.harrel.jsonschema.JsonNode
import dev.harrel.jsonschema.providers.OrgJsonNode

import nextflow.validation.config.ValidationConfig
import nextflow.validation.exceptions.SchemaValidationException
import nextflow.validation.validators.evaluators.CustomEvaluatorFactory

/**
 * The JSON schema validator
 *
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */

@Slf4j
@CompileDynamic
public class JsonSchemaValidator {

    final private ValidatorFactory validator
    final private ValidationConfig config

    JsonSchemaValidator(ValidationConfig config) {
        this.validator = new ValidatorFactory()
            .withJsonNodeFactory(new OrgJsonNode.Factory())
            // .withDialect() // TODO define the dialect
            .withEvaluatorFactory(
                EvaluatorFactory.compose(new CustomEvaluatorFactory(config), new FormatEvaluatorFactory())
            )
        this.config = config
    }

    ValidationResult validate(Object input, JSONObject schema) {
        JsonNode jsonInput = new OrgJsonNode.Factory().wrap(input)
        return validateObject(jsonInput, input, schema)
    }

    private ValidationResult validateObject(JsonNode input, Object rawJson, JSONObject schema) {
        String draft = getValueFromJsonPointer("#/\$schema", schema)
        if (draft != 'https://json-schema.org/draft/2020-12/schema') {
            log.error("""Failed to load the meta schema:
    The used schema draft (${draft}) is not correct, please use \"https://json-schema.org/draft/2020-12/schema\" instead.
        - If you are a pipeline developer, check our migration guide for more information: https://nextflow-io.github.io/nf-schema/latest/migration_guide/
        - If you are a pipeline user, revert back to nf-validation to avoid this error: https://www.nextflow.io/docs/latest/plugins.html#using-plugins, i.e. set `plugins {
    id 'nf-validation@1.1.3'
}` in your `nextflow.config` file
            """)
            throw new SchemaValidationException('', [])
        }
        Validator.Result result = this.validator.validate(schema, input)
        return new ValidationResult(result, rawJson, schema, this.config)
    }

}
