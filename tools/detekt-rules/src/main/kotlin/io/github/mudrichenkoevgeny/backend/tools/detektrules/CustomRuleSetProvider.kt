package io.github.mudrichenkoevgeny.backend.tools.detektrules

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

/**
 * Detekt rule set provider for backend platform specific custom static analysis rules.
 */
public class CustomRuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSetId = RuleSetId("backend-rules")

    override fun instance(): RuleSet = RuleSet(
        ruleSetId,
        listOf(
            ::KtorConsistentRoutePermissionsRule,
            ::KtorDelegatedRouteHandlerRule,
            ::KtorRoutePathConstantRule,
            ::KtorStrictRouteDefinitionRule,
            ::NoDirectSystemEnvRule,
            ::NoSuspendInsideDatabaseTransactionRule,
            ::PreferConstructorInjectionRule,
            ::RequireUtcTimeRule
        )
    )
}
