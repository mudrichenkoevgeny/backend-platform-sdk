package com.mudrichenkoevgeny.backend.core.security.di.module

import com.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import com.mudrichenkoevgeny.backend.core.security.config.factory.SecurityConfigFactory
import com.mudrichenkoevgeny.backend.core.security.config.factory.SecurityConfigFactoryImpl
import com.mudrichenkoevgeny.backend.core.security.config.model.SecurityConfig
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

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