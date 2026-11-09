package io.github.mudrichenkoevgeny.backend.feature.user.di.module

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.KtorWebSocketManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.messagehandler.CommonWebSocketMessageHandler
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.messagehandler.SecurityWebSocketMessageHandler
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.messagehandler.SettingsWebSocketMessageHandler
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.messagehandler.UserWebSocketMessageHandler
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.messagehandler.WebSocketMessageHandler
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.sessionlistener.UserSessionExpirationListener
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.sessionlistener.WebSocketSessionListener
import javax.inject.Singleton

/**
 * Registers WebSocket message handlers and session listeners contributed by the user feature.
 *
 * Uses Dagger set multibindings for [WebSocketMessageHandler] and [WebSocketSessionListener].
 */
@Module
public interface UserWebSocketModule {
    @Binds
    @Singleton
    public fun bindWebSocketManager(impl: KtorWebSocketManager): WebSocketManager

    @Binds
    @IntoSet
    public fun bindCommonWebSocketMessageHandler(
        commonWebSocketMessageHandler: CommonWebSocketMessageHandler
    ): WebSocketMessageHandler

    @Binds
    @IntoSet
    public fun bindUserWebSocketMessageHandler(
        userWebSocketMessageHandler: UserWebSocketMessageHandler
    ): WebSocketMessageHandler

    @Binds
    @IntoSet
    public fun bindUserSessionExpirationListener(
        userSessionExpirationListener: UserSessionExpirationListener
    ): WebSocketSessionListener

    @Binds
    @IntoSet
    public fun bindSecurityWebSocketMessageHandler(
        securityWebSocketMessageHandler: SecurityWebSocketMessageHandler
    ): WebSocketMessageHandler

    @Binds
    @IntoSet
    public fun bindSettingsWebSocketMessageHandler(
        settingsWebSocketMessageHandler: SettingsWebSocketMessageHandler
    ): WebSocketMessageHandler
}
