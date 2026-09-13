package io.github.mudrichenkoevgeny.backend.core.common.config.common.model

import io.github.mudrichenkoevgeny.backend.core.common.config.model.AppEnvironment
import io.github.mudrichenkoevgeny.backend.core.common.config.model.AppInstanceMode

fun createTestCommonConfig(
    environment: AppEnvironment = AppEnvironment.DEV,
    instanceMode: AppInstanceMode = AppInstanceMode.FULL,
    version: String = "1.0.0-test",
    appName: String = "test-app",
    ktorServerHost: String = "0.0.0.0",
    ktorServerPort: Int = 8080,
    ktorManagementPort: Int = 8081,
    serverUrl: String = "http://localhost:8080",
    allowedOrigins: List<String> = listOf("*")
) = CommonConfig(
    environment = environment,
    instanceMode = instanceMode,
    version = version,
    appName = appName,
    ktorServerHost = ktorServerHost,
    ktorServerPort = ktorServerPort,
    ktorManagementPort = ktorManagementPort,
    serverUrl = serverUrl,
    allowedOrigins = allowedOrigins
)
