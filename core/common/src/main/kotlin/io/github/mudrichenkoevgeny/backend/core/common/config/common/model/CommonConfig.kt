package io.github.mudrichenkoevgeny.backend.core.common.config.common.model

import io.github.mudrichenkoevgeny.backend.core.common.config.common.factory.CommonConfigFactoryImpl
import io.github.mudrichenkoevgeny.backend.core.common.config.model.AppEnvironment
import io.github.mudrichenkoevgeny.backend.core.common.config.model.AppInstanceMode

/**
 * Aggregated configuration for the core application runtime.
 *
 * Values are typically populated from environment variables by [CommonConfigFactoryImpl] and
 * describe basic service metadata, network configuration and global rate limiting.
 *
 * @param environment logical application environment.
 * @param instanceMode functional role of this specific application instance.
 * @param version current application version.
 * @param appName unique application name.
 * @param ktorServerHost host interface used by Ktor.
 * @param ktorServerPort main HTTP port exposed by Ktor.
 * @param ktorManagementPort management/health port exposed by Ktor.
 * @param serverUrl public base URL used in generated links and documentation.
 * @param allowedOrigins list of origins allowed by CORS.
 * @param rateLimit maximum number of requests per [rateLimitPeriodSeconds].
 * @param rateLimitPeriodSeconds period in seconds for calculating rate limits.
 */
data class CommonConfig(
    val environment: AppEnvironment,
    val instanceMode: AppInstanceMode,
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