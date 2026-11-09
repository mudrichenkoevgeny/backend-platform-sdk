package io.github.mudrichenkoevgeny.backend.core.common.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test

internal class ArchitectureTest {
    @Test
    fun `config factories are annotated with singleton`() {
        Konsist
            .scopeFromProduction()
            .classes()
            .withNameEndingWith(
                "ConfigFactory",
                "ConfigFactoryImpl"
            ).assertTrue { classDecl ->
                classDecl.hasAnnotationWithName("Singleton") ||
                    classDecl.hasAnnotationWithName(
                        "javax.inject.Singleton"
                    )
            }
    }

    @Test
    fun `core common does not depend on other internal core or feature modules`() {
        Konsist.scopeFromDirectory("core/common/src/main").imports.assertFalse { importDecl ->
            importDecl.name.startsWith(
                "io.github.mudrichenkoevgeny.backend.core.database"
            ) ||
                importDecl.name.startsWith(
                    "io.github.mudrichenkoevgeny.backend.core.security"
                ) ||
                importDecl.name.startsWith(
                    "io.github.mudrichenkoevgeny.backend.core.storage"
                ) ||
                importDecl.name.startsWith(
                    "io.github.mudrichenkoevgeny.backend.core.audit"
                ) ||
                importDecl.name.startsWith(
                    "io.github.mudrichenkoevgeny.backend.core.settings"
                ) ||
                importDecl.name.startsWith(
                    "io.github.mudrichenkoevgeny.backend.core.events"
                ) ||
                importDecl.name.startsWith("io.github.mudrichenkoevgeny.backend.feature")
        }
    }

    @Test
    fun `repositories do not depend on routes or usecases`() {
        Konsist.scopeFromProduction().files.assertFalse { file ->
            val isRepositoryFile = file.name.endsWith("Repository.kt") || file.name.endsWith("RepositoryImpl.kt")
            if (!isRepositoryFile) return@assertFalse false
            file.imports.any { importDecl ->
                importDecl.name.contains(".route.") || importDecl.name.contains(".usecase.")
            }
        }
    }
}
