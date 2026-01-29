package io.github.mudrichenkoevgeny.backend.core.common.di.module

import io.github.mudrichenkoevgeny.backend.core.common.config.swagger.model.SwaggerConfig
import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.initializer.SwaggerInitializer
import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.initializer.SwaggerInitializerImpl
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class SwaggerModule {

    @Provides
    @Singleton
    fun provideSwaggerInitializer(
        swaggerConfig: SwaggerConfig
    ): SwaggerInitializer {
        return SwaggerInitializerImpl(
            config = swaggerConfig
        )
    }
}