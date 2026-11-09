package io.github.mudrichenkoevgeny.backend.tools.detekt

import com.intellij.psi.PsiComment
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.RuleName
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

/**
 * Detekt rule prohibiting Fully Qualified Names (FQN) inside code bodies.
 */
public class NoFullyQualifiedNameRule(
    config: Config = Config.empty
) : Rule(
    config = config,
    description = RULE_DESCRIPTION
) {
    private val fqnRegex = Regex(
        """(?:kotlin|kotlinx|java|javax|android|androidx|com|org|io|net|dev)\.""" +
            """[a-z0-9_.]+\.[A-Z]\w+(?:\.[a-zA-Z0-9_]+)*"""
    )

    override val ruleName: RuleName = RuleName("NoFullyQualifiedName")

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)

        val kDocs = file.collectDescendantsOfType<KDoc>()
        for (kDoc in kDocs) {
            val textWithoutBackticks = kDoc.text.replace(
                Regex("`[^`]*`"),
                ""
            )
            val match = fqnRegex.find(textWithoutBackticks)?.value
            if (match != null) {
                report(
                    Finding(
                        entity = Entity.from(kDoc),
                        message = "Fully Qualified Name '$match' found in comment/documentation. " +
                            "Use simple imported class names in KDoc [SimpleClassName] as per project standards."
                    )
                )
            }
        }
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)

        val isImportOrPackage =
            expression.getStrictParentOfType<KtImportDirective>() != null ||
                expression.getStrictParentOfType<KtPackageDirective>() != null

        if (isImportOrPackage) return

        val isNestedInDotExpression = expression.getStrictParentOfType<KtDotQualifiedExpression>() != null
        if (isNestedInDotExpression) return

        val textWithoutStrings = expression.text
            .replace(
                Regex("\"\"\"[\\s\\S]*?\"\"\""),
                "\"\""
            ).replace(
                Regex("\"(?:\\\\\"|[^\"])*\""),
                "\"\""
            )

        val match = fqnRegex.find(textWithoutStrings)?.value
        if (match != null) {
            report(
                Finding(
                    entity = Entity.from(expression),
                    message = "Fully Qualified Name '$match' found in code. " +
                        "Import the symbol explicitly instead of using FQN."
                )
            )
        }
    }

    override fun visitComment(comment: PsiComment) {
        super.visitComment(comment)

        if (comment is KDoc) return

        val textWithoutBackticks = comment.text.replace(
            Regex("`[^`]*`"),
            ""
        )
        val match = fqnRegex.find(textWithoutBackticks)?.value
        if (match != null) {
            report(
                Finding(
                    entity = Entity.from(comment),
                    message = "Fully Qualified Name '$match' found in comment/documentation. " +
                        "Use simple imported class names in KDoc [SimpleClassName] as per project standards."
                )
            )
        }
    }

    public companion object {
        public const val RULE_DESCRIPTION: String =
            "Fully Qualified Names (FQN) in code bodies and comments are prohibited. Import the symbol explicitly."
    }
}
