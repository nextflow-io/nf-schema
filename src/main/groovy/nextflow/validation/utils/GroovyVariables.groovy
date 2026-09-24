package nextflow.validation.utils

import groovy.transform.CompileStatic

import org.codehaus.groovy.ast.expr.ConstructorCallExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.SecureASTCustomizer

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Utility class for evaluating Groovy expressions with variable bindings.
 */
@CompileStatic
class GroovyVariables {

    private static final Pattern VARIABLE =
        Pattern.compile(/\$\{([A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*)*)\}/)

    private static final CompilerConfiguration CONFIG = makeConfig()

    static String evaluate(String expression, Binding binding) {
        Matcher matcher = VARIABLE.matcher(expression)
        StringBuffer result = new StringBuffer()

        while (matcher.find()) {
            String variablePath = matcher.group(1)
            String rootVariable = variablePath.split(/\./, 2)[0]

            // The root must be explicitly present in the supplied binding.
            if (!binding.variables.containsKey(rootVariable)) {
                matcher.appendReplacement(
                    result,
                    Matcher.quoteReplacement(matcher.group(0))
                )
                continue
            }

            Object value
            try {
                value = new GroovyShell(binding, CONFIG).evaluate(variablePath)
            } catch (NullPointerException) {
                // Skip NullPointerException as this usually points to a nested parameter not existing
                matcher.appendReplacement(
                    result,
                    Matcher.quoteReplacement(matcher.group(0))
                )
                continue
            }

            matcher.appendReplacement(
                result,
                Matcher.quoteReplacement(String.valueOf(value))
            )
        }

        matcher.appendTail(result)
        return result.toString()
    }

    private static CompilerConfiguration makeConfig() {
        SecureASTCustomizer secure = new SecureASTCustomizer()

        secure.methodDefinitionAllowed = false
        secure.packageAllowed = false
        secure.closuresAllowed = false

        secure.importsWhitelist = []
        secure.starImportsWhitelist = []
        secure.staticImportsWhitelist = []
        secure.staticStarImportsWhitelist = []

        secure.addExpressionCheckers({ Expression expression ->
            if (expression in MethodCallExpression) {
                return false
            }
            if (expression in ConstructorCallExpression) {
                return false
            }
            if (expression in StaticMethodCallExpression) {
                return false
            }

            return expression in VariableExpression ||
                expression in PropertyExpression
        } as SecureASTCustomizer.ExpressionChecker)

        CompilerConfiguration config = new CompilerConfiguration()
        config.addCompilationCustomizers(secure)
        return config
    }

}
