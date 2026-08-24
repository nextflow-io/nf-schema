package nextflow.validation.validators.evaluators

import groovy.transform.CompileStatic

import java.nio.file.Path

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

@CompileStatic
class CustomEvaluatorFactory implements EvaluatorFactory {

    final private ValidationConfig config
    final private Path baseDir

    CustomEvaluatorFactory(ValidationConfig configInput) {
        Session session = Global.session as Session
        config = configInput
        baseDir = session.baseDir
    }

    @Override
    Optional<Evaluator> create(SchemaParsingContext ctx, String fieldName, JsonNode schemaNode) {
        if (fieldName == 'format' && schemaNode.string) {
            String schemaString = schemaNode.asString()
            switch (schemaString) {
                case 'directory-path':
                    return Optional.of((Evaluator) new FormatDirectoryPathEvaluator())
                case 'file-path':
                    return Optional.of((Evaluator) new FormatFilePathEvaluator())
                case 'path':
                    return Optional.of((Evaluator) new FormatPathEvaluator())
                case 'file-path-pattern':
                    return Optional.of((Evaluator) new FormatFilePathPatternEvaluator())
            }
        } else if (fieldName == 'exists' && schemaNode.boolean) {
            return Optional.of((Evaluator) new ExistsEvaluator(schemaNode.asBoolean()))
        } else if (fieldName == 'schema' && schemaNode.string) {
            return Optional.of((Evaluator) new SchemaEvaluator(schemaNode.asString(), this.baseDir, this.config))
        } else if (fieldName == 'uniqueEntries' && schemaNode.array) {
            return Optional.of((Evaluator) new UniqueEntriesEvaluator(schemaNode.asArray()))
        } else if (fieldName == 'type' && (schemaNode.string || schemaNode.array) && config.lenientMode) {
            return Optional.of((Evaluator) new LenientTypeEvaluator(schemaNode))
        } else if (fieldName == 'deprecated' && schemaNode.boolean) {
            return Optional.of((Evaluator) new DeprecatedEvaluator(schemaNode.asBoolean()))
        } else if (fieldName == 'pattern' && schemaNode.string) {
            JsonNode format = ctx.currentSchemaObject.get('format')
            if (format?.string && format.asString() in ['file-path', 'directory-path', 'path']) {
                return Optional.of((Evaluator) new ResolvedPathPatternEvaluator(schemaNode.asString()))
            }
        }
        return Optional.empty()
    }

}
