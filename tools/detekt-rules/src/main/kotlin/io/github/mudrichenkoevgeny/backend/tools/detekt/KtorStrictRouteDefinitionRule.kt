package io.github.mudrichenkoevgeny.backend.tools.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import org.jetbrains.kotlin.psi.KtCallExpression

/**
 * Detekt rule enforcing that all Ktor route parameters are named.
 */
public class KtorStrictRouteDefinitionRule(
    config: Config = Config.empty
) : Rule(
    config = config,
    description = RULE_DESCRIPTION
) {
    override val ruleName: RuleName = RuleName("KtorStrictRouteDefinition")

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

        val args = expression.valueArguments
        if (args.isEmpty()) return

        val hasUnnamed = args.any { !it.isNamed() }
        if (hasUnnamed) {
            report(
                Finding(
                    entity = Entity.from(expression),
                    message = "Ktor route definitions must use named arguments for ALL parameters. " +
                        "Trailing lambdas and positional arguments are prohibited here (use path = ..., body = ...)."
                )
            )
        }
    }

    public companion object {
        public const val RULE_DESCRIPTION: String =
            "Ktor route definitions must use named arguments for all parameters (e.g., path = ..., body = ...)."
    }
}
