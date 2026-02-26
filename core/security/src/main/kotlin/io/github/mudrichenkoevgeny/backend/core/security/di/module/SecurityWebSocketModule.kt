package io.github.mudrichenkoevgeny.backend.core.security.di.module

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.messagehandler.WebSocketMessageHandler
import io.github.mudrichenkoevgeny.backend.core.security.network.websockets.messagehandler.SecurityWebSocketMessageHandler

@Module
interface SecurityWebSocketModule {
    @Binds
    @IntoSet
    fun bindSecurityWebSocketMessageHandler(
        securityWebSocketMessageHandler: SecurityWebSocketMessageHandler
    ): WebSocketMessageHandler
}