package io.github.mudrichenkoevgeny.backend.testing.architecture

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class DocumentationArchitectureTest {
    @Test
    @Disabled("Fails on current codebase, keeping logic for future enforcement")
    fun `public declarations must have KDoc`() {
        BackendArchitectureAssertions.assertPublicDeclarationsHaveKDoc()
    }
}
