package io.github.mudrichenkoevgeny.backend.core.common.di.module

import dagger.Module
import dagger.Provides
import io.github.mudrichenkoevgeny.backend.core.common.config.swagger.model.SwaggerConfig
import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.initializer.SwaggerInitializer
import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.initializer.SwaggerInitializerImpl
import javax.inject.Singleton

/**
 * Wires Swagger/OpenAPI initialization for Ktor applications.
 *
 * Uses [SwaggerConfig] and exposes a singleton [SwaggerInitializer] that
 * can be used to register OpenAPI documentation endpoints.
 */
@Module
public class SwaggerModule {
    @Provides
    @Singleton
    public fun provideSwaggerInitializer(swaggerConfig: SwaggerConfig): SwaggerInitializer = SwaggerInitializerImpl(
        config = swaggerConfig
    )
}
