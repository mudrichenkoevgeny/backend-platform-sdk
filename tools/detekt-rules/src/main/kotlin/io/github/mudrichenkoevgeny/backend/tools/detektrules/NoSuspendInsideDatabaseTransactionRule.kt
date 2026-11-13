package io.github.mudrichenkoevgeny.backend.tools.detektrules

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

/**
 * Detekt rule prohibiting suspend or blocking calls inside Exposed database transactions.
 */
public class NoSuspendInsideDatabaseTransactionRule(
    config: Config = Config.empty
) : Rule(
    config = config,
    description = RULE_DESCRIPTION
) {
    override val ruleName: RuleName = RuleName("NoSuspendInsideDatabaseTransaction")

    private val forbiddenCalls = setOf(
        "delay",
        "withContext",
        "coroutineScope",
        "runBlocking",
        "await",
        "awaitAll",
        "yield",
        "launch",
        "async"
    )
    private val transactionMethods = setOf(
        "transaction",
        "newSuspendedTransaction",
        "suspendedTransactionAsync"
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val calleeName = expression.calleeExpression?.text ?: return
        if (calleeName !in transactionMethods) return

        val lambdas = expression.valueArguments.mapNotNull {
            it.getArgumentExpression() as? KtLambdaExpression
        } + expression.lambdaArguments.mapNotNull { it.getLambdaExpression() }

        lambdas.forEach { lambda ->
            val badCalls = lambda.collectDescendantsOfType<KtCallExpression> { call ->
                call.calleeExpression?.text in forbiddenCalls
            }

            badCalls.forEach { badCall ->
                report(
                    Finding(
                        entity = Entity.from(badCall),
                        message = "Suspension or blocking calls ('${badCall.calleeExpression?.text}') are prohibited " +
                            "inside an Exposed transaction block. Fetch required data before or after " +
                            "the transaction to avoid starving the DB connection pool."
                    )
                )
            }
        }
    }

    public companion object {
        public const val RULE_DESCRIPTION: String =
            "Do not suspend or block inside database transactions to prevent connection pool exhaustion."
    }
}
