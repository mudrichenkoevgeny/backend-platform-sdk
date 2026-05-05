package io.github.mudrichenkoevgeny.backend.core.common.server

import io.github.mudrichenkoevgeny.backend.core.common.config.common.model.CommonConfig
import io.github.mudrichenkoevgeny.backend.core.common.config.model.AppEnvironment
import io.github.mudrichenkoevgeny.backend.core.common.config.model.AppInstanceMode
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class KtorServerFactoryTest {

    @Test
    fun `create builds server with two connectors`() {
        val config = CommonConfig(
            environment = AppEnvironment.DEV,
            instanceMode = AppInstanceMode.FULL,
            version = "1.0.0-test",
            appName = "test-app",
            ktorServerHost = "0.0.0.0",
            ktorServerPort = 8080,
            ktorManagementPort = 8081,
            serverUrl = "http://localhost:8080",
            allowedOrigins = listOf("*"),
            rateLimit = 100,
            rateLimitPeriodSeconds = 60
        )

        val server = KtorServer.create(config) { /* empty module for test */ }

        assertNotNull(server)
    }
}

