package io.github.mudrichenkoevgeny.backend.core.common.config.common

import io.github.mudrichenkoevgeny.backend.core.common.config.common.envkeys.CommonEnvKeys
import io.github.mudrichenkoevgeny.backend.core.common.config.common.factory.CommonConfigFactoryImpl
import io.github.mudrichenkoevgeny.backend.core.common.config.common.model.AppInfo
import io.github.mudrichenkoevgeny.backend.core.common.config.common.model.CommonConfig
import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.common.config.model.AppEnvironment
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CommonConfigFactoryImplTest {

    private companion object {
        private const val ENV_DEV = "dev"
        private const val SERVER_URL = "http://localhost:8080"
        private const val KTOR_HOST = "0.0.0.0"
        private const val KTOR_PORT = "8080"
        private const val KTOR_MANAGEMENT_PORT = "8081"
        private const val ALLOWED_ORIGIN = "http://localhost:3000"
        private const val RATE_LIMIT = "100"
        private const val RATE_LIMIT_PERIOD = "60"
        private const val APP_NAME = "test-app"
        private const val APP_VERSION = "1.0.0"
    }

    private val envReader = mockk<EnvReader>()
    private val appInfo = mockk<AppInfo> {
        every { appName } returns APP_NAME
        every { version } returns APP_VERSION
    }

    @Test
    fun `create builds CommonConfig from env`() {
        every { envReader.getByKey(CommonEnvKeys.ENVIRONMENT) } returns ENV_DEV
        every { envReader.getByKey(CommonEnvKeys.SERVER_URL) } returns SERVER_URL
        every { envReader.getByKey(CommonEnvKeys.KTOR_SERVER_HOST) } returns KTOR_HOST
        every { envReader.getByKey(CommonEnvKeys.KTOR_SERVER_PORT) } returns KTOR_PORT
        every { envReader.getByKey(CommonEnvKeys.KTOR_MANAGEMENT_PORT) } returns KTOR_MANAGEMENT_PORT
        every { envReader.getByKeyOrNull(CommonEnvKeys.ALLOWED_ORIGINS) } returns ALLOWED_ORIGIN
        every { envReader.getByKey(CommonEnvKeys.RATE_LIMIT) } returns RATE_LIMIT
        every { envReader.getByKey(CommonEnvKeys.RATE_LIMIT_PERIOD_SECONDS) } returns RATE_LIMIT_PERIOD

        val factory = CommonConfigFactoryImpl(envReader, appInfo)

        val config: CommonConfig = factory.create()

        assertEquals(AppEnvironment.DEV, config.environment)
        assertEquals(APP_VERSION, config.version)
        assertEquals(APP_NAME, config.appName)
        assertEquals(KTOR_HOST, config.ktorServerHost)
        assertEquals(KTOR_PORT.toInt(), config.ktorServerPort)
        assertEquals(KTOR_MANAGEMENT_PORT.toInt(), config.ktorManagementPort)
        assertEquals(SERVER_URL, config.serverUrl)
        assertEquals(listOf(ALLOWED_ORIGIN), config.allowedOrigins)
        assertEquals(RATE_LIMIT.toInt(), config.rateLimit)
        assertEquals(RATE_LIMIT_PERIOD.toInt(), config.rateLimitPeriodSeconds)
    }
}

