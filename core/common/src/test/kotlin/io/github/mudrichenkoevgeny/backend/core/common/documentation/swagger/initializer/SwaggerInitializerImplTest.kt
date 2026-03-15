package io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.initializer

import io.github.mudrichenkoevgeny.backend.core.common.config.swagger.model.SwaggerConfig
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test

/**
 * Tests for [SwaggerInitializerImpl]: ensures `initialize(Application)` runs without throwing when applied to a test application.
 */
class SwaggerInitializerImplTest {

    @Test
    fun `initialize installs OpenApi plugin without throwing`() = testApplication {
        val config = SwaggerConfig(
            title = "Test API",
            description = "Test",
            version = "1.0",
            serverUrl = "http://localhost",
            serverDescription = "Local"
        )
        val initializer = SwaggerInitializerImpl(config)
        application {
            initializer.initialize(this)
        }
    }
}
