package io.github.mudrichenkoevgeny.backend.tools.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtPostfixExpression

/**
 * Detekt rule prohibiting non-null assertion operator (!!) in production code.
 */
public class ProhibitNonSafeAssertionRule(
    config: Config = Config.empty
) : Rule(
    config = config,
    description = RULE_DESCRIPTION
) {
    override val ruleName: RuleName = RuleName("ProhibitNonSafeAssertion")

    override fun visitPostfixExpression(expression: KtPostfixExpression) {
        super.visitPostfixExpression(expression)

        if (expression.operationToken == KtTokens.EXCLEXCL) {
            val filePath = expression.containingKtFile.virtualFilePath.replace(
                '\\',
                '/'
            )
            val isTestFile = filePath.endsWith("Test.kt") ||
                filePath.contains("/test/") ||
                filePath.contains(
                    "/testing/"
                )

            if (!isTestFile) {
                report(
                    Finding(
                        entity = Entity.from(expression),
                        message = "The !! operator is strictly forbidden in production code. " +
                            "Use requireNotNull(...), checkNotNull(...), ?: error(...) or safe-calls ?."
                    )
                )
            }
        }
    }

    public companion object {
        public const val RULE_DESCRIPTION: String =
            "The !! operator is strictly forbidden in production code."
    }
}
