package io.github.mudrichenkoevgeny.backend.tools.detektrules

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile

/**
 * Detekt rule prohibiting direct usage of system time (e.g., `LocalDateTime.now()`, `Clock.System.now()`, `Date()`, `System.currentTimeMillis()`).
 * Enforces the use of an injected `Clock` parameter for testability and UTC consistency.
 *
 * @param config Configuration for the Detekt rule.
 */
public class RequireUtcTimeRule(
    config: Config = Config.empty
) : Rule(
    config = config,
    description = RULE_DESCRIPTION
) {
    /**
     * Unique identifier name for this Detekt rule.
     */
    override val ruleName: RuleName = RuleName("RequireUtcTime")

    private val bannedJavaTimeNowReceivers = setOf(
        "LocalDateTime",
        "LocalDate",
        "LocalTime",
        "OffsetDateTime",
        "ZonedDateTime",
        "Instant",
        "Year",
        "YearMonth",
        "MonthDay"
    )

    /**
     * Inspects dot-qualified expressions to report forbidden direct system time calls.
     *
     * @param expression The dot-qualified expression PSI element to inspect.
     */
    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)

        val ktFile = expression.containingKtFile
        if (isTestFile(ktFile)) {
            return
        }

        val receiverText = expression.receiverExpression.text
        val receiverSimpleType = receiverText.substringAfterLast('.')
        val selector = expression.selectorExpression

        if (selector is KtCallExpression) {
            val calleeName = selector.calleeExpression?.text
            val isNoArgsCall = selector.valueArguments.isEmpty()

            if (isClockSystemReceiver(receiverText, ktFile)) {
                report(
                    Finding(
                        entity = Entity.from(expression),
                        message = "Do not use '$receiverText.$calleeName(...)' directly. " +
                            "Pass an explicit injected Clock instance for UTC consistency and testability."
                    )
                )
                return
            }

            if (calleeName == "now" && isNoArgsCall && (receiverSimpleType in bannedJavaTimeNowReceivers)) {
                report(
                    Finding(
                        entity = Entity.from(expression),
                        message = "Do not use '$receiverText.now()' directly without a Clock. " +
                            "Pass an explicit Clock parameter (e.g., $receiverText.now(clock)) " +
                            "to guarantee UTC consistency and testability."
                    )
                )
                return
            }

            val isCurrentTimeMillis = calleeName == "currentTimeMillis" &&
                (receiverSimpleType == "System" || receiverText == "java.lang.System")
            if (isCurrentTimeMillis) {
                report(
                    Finding(
                        entity = Entity.from(expression),
                        message = "Do not use 'System.currentTimeMillis()' directly. " +
                            "Use an injected Clock instance for testability and UTC consistency."
                    )
                )
                return
            }

            val isCalendarInstance = calleeName == "getInstance" &&
                (receiverSimpleType == "Calendar" || receiverText == "java.util.Calendar")
            if (isCalendarInstance) {
                report(
                    Finding(
                        entity = Entity.from(expression),
                        message = "Do not use 'Calendar.getInstance()' directly. " +
                            "Use an injected Clock instance for testability and UTC consistency."
                    )
                )
                return
            }

            val isSystemDefaultZone = calleeName == "systemDefaultZone" &&
                (receiverSimpleType == "Clock" || receiverText == "java.time.Clock")
            if (isSystemDefaultZone) {
                report(
                    Finding(
                        entity = Entity.from(expression),
                        message = "Do not use 'Clock.systemDefaultZone()' directly. " +
                            "Inject a UTC Clock instance for testability and UTC consistency."
                    )
                )
                return
            }
        }
    }

    /**
     * Inspects call expressions to report forbidden direct constructor or top-level function calls for system time.
     *
     * @param expression The call expression PSI element to inspect.
     */
    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val ktFile = expression.containingKtFile
        if (isTestFile(ktFile)) {
            return
        }

        val calleeText = expression.calleeExpression?.text
        val calleeSimpleName = calleeText?.substringAfterLast('.')
        val isNoArgsCall = expression.valueArguments.isEmpty()

        if ((calleeSimpleName == "Date") && isNoArgsCall) {
            report(
                Finding(
                    entity = Entity.from(expression),
                    message = "Do not instantiate 'Date()' directly. Use a centralized injected Clock provider " +
                        "for testability and UTC consistency."
                )
            )
            return
        }

        if ((calleeSimpleName == "now") && isNoArgsCall && importsClockSystemNow(ktFile)) {
            report(
                Finding(
                    entity = Entity.from(expression),
                    message = "Do not call 'now()' directly via static import of Clock.System.now. " +
                        "Pass an explicit injected Clock instance for UTC consistency and testability."
                )
            )
            return
        }
    }

    /**
     * Checks if the given receiver text corresponds to `Clock.System` from `kotlinx.datetime`.
     *
     * @param receiverText The raw text of the receiver expression.
     * @param ktFile The containing Kotlin file to check for import directives.
     * @return `true` if the receiver refers to `Clock.System`, `false` otherwise.
     */
    private fun isClockSystemReceiver(
        receiverText: String,
        ktFile: KtFile
    ): Boolean =
        (receiverText == "Clock.System") ||
            (receiverText.endsWith(".Clock.System")) ||
            (receiverText == "System" && importsClockSystem(ktFile))

    /**
     * Checks if the given file imports `kotlinx.datetime.Clock.System` or wildcard package imports.
     *
     * @param ktFile The Kotlin file to inspect.
     * @return `true` if `Clock.System` is imported, `false` otherwise.
     */
    private fun importsClockSystem(ktFile: KtFile): Boolean =
        ktFile.importDirectives.any { importDirective ->
            val importedFqName = importDirective.importedFqName?.asString()
            val isAllUnder = importDirective.isAllUnder
            importedFqName == "kotlinx.datetime.Clock.System" ||
                (importedFqName == "kotlinx.datetime.Clock" && isAllUnder) ||
                (importedFqName == "kotlinx.datetime" && isAllUnder)
        }

    /**
     * Checks if the given file statically imports `kotlinx.datetime.Clock.System.now`.
     *
     * @param ktFile The Kotlin file to inspect.
     * @return `true` if `Clock.System.now` is imported, `false` otherwise.
     */
    private fun importsClockSystemNow(ktFile: KtFile): Boolean =
        ktFile.importDirectives.any { importDirective ->
            importDirective.importedFqName?.asString() == "kotlinx.datetime.Clock.System.now"
        }

    /**
     * Checks if the file path indicates a test file or test fixture.
     *
     * @param ktFile The Kotlin file to check.
     * @return `true` if the file is a test file, `false` otherwise.
     */
    private fun isTestFile(ktFile: KtFile): Boolean {
        val filePath = ktFile.virtualFilePath.replace('\\', '/')
        val fileName = ktFile.name
        return fileName.endsWith("Test.kt") ||
            filePath.contains("/test/") ||
            filePath.contains("/testFixtures/") ||
            filePath.contains("/testing/")
    }

    /**
     * Companion object containing rule description constants.
     */
    public companion object {
        /**
         * Rule description message used by Detekt.
         */
        public const val RULE_DESCRIPTION: String =
            "Do not use system time directly. Inject a Clock instance for testability and UTC consistency."
    }
}
