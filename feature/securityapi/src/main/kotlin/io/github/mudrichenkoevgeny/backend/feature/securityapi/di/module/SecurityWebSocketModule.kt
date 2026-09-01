package io.github.mudrichenkoevgeny.backend.feature.securityapi.di.module

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import io.github.mudrichenkoevgeny.backend.feature.securityapi.network.websockets.messagehandler.SecurityWebSocketMessageHandler
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.messagehandler.WebSocketMessageHandler

/**
 * Dagger module that contributes security-related [WebSocketMessageHandler] implementations.
 *
 * Adds [SecurityWebSocketMessageHandler] into the global handler set via multibindings.
 */
@Module
interface SecurityWebSocketModule {
    @Binds
    @IntoSet
    fun bindSecurityWebSocketMessageHandler(
        securityWebSocketMessageHandler: SecurityWebSocketMessageHandler
    ): WebSocketMessageHandler
}