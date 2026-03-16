package io.github.mudrichenkoevgeny.backend.core.common.config.pathresolver

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class PathResolverConfigHolderTest {

    @Test
    fun `get returns default config when not set`() {
        PathResolverConfigHolder.set(PathResolverConfig())
        val config = PathResolverConfigHolder.get()

        assertEquals(PathResolverConfig(), config)
    }

    @Test
    fun `set updates config returned by get`() {
        val root = File(".").absoluteFile
        val custom = PathResolverConfig(
            projectRoot = root,
            envFilePath = ".env",
            secretsDirPath = "secrets",
        )

        PathResolverConfigHolder.set(custom)

        val result = PathResolverConfigHolder.get()
        assertEquals(custom, result)
    }
}

