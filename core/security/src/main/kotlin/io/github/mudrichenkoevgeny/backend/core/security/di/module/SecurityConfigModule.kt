package io.github.mudrichenkoevgeny.backend.core.security.di.module

import dagger.Module
import dagger.Provides
import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.security.config.factory.SecurityConfigFactory
import io.github.mudrichenkoevgeny.backend.core.security.config.factory.SecurityConfigFactoryImpl
import io.github.mudrichenkoevgeny.backend.core.security.config.model.SecurityConfig
import javax.inject.Singleton

/**
 * Dagger module that wires security configuration.
 *
 * Provides:
 * - [SecurityConfigFactory] as [SecurityConfigFactoryImpl]
 * - [SecurityConfig] produced by the factory
 */
@Module
public class SecurityConfigModule {
    @Provides
    @Singleton
    public fun provideSecurityConfigFactory(envReader: EnvReader): SecurityConfigFactory = SecurityConfigFactoryImpl(
        envReader = envReader
    )

    @Provides
    @Singleton
    public fun provideSecurityConfig(securityConfigFactory: SecurityConfigFactory): SecurityConfig =
        securityConfigFactory.create()
}
