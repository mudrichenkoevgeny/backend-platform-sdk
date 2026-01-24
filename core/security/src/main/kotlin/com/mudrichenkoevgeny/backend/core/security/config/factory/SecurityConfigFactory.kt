package com.mudrichenkoevgeny.backend.core.security.config.factory

import com.mudrichenkoevgeny.backend.core.security.config.model.SecurityConfig

interface SecurityConfigFactory {
    fun create(): SecurityConfig
}