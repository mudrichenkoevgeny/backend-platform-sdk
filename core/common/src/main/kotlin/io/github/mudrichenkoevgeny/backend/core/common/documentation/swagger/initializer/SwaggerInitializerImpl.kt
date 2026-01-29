package io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.initializer

import io.github.mudrichenkoevgeny.backend.core.common.config.swagger.model.SwaggerConfig
import io.github.smiley4.ktoropenapi.OpenApi
import io.github.smiley4.ktoropenapi.config.OutputFormat
import io.github.smiley4.ktoropenapi.config.SchemaGenerator
import io.ktor.server.application.Application
import io.ktor.server.application.install
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SwaggerInitializerImpl @Inject constructor(
    private val config: SwaggerConfig
): SwaggerInitializer {

    override fun initialize(application: Application) {
        application.install(OpenApi) {
            info {
                title = config.title
                version = config.version
                description = config.description
            }
            server {
                url = config.serverUrl
                description = config.serverDescription
            }
            schemas {
                generator = SchemaGenerator.kotlinx()
            }
            outputFormat = OutputFormat.JSON
        }
    }
}