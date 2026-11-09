package io.github.mudrichenkoevgeny.backend.tools.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression

/**
 * Detekt rule prohibiting unstructured 'println' or 'print' calls in production code.
 */
public class ProhibitPrintlnRule(
    config: Config = Config.empty
) : Rule(
    config = config,
    description = RULE_DESCRIPTION
) {
    override val ruleName: RuleName = RuleName("ProhibitPrintln")

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val calleeName = expression.calleeExpression?.text ?: return
        if (calleeName == "println" || calleeName == "print") {
            val filePath = expression.containingKtFile.virtualFilePath.replace(
                '\\',
                '/'
            )
            val isTestOrToolFile = filePath.endsWith("Test.kt") ||
                filePath.contains("/test/") ||
                filePath.contains(
                    "/testing/"
                ) ||
                filePath.contains("/tools/")

            if (!isTestOrToolFile) {
                report(
                    Finding(
                        entity = Entity.from(expression),
                        message = "The '$calleeName' call is prohibited in production code. " +
                            "Use structured loggers or KMP logging primitives."
                    )
                )
            }
        }
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)

        val text = expression.text
        if (text.startsWith("System.out.print") || text.startsWith("System.err.print")) {
            val filePath = expression.containingKtFile.virtualFilePath.replace(
                '\\',
                '/'
            )
            val isTestOrToolFile = filePath.endsWith("Test.kt") ||
                filePath.contains("/test/") ||
                filePath.contains(
                    "/testing/"
                ) ||
                filePath.contains("/tools/")

            if (!isTestOrToolFile) {
                report(
                    Finding(
                        entity = Entity.from(expression),
                        message = "Direct System.out/err print call '$text' is prohibited in production code. " +
                            "Use structured loggers or KMP logging primitives."
                    )
                )
            }
        }
    }

    public companion object {
        public const val RULE_DESCRIPTION: String =
            "The 'println' or 'print' call is prohibited in production code. " +
                "Use structured loggers or KMP logging primitives."
    }
}
