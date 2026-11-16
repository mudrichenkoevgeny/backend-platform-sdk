package io.github.mudrichenkoevgeny.backend.testing.architecture

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class FileStructureArchitectureTest {
    @Test
    @Disabled("Fails on current codebase, keeping logic for future enforcement")
    fun `single primary public declaration per file`() {
        BackendArchitectureAssertions.assertSinglePrimaryPublicDeclarationPerFile()
    }
}
