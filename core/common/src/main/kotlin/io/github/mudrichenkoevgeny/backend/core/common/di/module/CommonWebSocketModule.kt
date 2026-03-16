package io.github.mudrichenkoevgeny.backend.core.common.di.module

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds
import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.manager.KtorWebSocketManager
import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.messagehandler.CommonWebSocketMessageHandler
import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.messagehandler.WebSocketMessageHandler
import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.sessionlistener.WebSocketSessionListener
import javax.inject.Singleton

/**
 * Configures common WebSocket infrastructure used by the library.
 *
 * Provides:
 * - [WebSocketManager] implementation;
 * - default [CommonWebSocketMessageHandler] contributed into a multibinding set of handlers;
 * - empty multibinding sets for message handlers and session listeners that
 *   can be extended by application and feature modules.
 */
@Module
interface CommonWebSocketModule {

    @Binds
    @Singleton
    fun bindWebSocketManager(ktorWebSocketManager: KtorWebSocketManager): WebSocketManager

    @Binds
    @IntoSet
    @Singleton
    fun bindCommonWebSocketMessageHandler(
        commonWebSocketMessageHandler: CommonWebSocketMessageHandler
    ): WebSocketMessageHandler

    @Multibinds
    fun bindWebSocketMessageHandlers(): Set<WebSocketMessageHandler>

    @Multibinds
    fun bindWebSocketSessionListeners(): Set<WebSocketSessionListener>
}