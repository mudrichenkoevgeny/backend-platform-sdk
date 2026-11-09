package io.github.mudrichenkoevgeny.backend.tools.detekt

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

/**
 * Detekt rule set provider for project-specific custom static analysis rules.
 */
public class CustomRuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSetId = RuleSetId("custom-rules")

    override fun instance(): RuleSet = RuleSet(
        ruleSetId,
        listOf(
            ::KtorConsistentRoutePermissionsRule,
            ::KtorDelegatedRouteHandlerRule,
            ::KtorRoutePathConstantRule,
            ::KtorStrictRouteDefinitionRule,
            ::NoDirectSystemEnvRule,
            ::NoFullyQualifiedNameRule,
            ::NoInlineCommentRule,
            ::NoSuspendInsideDatabaseTransactionRule,
            ::PreferConstructorInjectionRule,
            ::PreferValueOverNameRule,
            ::ProhibitNonSafeAssertionRule,
            ::ProhibitPrintlnRule,
            ::RequireUtcTimeRule
        )
    )
}
