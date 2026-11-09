package io.github.mudrichenkoevgeny.backend.tools.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

/**
 * Detekt rule prohibiting hardcoded string literals/templates for Ktor route paths.
 */
public class KtorRoutePathConstantRule(
    config: Config = Config.empty
) : Rule(
    config = config,
    description = RULE_DESCRIPTION
) {
    override val ruleName: RuleName = RuleName("KtorRoutePathConstant")

    private val routeMethods = setOf(
        "get",
        "post",
        "put",
        "delete",
        "patch",
        "head",
        "options",
        "route"
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val fileName = expression.containingKtFile.name
        if (!fileName.contains("Router") && !fileName.contains("Route")) return

        val calleeName = expression.calleeExpression?.text ?: return
        if (calleeName !in routeMethods) return

        val pathArg = expression.valueArguments.find { it.getArgumentName()?.asName?.identifier == "path" }
            ?: expression.valueArguments.firstOrNull() ?: return

        val argExpression = pathArg.getArgumentExpression()
        if (argExpression is KtStringTemplateExpression) {
            report(
                Finding(
                    entity = Entity.from(argExpression),
                    message = "Hardcoded route path '$argExpression' is prohibited. " +
                        "Use a centralized constant object (e.g., AuthRoutes.GET_AUTH)."
                )
            )
        }
    }

    public companion object {
        public const val RULE_DESCRIPTION: String =
            "Hardcoded route paths are prohibited. Use a centralized constant object."
    }
}
