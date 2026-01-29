package io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.initializer

import io.ktor.server.application.Application

interface SwaggerInitializer {
    fun initialize(application: Application)
}