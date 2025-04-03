package nextflow.validation.validators.evaluators

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

class CustomEvaluatorFactory implements EvaluatorFactory {

    private ValidationConfig config
    private String baseDir

    CustomEvaluatorFactory(ValidationConfig config) {
        def Session session = Global.getSession()
        this.config = config
        this.baseDir = session.baseDir.toString()
    }

    @Override
    public Optional<Evaluator> create(SchemaParsingContext ctx, String fieldName, JsonNode schemaNode) {
        if (fieldName == "format" && schemaNode.isString()) {
            def String schemaString = schemaNode.asString()
            switch (schemaString) {
                case "directory-path":
                return Optional.of(new FormatDirectoryPathEvaluator())
                case "file-path":
                return Optional.of(new FormatFilePathEvaluator())
                case "path":
                return Optional.of(new FormatPathEvaluator())
                case "file-path-pattern":
                return Optional.of(new FormatFilePathPatternEvaluator())
            }
        } else if (fieldName == "exists" && schemaNode.isBoolean()) {
            return Optional.of(new ExistsEvaluator(schemaNode.asBoolean()))
        } else if (fieldName == "schema" && schemaNode.isString()) {
            return Optional.of(new SchemaEvaluator(schemaNode.asString(), this.baseDir, this.config))
        } else if (fieldName == "uniqueEntries" && schemaNode.isArray()) {
            return Optional.of(new UniqueEntriesEvaluator(schemaNode.asArray()))
        } else if (fieldName == "type" && (schemaNode.isString() || schemaNode.isArray()) && config.lenientMode) {
            return Optional.of(new LenientTypeEvaluator(schemaNode))
        } else if (fieldName == "deprecated" && schemaNode.isBoolean()) {
            return Optional.of(new DeprecatedEvaluator(schemaNode.asBoolean()))
        }

        return Optional.empty()
    }
}