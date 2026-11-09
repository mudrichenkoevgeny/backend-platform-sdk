package io.github.mudrichenkoevgeny.backend.tools.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile

/**
 * Detekt rule prohibiting direct usage of system time (e.g., `LocalDateTime.now()`, `Date()`).
 * Enforces the use of an injected `Clock` parameter for testability and UTC consistency.
 */
public class RequireUtcTimeRule(
    config: Config = Config.empty
) : Rule(
    config = config,
    description = RULE_DESCRIPTION
) {
    override val ruleName: RuleName = RuleName("RequireUtcTime")

    private val bannedNowReceivers = setOf(
        "LocalDateTime",
        "LocalDate",
        "LocalTime",
        "OffsetDateTime",
        "ZonedDateTime",
        "Instant"
    )

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)

        if (isTestFile(expression.containingKtFile)) return

        val receiverText = expression.receiverExpression.text
        val selector = expression.selectorExpression

        if (selector is KtCallExpression) {
            val calleeName = selector.calleeExpression?.text
            if (calleeName == "now" && selector.valueArguments.isEmpty()) {
                if (receiverText in bannedNowReceivers || receiverText.endsWith(".Clock.System")) {
                    report(
                        Finding(
                            entity = Entity.from(expression),
                            message = "Do not use '$receiverText.now()' directly without a Clock. " +
                                "Pass an explicit Clock parameter (e.g., $receiverText.now(clock)) " +
                                "to guarantee UTC consistency and testability."
                        )
                    )
                }
            }
        }
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        if (isTestFile(expression.containingKtFile)) return

        val calleeName = expression.calleeExpression?.text
        if (calleeName == "Date" && expression.valueArguments.isEmpty()) {
            report(
                Finding(
                    entity = Entity.from(expression),
                    message = "Do not instantiate 'Date()' directly. Use a centralized injected Clock provider " +
                        "for testability and UTC consistency."
                )
            )
        }
    }

    private fun isTestFile(ktFile: KtFile): Boolean {
        val filePath = ktFile.virtualFilePath.replace(
            '\\',
            '/'
        )
        return filePath.endsWith("Test.kt") ||
            filePath.contains("/test/") ||
            filePath.contains(
                "/testFixtures/"
            ) ||
            filePath.contains("/testing/")
    }

    public companion object {
        public const val RULE_DESCRIPTION: String =
            "Do not use system time directly. Inject a Clock instance for testability and UTC consistency."
    }
}
