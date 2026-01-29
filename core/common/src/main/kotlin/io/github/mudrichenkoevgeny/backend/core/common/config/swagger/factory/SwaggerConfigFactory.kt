package io.github.mudrichenkoevgeny.backend.core.common.config.swagger.factory

import io.github.mudrichenkoevgeny.backend.core.common.config.swagger.model.SwaggerConfig

interface SwaggerConfigFactory {
    fun create(): SwaggerConfig
}