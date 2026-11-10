package io.github.mudrichenkoevgeny.backend.tools.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression

/**
 * Detekt rule ensuring OpenAPI documentation permissions (builder) match actual route permissions (body).
 */
public class KtorConsistentRoutePermissionsRule(
    config: Config = Config.empty
) : Rule(
    config = config,
    description = RULE_DESCRIPTION
) {
    override val ruleName: RuleName = RuleName("KtorConsistentRoutePermissions")

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

        checkRoutePermissions(expression)
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

    private fun checkRoutePermissions(expression: KtCallExpression) {
        val builderArg = expression.valueArguments.find { it.getArgumentName()?.asName?.identifier == "builder" }
        val bodyArg = expression.valueArguments.find { it.getArgumentName()?.asName?.identifier == "body" }

        if (builderArg != null && bodyArg != null) {
            val builderLambda = builderArg.getArgumentExpression() as? KtLambdaExpression
            val bodyLambda = bodyArg.getArgumentExpression() as? KtLambdaExpression

            val builderCall = builderLambda?.bodyExpression?.statements?.firstOrNull() as? KtCallExpression
            val bodyCall = bodyLambda?.bodyExpression?.statements?.firstOrNull() as? KtCallExpression

            if (builderCall != null && bodyCall != null) {
                val builderVars = builderCall.valueArguments.mapNotNull { it.getArgumentExpression()?.text }.toSet()
                val bodyVars = bodyCall.valueArguments.mapNotNull { it.getArgumentExpression()?.text }.toSet()

                val builderSecurityVars = builderVars
                    .filter {
                        it.contains(
                            "Role",
                            ignoreCase = true
                        ) ||
                            it.contains(
                                "Status",
                                ignoreCase = true
                            )
                    }.toSet()

                val bodySecurityVars = bodyVars
                    .filter {
                        it.contains(
                            "Role",
                            ignoreCase = true
                        ) ||
                            it.contains(
                                "Status",
                                ignoreCase = true
                            )
                    }.toSet()

                val isExplicitlyPublic = builderVars.any { it == "isPublic = true" || it == "true" } ||
                    expression.containingKtFile.text.contains(
                        "isPublic = true"
                    )

                if (isExplicitlyPublic && bodySecurityVars.isEmpty()) {
                    return
                }

                if (builderSecurityVars.isNotEmpty() || bodySecurityVars.isNotEmpty()) {
                    if (builderSecurityVars != bodySecurityVars) {
                        report(
                            Finding(
                                entity = Entity.from(expression),
                                message = "Security Drift: OpenAPI documentation (builder) and route handler (body) " +
                                    "use different security parameters.\n" +
                                    "Builder uses: $builderSecurityVars\n" +
                                    "Body uses: $bodySecurityVars\n" +
                                    "Pass exactly the same local variables (e.g. allowedRoles) to both."
                            )
                        )
                    }
                }
            }
        }
    }

    public companion object {
        public const val RULE_DESCRIPTION: String =
            "OpenAPI documentation parameters must match route handler parameters."
    }
}
