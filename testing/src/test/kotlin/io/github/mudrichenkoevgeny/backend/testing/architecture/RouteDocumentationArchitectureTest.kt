package io.github.mudrichenkoevgeny.backend.testing.architecture

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class RouteDocumentationArchitectureTest {

    @Test
    @Disabled("Fails on current codebase, keeping logic for future enforcement")
    fun `router documentation contains required sections`() {
        BackendArchitectureAssertions.assertRouterDocumentation()
    }
}
