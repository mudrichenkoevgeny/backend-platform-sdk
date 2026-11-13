package io.github.mudrichenkoevgeny.backend.tools.detektrules

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.psi.KtWhenExpression

/**
 * Detekt rule enforcing that Ktor route builders and bodies delegate logic to named functions.
 */
public class KtorDelegatedRouteHandlerRule(
    config: Config = Config.empty
) : Rule(
    config = config,
    description = RULE_DESCRIPTION
) {
    override val ruleName: RuleName = RuleName("KtorDelegatedRouteHandler")

    private val routeMethods = setOf(
        "get",
        "post",
        "put",
        "delete",
        "patch",
        "head",
        "options"
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        if (isTestOrIgnoredFile(expression)) return

        val calleeName = expression.calleeExpression?.text ?: return
        if (calleeName !in routeMethods) return

        expression.valueArguments.forEach { arg ->
            val name = arg.getArgumentName()?.asName?.identifier
            if (name == "body" || name == "builder") {
                val lambda = arg.getArgumentExpression() as? KtLambdaExpression ?: return@forEach
                checkLambdaStatements(lambda, name)
            }
        }
    }

    private fun isTestOrIgnoredFile(expression: KtCallExpression): Boolean {
        val ktFile = expression.containingKtFile
        val fileName = ktFile.name
        val filePath = ktFile.virtualFilePath.replace('\\', '/')
        val isTestFile = fileName.endsWith("Test.kt") ||
            filePath.contains("/test/") ||
            filePath.contains("/testFixtures/")
        return isTestFile || (!fileName.contains("Router") && !fileName.contains("Route"))
    }

    private fun checkLambdaStatements(
        lambda: KtLambdaExpression,
        name: String
    ) {
        val statements = lambda.bodyExpression?.statements ?: return

        if (statements.size > 1) {
            report(
                Finding(
                    entity = Entity.from(lambda),
                    message = "Route lambda '$name' must contain exactly one function call delegating " +
                        "to a separate handler. Found multiple statements."
                )
            )
        } else if (statements.size == 1) {
            val singleStatement = statements[0]
            if (singleStatement !is KtCallExpression &&
                singleStatement !is KtDotQualifiedExpression &&
                singleStatement !is KtReturnExpression
            ) {
                report(
                    Finding(
                        entity = Entity.from(lambda),
                        message = "Route lambda '$name' must delegate to a " +
                            "function. Found inline logic."
                    )
                )
            }
            if (
                (singleStatement is KtIfExpression) ||
                (singleStatement is KtWhenExpression) ||
                (singleStatement is KtTryExpression)
            ) {
                report(
                    Finding(
                        entity = Entity.from(lambda),
                        message = "Route lambda '$name' must not contain complex logic (if/when/try). " +
                            "Delegate to a function."
                    )
                )
            }
        }
    }

    public companion object {
        public const val RULE_DESCRIPTION: String =
            "Route body and builder must delegate to a named function. Do not inline business logic."
    }
}
