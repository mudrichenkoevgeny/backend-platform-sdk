package io.github.mudrichenkoevgeny.backend.tools.detekt

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier

/**
 * Detekt rule prohibiting field injection (`@Inject lateinit var`) in favor of constructor injection.
 */
public class PreferConstructorInjectionRule(
    config: Config = Config.empty
) : Rule(
    config = config,
    description = RULE_DESCRIPTION
) {
    override val ruleName: RuleName = RuleName("PreferConstructorInjection")

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)

        if (property.hasExpectModifier()) return

        val hasInjectAnnotation = property.annotationEntries.any {
            val name = it.shortName?.asString()
            name == "Inject" || name == "javax.inject.Inject" || name == "jakarta.inject.Inject"
        }

        if (hasInjectAnnotation) {
            val isLateinit = property.modifierList?.hasModifier(KtTokens.LATEINIT_KEYWORD) == true

            if (isLateinit) {
                val containingFile = property.containingKtFile.name
                val isFrameworkEntryPoint = containingFile.contains("Bootstrap") ||
                    containingFile.contains(
                        "Module"
                    ) ||
                    containingFile.contains("Component") ||
                    containingFile.contains("Runner")

                if (!isFrameworkEntryPoint) {
                    report(
                        Finding(
                            entity = Entity.from(property),
                            message =
                            "Field injection is prohibited here. Refactor '${property.name}' to be injected " +
                                "via constructor (@Inject constructor(private val ${property.name}: ...))."
                        )
                    )
                }
            }
        }
    }

    public companion object {
        public const val RULE_DESCRIPTION: String =
            "Field injection (@Inject lateinit var) is prohibited for business logic classes. " +
                "Use constructor injection (@Inject constructor(...)) for better testability and immutability."
    }
}
