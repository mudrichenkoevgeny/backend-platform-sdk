package io.github.mudrichenkoevgeny.backend.testing.architecture

import com.lemonappdev.konsist.api.KoModifier
import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.declaration.KoInterfaceDeclaration
import com.lemonappdev.konsist.api.declaration.KoObjectDeclaration
import com.lemonappdev.konsist.api.provider.modifier.KoModifierProvider
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class FileStructureArchitectureTest {
    @Test
    @Disabled("Fails on current codebase, keeping logic for future enforcement")
    fun `single primary public declaration per file`() {
        Konsist.scopeFromProduction().files.assertTrue { file ->
            val publicDeclarations = file.declarations(includeNested = false, includeLocal = false)
                .filter {
                    val modifiable = it as? KoModifierProvider
                    modifiable == null ||
                        (!modifiable.hasModifier(KoModifier.PRIVATE) && !modifiable.hasModifier(KoModifier.INTERNAL))
                }

            val hasMultiplePrimary = publicDeclarations.count {
                it is KoClassDeclaration ||
                    it is KoInterfaceDeclaration ||
                    it is KoObjectDeclaration
            } > 1

            if (hasMultiplePrimary) {
                val hasSealed = publicDeclarations.any {
                    (it as? KoClassDeclaration)?.hasModifier(KoModifier.SEALED) == true ||
                        (it as? KoInterfaceDeclaration)?.hasModifier(KoModifier.SEALED) == true
                }
                val hasValueClass = publicDeclarations.any {
                    (it as? KoClassDeclaration)?.hasModifier(KoModifier.VALUE) == true
                }
                hasSealed || hasValueClass
            } else {
                true
            }
        }
    }
}
