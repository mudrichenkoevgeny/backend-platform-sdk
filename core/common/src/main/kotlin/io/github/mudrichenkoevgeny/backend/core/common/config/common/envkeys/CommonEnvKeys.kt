package io.github.mudrichenkoevgeny.backend.core.common.config.common.envkeys

import io.github.mudrichenkoevgeny.backend.core.common.config.common.factory.CommonConfigFactoryImpl
import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader

/**
 * Environment variable keys used to configure core application behavior.
 *
 * These keys are read by [EnvReader] consumers such as [CommonConfigFactoryImpl]
 * to build strongly typed configuration objects.
 */
public object CommonEnvKeys {
    public const val ENV_FILE: String = "ENV_FILE"
    public const val SECRETS_DIR: String = "SECRETS_DIR"
    public const val ENVIRONMENT: String = "ENVIRONMENT"
    public const val INSTANCE_MODE: String = "INSTANCE_MODE"
    public const val SERVER_URL: String = "SERVER_URL"
    public const val KTOR_SERVER_HOST: String = "KTOR_SERVER_HOST"
    public const val KTOR_SERVER_PORT: String = "KTOR_SERVER_PORT"
    public const val KTOR_MANAGEMENT_PORT: String = "KTOR_MANAGEMENT_PORT"
    public const val KTOR_SHUTDOWN_GRACE_PERIOD_MS: String = "KTOR_SHUTDOWN_GRACE_PERIOD_MS"
    public const val KTOR_SHUTDOWN_TIMEOUT_MS: String = "KTOR_SHUTDOWN_TIMEOUT_MS"
    public const val ALLOWED_ORIGINS: String = "ALLOWED_ORIGINS"
}
