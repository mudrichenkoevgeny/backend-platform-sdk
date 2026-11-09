package io.github.mudrichenkoevgeny.backend.feature.user.di.module

import dagger.Module
import dagger.Provides
import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.feature.user.config.factory.UserConfigFactory
import io.github.mudrichenkoevgeny.backend.feature.user.config.factory.UserConfigFactoryImpl
import io.github.mudrichenkoevgeny.backend.feature.user.config.model.UserConfig
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.ManagementAuthSettings
import javax.inject.Singleton

/**
 * Provides user feature configuration bindings.
 *
 * Binds:
 * - [UserConfigFactory] to [UserConfigFactoryImpl]
 * - [UserConfig] created at startup via the factory
 * - [ManagementAuthSettings] extracted from [UserConfig] for defaults and persistence seeding
 */
@Module
public class UserConfigModule {
    @Provides
    @Singleton
    public fun provideUserConfigFactory(envReader: EnvReader): UserConfigFactory = UserConfigFactoryImpl(
        envReader = envReader
    )

    @Provides
    @Singleton
    public fun provideUserConfig(userConfigFactory: UserConfigFactory): UserConfig = userConfigFactory.create()

    @Provides
    @Singleton
    public fun provideManagementAuthSettings(userConfig: UserConfig): ManagementAuthSettings =
        userConfig.managementAuthSettings
}
