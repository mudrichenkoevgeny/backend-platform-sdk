package io.github.mudrichenkoevgeny.backend.testing.architecture

import com.lemonappdev.konsist.api.KoModifier
import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.container.KoScope
import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.declaration.KoInterfaceDeclaration
import com.lemonappdev.konsist.api.declaration.KoObjectDeclaration
import com.lemonappdev.konsist.api.declaration.KoPropertyDeclaration
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.provider.KoKDocProvider
import com.lemonappdev.konsist.api.provider.modifier.KoModifierProvider
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue

/**
 * Reusable assertions for backend architecture using Konsist.
 */
public object BackendArchitectureAssertions {

    /**
     * Verifies classes ending with "ConfigFactory" or "ConfigFactoryImpl" are annotated with "Singleton" (either standard or JSR-330).
     *
     * @param scope The [KoScope] to analyze.
     */
    public fun assertConfigFactoriesAnnotatedWithSingleton(scope: KoScope = Konsist.scopeFromProduction()) {
        val configFactories = scope.classes().withNameEndingWith("ConfigFactory", "ConfigFactoryImpl")
        if (configFactories.isEmpty()) return

        configFactories.assertTrue(testName = "assertConfigFactoriesAnnotatedWithSingleton") { classDecl ->
            classDecl.hasAnnotationWithName("Singleton") ||
                classDecl.hasAnnotationWithName("javax.inject.Singleton")
        }
    }

    /**
     * Verifies files ending with "Repository.kt" or "RepositoryImpl.kt" do not import packages containing ".route." or ".usecase.".
     *
     * @param scope The [KoScope] to analyze.
     */
    public fun assertRepositoriesDoNotDependOnRoutesOrUsecases(scope: KoScope = Konsist.scopeFromProduction()) {
        if (scope.files.isEmpty()) return

        scope.files.assertFalse(testName = "assertRepositoriesDoNotDependOnRoutesOrUsecases") { file ->
            val isRepositoryFile = file.name.endsWith("Repository.kt") || file.name.endsWith("RepositoryImpl.kt")
            if (!isRepositoryFile) return@assertFalse false
            file.imports.any { importDecl ->
                importDecl.name.contains(".route.") || importDecl.name.contains(".usecase.")
            }
        }
    }

    /**
     * Verifies public declarations have KDoc, excluding private/internal, override members, and data class properties.
     *
     * @param scope The [KoScope] to analyze.
     */
    public fun assertPublicDeclarationsHaveKDoc(scope: KoScope = Konsist.scopeFromProduction()) {
        val publicDeclarations = scope.declarations(includeNested = true, includeLocal = false)
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

        if (publicDeclarations.isEmpty()) return

        publicDeclarations.assertTrue(testName = "assertPublicDeclarationsHaveKDoc") { (it as KoKDocProvider).hasKDoc }
    }

    /**
     * Verifies there is only a single primary public declaration per file (allowing sealed or value class co-declarations).
     *
     * @param scope The [KoScope] to analyze.
     */
    public fun assertSinglePrimaryPublicDeclarationPerFile(scope: KoScope = Konsist.scopeFromProduction()) {
        if (scope.files.isEmpty()) return

        scope.files.assertTrue(testName = "assertSinglePrimaryPublicDeclarationPerFile") { file ->
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

    /**
     * Verifies that BaseRouter KDoc contains "Endpoint:" and "Authorization:".
     *
     * @param scope The [KoScope] to analyze.
     */
    public fun assertRouterDocumentation(scope: KoScope = Konsist.scopeFromProduction()) {
        val routers = scope.classes().filter { it.hasParentWithName("BaseRouter") }
        if (routers.isEmpty()) return

        routers.assertTrue(testName = "assertRouterDocumentation") { routerClass ->
            val classHasRequired = hasRequiredDocs(routerClass)

            val routeFunctions = routerClass.functions(includeNested = false, includeLocal = false)
                .filter { it.name.startsWith("register") }

            val funsHaveRequired = routeFunctions.any { hasRequiredDocs(it) }

            classHasRequired || funsHaveRequired || routeFunctions.isEmpty()
        }
    }

    private fun hasRequiredDocs(provider: KoKDocProvider): Boolean {
        val kdocText = provider.kDoc?.text ?: return false
        return kdocText.contains("Endpoint:") && kdocText.contains("Authorization:")
    }

    /**
     * Executes the active consumer-ready rules.
     *
     * @param scope The [KoScope] to analyze.
     */
    public fun assertAllConsumerRules(scope: KoScope = Konsist.scopeFromProduction()) {
        assertConfigFactoriesAnnotatedWithSingleton(scope)
        assertRepositoriesDoNotDependOnRoutesOrUsecases(scope)
    }
}
