package io.github.mudrichenkoevgeny.backend.core.database.config.factory

import io.github.mudrichenkoevgeny.backend.core.database.config.model.DatabaseConfig

interface DatabaseConfigFactory {
    fun create(): DatabaseConfig
}