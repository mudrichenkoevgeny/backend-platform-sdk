package io.github.mudrichenkoevgeny.backend.core.common.config.common.model

import io.github.mudrichenkoevgeny.backend.core.common.config.enums.AppEnvironment

data class CommonConfig(
    val environment: AppEnvironment,
    val version: String,
    val appName: String,
    val ktorServerHost: String,
    val ktorServerPort: Int,
    val ktorManagementPort: Int,
    val serverUrl: String,
    val allowedOrigins: List<String>,
    val rateLimit: Int,
    val rateLimitPeriodSeconds: Int
)