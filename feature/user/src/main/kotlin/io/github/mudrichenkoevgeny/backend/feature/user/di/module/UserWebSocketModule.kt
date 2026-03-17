package io.github.mudrichenkoevgeny.backend.feature.user.di.module

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.messagehandler.WebSocketMessageHandler
import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.sessionlistener.WebSocketSessionListener
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.messagehandler.UserWebSocketMessageHandler
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.sessionlistener.UserSessionExpirationListener

@Module
/**
 * Registers WebSocket message handlers and session listeners contributed by the user feature.
 *
 * Uses Dagger set multibindings for [WebSocketMessageHandler] and [WebSocketSessionListener].
 */
interface UserWebSocketModule {
    @Binds
    @IntoSet
    fun bindUserWebSocketMessageHandler(
        userWebSocketMessageHandler: UserWebSocketMessageHandler
    ): WebSocketMessageHandler

    @Binds
    @IntoSet
    fun bindUserSessionExpirationListener(
        userSessionExpirationListener: UserSessionExpirationListener
    ): WebSocketSessionListener
}