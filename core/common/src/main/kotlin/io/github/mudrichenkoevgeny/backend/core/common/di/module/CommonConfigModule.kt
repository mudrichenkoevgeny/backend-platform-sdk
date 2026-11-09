package io.github.mudrichenkoevgeny.backend.core.common.di.module

import dagger.Binds
import dagger.Module
import dagger.Provides
import io.github.mudrichenkoevgeny.backend.core.common.config.common.factory.CommonConfigFactory
import io.github.mudrichenkoevgeny.backend.core.common.config.common.factory.CommonConfigFactoryImpl
import io.github.mudrichenkoevgeny.backend.core.common.config.common.model.CommonConfig
import io.github.mudrichenkoevgeny.backend.core.common.config.pathresolver.PathResolver
import io.github.mudrichenkoevgeny.backend.core.common.config.pathresolver.PathResolverConfig
import io.github.mudrichenkoevgeny.backend.core.common.config.pathresolver.PathResolverConfigHolder
import io.github.mudrichenkoevgeny.backend.core.common.config.pathresolver.PathResolverImpl
import io.github.mudrichenkoevgeny.backend.core.common.config.swagger.factory.SwaggerConfigFactory
import io.github.mudrichenkoevgeny.backend.core.common.config.swagger.factory.SwaggerConfigFactoryImpl
import io.github.mudrichenkoevgeny.backend.core.common.config.swagger.model.SwaggerConfig
import javax.inject.Singleton

/**
 * Wires configuration factories and their products used by the common layer.
 *
 * This module binds factory implementations and exposes:
 * - [PathResolverConfig] (via [PathResolverConfigHolder]) and [PathResolver];
 * - [CommonConfig] created by [CommonConfigFactory];
 * - [SwaggerConfig] created by [SwaggerConfigFactory].
 *
 * Applications may override these bindings or configuration holders if they need
 * different sources of configuration.
 */
@Module
public interface CommonConfigModule {
    @Binds
    @Singleton
    public fun bindPathResolver(pathResolverImpl: PathResolverImpl): PathResolver

    @Binds
    @Singleton
    public fun bindCommonConfigFactory(commonConfigFactoryImpl: CommonConfigFactoryImpl): CommonConfigFactory

    @Binds
    @Singleton
    public fun bindSwaggerConfigFactory(swaggerConfigFactoryImpl: SwaggerConfigFactoryImpl): SwaggerConfigFactory

    public companion object {
        @Provides
        @Singleton
        public fun providePathResolverConfig(): PathResolverConfig = PathResolverConfigHolder.get()

        @Provides
        @Singleton
        public fun provideCommonConfig(commonConfigFactory: CommonConfigFactory): CommonConfig =
            commonConfigFactory.create()

        @Provides
        @Singleton
        public fun provideSwaggerConfig(swaggerConfigFactory: SwaggerConfigFactory): SwaggerConfig =
            swaggerConfigFactory.create()
    }
}
