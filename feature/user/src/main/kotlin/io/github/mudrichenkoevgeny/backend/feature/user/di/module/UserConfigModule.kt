package io.github.mudrichenkoevgeny.backend.feature.user.di.module

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.feature.user.config.factory.UserConfigFactory
import io.github.mudrichenkoevgeny.backend.feature.user.config.factory.UserConfigFactoryImpl
import io.github.mudrichenkoevgeny.backend.feature.user.config.model.UserConfig
import dagger.Module
import dagger.Provides
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AuthSettings
import javax.inject.Singleton

@Module
/**
 * Provides user feature configuration bindings.
 *
 * Binds:
 * - [UserConfigFactory] to [UserConfigFactoryImpl]
 * - [UserConfig] created at startup via the factory
 * - [AuthSettings] extracted from [UserConfig]
 */
class UserConfigModule {

    @Provides
    @Singleton
    fun provideUserConfigFactory(
        envReader: EnvReader
    ): UserConfigFactory {
        return UserConfigFactoryImpl(
            envReader = envReader
        )
    }

    @Provides
    @Singleton
    fun provideUserConfig(
        userConfigFactory: UserConfigFactory
    ): UserConfig {
        return userConfigFactory.create()
    }

    @Provides
    @Singleton
    fun provideAuthSettings(
        userConfig: UserConfig
    ): AuthSettings {
        return userConfig.authSettings
    }
}