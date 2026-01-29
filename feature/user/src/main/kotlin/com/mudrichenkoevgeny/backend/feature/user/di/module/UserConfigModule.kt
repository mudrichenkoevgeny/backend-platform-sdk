package io.github.mudrichenkoevgeny.backend.feature.user.di.module

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.feature.user.config.factory.UserConfigFactory
import io.github.mudrichenkoevgeny.backend.feature.user.config.factory.UserConfigFactoryImpl
import io.github.mudrichenkoevgeny.backend.feature.user.config.model.UserConfig
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
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
}