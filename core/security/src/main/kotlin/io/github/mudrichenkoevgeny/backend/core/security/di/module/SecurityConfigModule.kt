package io.github.mudrichenkoevgeny.backend.core.security.di.module

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.security.config.factory.SecurityConfigFactory
import io.github.mudrichenkoevgeny.backend.core.security.config.factory.SecurityConfigFactoryImpl
import io.github.mudrichenkoevgeny.backend.core.security.config.model.SecurityConfig
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Dagger module that wires security configuration.
 *
 * Provides:
 * - [SecurityConfigFactory] as [SecurityConfigFactoryImpl]
 * - [SecurityConfig] produced by the factory
 */
@Module
class SecurityConfigModule {

    @Provides
    @Singleton
    fun provideSecurityConfigFactory(
        envReader: EnvReader
    ): SecurityConfigFactory {
        return SecurityConfigFactoryImpl(
            envReader = envReader
        )
    }

    @Provides
    @Singleton
    fun provideSecurityConfig(
        securityConfigFactory: SecurityConfigFactory
    ): SecurityConfig {
        return securityConfigFactory.create()
    }
}