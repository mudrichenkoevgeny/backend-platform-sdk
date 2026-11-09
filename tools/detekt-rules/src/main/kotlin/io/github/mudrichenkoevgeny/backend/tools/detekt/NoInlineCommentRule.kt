package io.github.mudrichenkoevgeny.backend.tools.detekt

import com.intellij.psi.PsiComment
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.RuleName

/**
 * Detekt rule prohibiting redundant inline comments in code bodies.
 */
public class NoInlineCommentRule(
    config: Config = Config.empty
) : Rule(
    config = config,
    description = RULE_DESCRIPTION
) {
    private val allowedPrefixes = listOf(
        "/**",
        "// todo",
        "// fixme",
        "// workaround",
        "// suppress",
        "// @",
        "// ---"
    )

    override val ruleName: RuleName = RuleName("NoInlineComment")

    override fun visitComment(comment: PsiComment) {
        super.visitComment(comment)
        val text = comment.text.trim().lowercase()
        val isAllowed = allowedPrefixes.any { text.startsWith(it) }
        if (!isAllowed) {
            report(
                Finding(
                    entity = Entity.from(comment),
                    message = "Redundant inline comment found: '${comment.text.trim()}'. " +
                        "Decompose steps into named functions or use KDoc for public APIs."
                )
            )
        }
    }

    public companion object {
        public const val RULE_DESCRIPTION: String =
            "Banal and redundant inline comments are prohibited. " +
                "Decompose complex logic into private functions, use KDoc, or use TODO/FIXME/workaround."
    }
}
