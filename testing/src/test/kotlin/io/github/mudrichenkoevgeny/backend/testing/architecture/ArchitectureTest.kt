package io.github.mudrichenkoevgeny.backend.testing.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.jupiter.api.Test

internal class ArchitectureTest {
    @Test
    fun `config factories are annotated with singleton`() {
        BackendArchitectureAssertions.assertConfigFactoriesAnnotatedWithSingleton()
    }

    @Test
    fun `core common does not depend on other internal core or feature modules`() {
        val scope = Konsist.scopeFromDirectory("core/common/src/main")
        check(scope.files.isNotEmpty()) { "Konsist scope for core/common is empty!" }

        scope.imports.assertFalse { importDecl ->
            val name = importDecl.name
            val isOtherCore = name.contains(".backend.core.") && !name.contains(".backend.core.common")
            val isFeature = name.contains(".backend.feature.")
            isOtherCore || isFeature
        }
    }

    @Test
    fun `repositories do not depend on routes or usecases`() {
        BackendArchitectureAssertions.assertRepositoriesDoNotDependOnRoutesOrUsecases()
    }
}
