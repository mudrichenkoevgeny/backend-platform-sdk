package io.github.mudrichenkoevgeny.backend.testing.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test

internal class ArchitectureTest {
    @Test
    fun `config factories are annotated with singleton`() {
        val classes = Konsist
            .scopeFromProduction()
            .classes()

        check(classes.isNotEmpty()) { "Konsist scopeFromProduction classes is empty!" }

        classes.withNameEndingWith(
            "ConfigFactory",
            "ConfigFactoryImpl"
        ).assertTrue { classDecl ->
            classDecl.hasAnnotationWithName("Singleton") ||
                classDecl.hasAnnotationWithName("javax.inject.Singleton")
        }
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
        val scope = Konsist.scopeFromProduction()
        check(scope.files.isNotEmpty()) { "Konsist scopeFromProduction files is empty!" }

        scope.files.assertFalse { file ->
            val isRepositoryFile = file.name.endsWith("Repository.kt") || file.name.endsWith("RepositoryImpl.kt")
            if (!isRepositoryFile) return@assertFalse false
            file.imports.any { importDecl ->
                importDecl.name.contains(".route.") || importDecl.name.contains(".usecase.")
            }
        }
    }
}
