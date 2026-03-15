package io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Tests for [SwaggerConfig] path constants so that route setup and clients stay in sync.
 */
class SwaggerConfigTest {

    @Test
    fun `SWAGGER_UI_PATH has expected value`() {
        assertEquals("swagger", SwaggerConfig.SWAGGER_UI_PATH)
    }

    @Test
    fun `OPENAPI_JSON_PATH has expected value`() {
        assertEquals("/api.json", SwaggerConfig.OPENAPI_JSON_PATH)
    }
}
