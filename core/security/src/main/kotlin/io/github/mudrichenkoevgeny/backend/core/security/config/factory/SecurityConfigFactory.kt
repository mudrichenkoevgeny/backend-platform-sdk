package io.github.mudrichenkoevgeny.backend.core.security.config.factory

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.security.config.factory.SecurityConfigFactoryImpl
import io.github.mudrichenkoevgeny.backend.core.security.config.model.SecurityConfig

/**
 * Creates a [SecurityConfig] instance for the current runtime environment.
 *
 * The default implementation ([SecurityConfigFactoryImpl]) reads from the environment via
 * [EnvReader] and converts values into strongly typed configuration.
 */
interface SecurityConfigFactory {
    fun create(): SecurityConfig
}