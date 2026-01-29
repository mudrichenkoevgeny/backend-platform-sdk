package io.github.mudrichenkoevgeny.backend.core.security.config.factory

import io.github.mudrichenkoevgeny.backend.core.security.config.model.SecurityConfig

interface SecurityConfigFactory {
    fun create(): SecurityConfig
}