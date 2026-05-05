package io.github.mudrichenkoevgeny.backend.core.common.config.swagger.factory

import io.github.mudrichenkoevgeny.backend.core.common.config.common.envkeys.CommonEnvKeys
import io.github.mudrichenkoevgeny.backend.core.common.config.common.model.AppInfo
import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.common.config.swagger.envkeys.SwaggerEnvKeys
import io.github.mudrichenkoevgeny.backend.core.common.config.swagger.model.SwaggerConfig
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SwaggerConfigFactoryImplTest {

    private companion object {
        private const val APP_VERSION = "2.1.0"
        private const val SERVER_URL = "https://api.example.com"
        private const val SWAGGER_TITLE = "Test API"
        private const val SWAGGER_DESCRIPTION = "Test Description"
        private const val SWAGGER_SERVER_DESCRIPTION = "Production Server"
    }

    private val envReader = mockk<EnvReader>()
    private val appInfo = mockk<AppInfo> {
        every { version } returns APP_VERSION
    }

    @Test
    fun `create builds SwaggerConfig from env and appInfo`() {
        every { envReader.getByKey(CommonEnvKeys.SERVER_URL) } returns SERVER_URL
        every { envReader.getByKey(SwaggerEnvKeys.SWAGGER_TITLE) } returns SWAGGER_TITLE
        every { envReader.getByKey(SwaggerEnvKeys.SWAGGER_DESCRIPTION) } returns SWAGGER_DESCRIPTION
        every { envReader.getByKey(SwaggerEnvKeys.SWAGGER_SERVER_DESCRIPTION) } returns SWAGGER_SERVER_DESCRIPTION

        val factory = SwaggerConfigFactoryImpl(envReader, appInfo)

        val config: SwaggerConfig = factory.create()

        assertEquals(SWAGGER_TITLE, config.title)
        assertEquals(SWAGGER_DESCRIPTION, config.description)
        assertEquals(APP_VERSION, config.version)
        assertEquals(SERVER_URL, config.serverUrl)
        assertEquals(SWAGGER_SERVER_DESCRIPTION, config.serverDescription)
    }
}