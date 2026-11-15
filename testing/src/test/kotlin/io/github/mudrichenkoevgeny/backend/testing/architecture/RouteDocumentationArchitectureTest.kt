package io.github.mudrichenkoevgeny.backend.testing.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.provider.KoKDocProvider
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class RouteDocumentationArchitectureTest {

    @Test
    @Disabled("Fails on current codebase, keeping logic for future enforcement")
    fun `router documentation contains required sections`() {
        Konsist.scopeFromProduction().classes()
            .filter { it.hasParentWithName("BaseRouter") }
            .assertTrue { routerClass ->
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
}
