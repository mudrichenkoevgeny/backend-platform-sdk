package io.github.mudrichenkoevgeny.backend.core.common.di.module

import io.github.mudrichenkoevgeny.backend.core.common.config.common.factory.CommonConfigFactory
import io.github.mudrichenkoevgeny.backend.core.common.config.common.factory.CommonConfigFactoryImpl
import io.github.mudrichenkoevgeny.backend.core.common.config.common.model.CommonConfig
import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.common.config.pathresolver.PathResolver
import io.github.mudrichenkoevgeny.backend.core.common.config.pathresolver.PathResolverImpl
import io.github.mudrichenkoevgeny.backend.core.common.config.swagger.factory.SwaggerConfigFactory
import io.github.mudrichenkoevgeny.backend.core.common.config.swagger.factory.SwaggerConfigFactoryImpl
import io.github.mudrichenkoevgeny.backend.core.common.config.swagger.model.SwaggerConfig
import io.github.mudrichenkoevgeny.backend.core.common.propertiesprovider.ApplicationPropertiesProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
interface BaseAppConfigModule {

    @Binds
    @Singleton
    fun bindPathResolver(pathResolverImpl: PathResolverImpl): PathResolver

    companion object {

        @Provides
        @Singleton
        fun provideCommonConfigFactory(
            envReader: EnvReader,
            applicationPropertiesProvider: ApplicationPropertiesProvider
        ): CommonConfigFactory {
            return CommonConfigFactoryImpl(
                envReader = envReader,
                propertiesProvider = applicationPropertiesProvider
            )
        }

        @Provides
        @Singleton
        fun provideCommonConfig(
            commonConfigFactory: CommonConfigFactory
        ): CommonConfig {
            return commonConfigFactory.create()
        }

        @Provides
        @Singleton
        fun provideSwaggerConfigFactory(
            envReader: EnvReader,
            applicationPropertiesProvider: ApplicationPropertiesProvider
        ): SwaggerConfigFactory {
            return SwaggerConfigFactoryImpl(
                envReader = envReader,
                propertiesProvider = applicationPropertiesProvider
            )
        }

        @Provides
        @Singleton
        fun provideSwaggerConfig(
            swaggerConfigFactory: SwaggerConfigFactory
        ): SwaggerConfig {
            return swaggerConfigFactory.create()
        }
    }
}