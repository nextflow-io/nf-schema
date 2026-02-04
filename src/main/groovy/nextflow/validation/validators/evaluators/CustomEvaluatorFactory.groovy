package nextflow.validation.validators.evaluators

import groovy.transform.CompileDynamic

import nextflow.Global
import nextflow.Session
import dev.harrel.jsonschema.EvaluatorFactory
import dev.harrel.jsonschema.Evaluator
import dev.harrel.jsonschema.SchemaParsingContext
import dev.harrel.jsonschema.JsonNode

import nextflow.validation.config.ValidationConfig

/**
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 */

@CompileDynamic
class CustomEvaluatorFactory implements EvaluatorFactory {

    final private ValidationConfig config
    final private String baseDir

    CustomEvaluatorFactory(ValidationConfig configInput) {
        Session session = Global.session
        config = configInput
        baseDir = session.baseDir.toString()
    }

    @Override
    Optional<Evaluator> create(SchemaParsingContext ctx, String fieldName, JsonNode schemaNode) {
        if (fieldName == 'format' && schemaNode.string) {
            String schemaString = schemaNode.asString()
            switch (schemaString) {
                case 'directory-path':
                    return Optional.of(new FormatDirectoryPathEvaluator())
                case 'file-path':
                    return Optional.of(new FormatFilePathEvaluator())
                case 'path':
                    return Optional.of(new FormatPathEvaluator())
                case 'file-path-pattern':
                    return Optional.of(new FormatFilePathPatternEvaluator())
            }
        } else if (fieldName == 'exists' && schemaNode.boolean) {
            return Optional.of(new ExistsEvaluator(schemaNode.asBoolean()))
        } else if (fieldName == 'schema' && schemaNode.string) {
            return Optional.of(new SchemaEvaluator(schemaNode.asString(), this.baseDir, this.config))
        } else if (fieldName == 'uniqueEntries' && schemaNode.array) {
            return Optional.of(new UniqueEntriesEvaluator(schemaNode.asArray()))
        } else if (fieldName == 'type' && (schemaNode.string || schemaNode.array) && config.lenientMode) {
            return Optional.of(new LenientTypeEvaluator(schemaNode))
        } else if (fieldName == 'deprecated' && schemaNode.boolean) {
            return Optional.of(new DeprecatedEvaluator(schemaNode.asBoolean()))
        }
        return Optional.empty()
    }

}
