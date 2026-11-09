package io.github.mudrichenkoevgeny.backend.tools.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression

/**
 * Detekt rule prohibiting the use of System.getenv or System.getProperty in business logic.
 */
public class NoDirectSystemEnvRule(
    config: Config = Config.empty
) : Rule(
    config = config,
    description = RULE_DESCRIPTION
) {
    override val ruleName: RuleName = RuleName("NoDirectSystemEnv")

    private val allowedPackagePrefixes: List<String> by config(
        defaultValue = listOf(
            "io.github.mudrichenkoevgeny.backend.core.common.config",
            "io.github.mudrichenkoevgeny.backend.core.common.testing"
        )
    )

    private val ignoreTestFiles: Boolean by config(defaultValue = true)

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)

        val ktFile = expression.containingKtFile
        val filePath = ktFile.virtualFilePath.replace(
            '\\',
            '/'
        )

        if (ignoreTestFiles && isTestOrFixturePath(filePath)) {
            return
        }

        val packageName = ktFile.packageFqName.asString()
        if (allowedPackagePrefixes.any { allowed -> packageName.startsWith(allowed) }) {
            return
        }

        val receiver = expression.receiverExpression.text
        val selector = expression.selectorExpression?.text ?: return

        if (receiver == "System" || receiver == "java.lang.System") {
            if (selector.startsWith("getenv") ||
                selector.startsWith("getProperty") ||
                selector.startsWith(
                    "getProperties"
                )
            ) {
                report(
                    Finding(
                        entity = Entity.from(expression),
                        message = "Do not use $receiver.$selector directly in business logic. " +
                            "Read configuration via Ktor ApplicationConfig or centralize " +
                            "in a dedicated settings wrapper."
                    )
                )
            }
        }
    }

    private fun isTestOrFixturePath(path: String): Boolean =
        path.contains("/test/") || path.contains("/testFixtures/") || path.contains("/androidTest/")

    public companion object {
        public const val RULE_DESCRIPTION: String =
            "System.getenv or System.getProperty is prohibited in business logic. Centralize configuration."
    }
}
