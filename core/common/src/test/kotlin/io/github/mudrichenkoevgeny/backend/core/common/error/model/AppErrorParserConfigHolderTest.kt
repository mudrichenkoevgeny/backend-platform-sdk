package io.github.mudrichenkoevgeny.backend.core.common.error.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/**
 * Unit tests for [AppErrorParserConfigHolder].
 */
class AppErrorParserConfigHolderTest {

    @Test
    fun `get returns default config when not set`() {
        val config = AppErrorParserConfigHolder.get()
        assertNotNull(config)
    }

    @Test
    fun `set stores config and get retrieves it`() {
        val customConfig = AppErrorParserConfig(
            resourcePaths = listOf("custom_path"),
            supportedLocales = setOf("en", "de")
        )

        AppErrorParserConfigHolder.set(customConfig)

        val retrievedConfig = AppErrorParserConfigHolder.get()
        assertEquals(customConfig, retrievedConfig)
        assertEquals(listOf("custom_path"), retrievedConfig.resourcePaths)
        assertEquals(setOf("en", "de"), retrievedConfig.supportedLocales)
    }
}
