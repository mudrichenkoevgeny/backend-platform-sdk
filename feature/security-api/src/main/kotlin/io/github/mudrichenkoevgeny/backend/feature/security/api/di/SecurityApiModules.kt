package io.github.mudrichenkoevgeny.backend.feature.security.api.di

import dagger.Module
import io.github.mudrichenkoevgeny.backend.feature.security.api.di.module.SecurityWebSocketModule

/**
 * Dagger aggregation module for the `core/security` package.
 *
 * Includes:
 * - [SecurityWebSocketModule] (WebSocket handlers contributed into the global handler set)
 */
@Module(
    includes = [
        SecurityWebSocketModule::class
    ]
)
interface SecurityApiModules