package io.github.mudrichenkoevgeny.backend.testing.architecture

import com.lemonappdev.konsist.api.KoModifier
import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.declaration.KoPropertyDeclaration
import com.lemonappdev.konsist.api.provider.KoKDocProvider
import com.lemonappdev.konsist.api.provider.modifier.KoModifierProvider
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class DocumentationArchitectureTest {
    @Test
    @Disabled("Fails on current codebase, keeping logic for future enforcement")
    fun `public declarations must have KDoc`() {
        Konsist.scopeFromProduction().declarations(includeNested = true, includeLocal = false)
            .filter {
                val modifiable = it as? KoModifierProvider
                modifiable != null &&
                    !modifiable.hasModifier(KoModifier.PRIVATE) &&
                    !modifiable.hasModifier(KoModifier.INTERNAL)
            }
            .filter { it is KoKDocProvider }
            .filter {
                val modifiable = it as? KoModifierProvider
                modifiable?.hasModifier(KoModifier.OVERRIDE) != true
            }
            .filter { declaration ->
                if (declaration is KoPropertyDeclaration) {
                    val parentClass = declaration.containingDeclaration as? KoClassDeclaration
                    parentClass?.hasModifier(KoModifier.DATA) != true
                } else {
                    true
                }
            }
            .assertTrue { (it as KoKDocProvider).hasKDoc }
    }
}
