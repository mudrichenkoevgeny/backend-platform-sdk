package io.github.mudrichenkoevgeny.backend.core.common.config.common.envkeys

import io.github.mudrichenkoevgeny.backend.core.common.config.common.factory.CommonConfigFactoryImpl
import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader

/**
 * Environment variable keys used to configure core application behavior.
 *
 * These keys are read by [EnvReader] consumers such as [CommonConfigFactoryImpl]
 * to build strongly typed configuration objects.
 */
object CommonEnvKeys {
    const val ENV_FILE = "ENV_FILE"
    const val SECRETS_DIR = "SECRETS_DIR"
    const val ENVIRONMENT = "ENVIRONMENT"
    const val SERVER_URL = "SERVER_URL"
    const val KTOR_SERVER_HOST = "KTOR_SERVER_HOST"
    const val KTOR_SERVER_PORT = "KTOR_SERVER_PORT"
    const val KTOR_MANAGEMENT_PORT = "KTOR_MANAGEMENT_PORT"
    const val ALLOWED_ORIGINS = "ALLOWED_ORIGINS"
    const val RATE_LIMIT = "RATE_LIMIT"
    const val RATE_LIMIT_PERIOD_SECONDS = "RATE_LIMIT_PERIOD_SECONDS"
}