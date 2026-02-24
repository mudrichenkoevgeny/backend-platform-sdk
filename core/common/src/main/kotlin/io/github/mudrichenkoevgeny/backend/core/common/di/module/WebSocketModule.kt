package io.github.mudrichenkoevgeny.backend.core.common.di.module

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.manager.KtorWebSocketManager
import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.messagehandler.CommonWebSocketMessageHandler
import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.messagehandler.WebSocketMessageHandler
import javax.inject.Singleton

@Module
interface WebSocketModule {

    @Binds
    @Singleton
    fun bindWebSocketManager(ktorWebSocketManager: KtorWebSocketManager): WebSocketManager

    @Binds
    @IntoSet
    fun bindCommonWebSocketMessageHandler(
        commonWebSocketMessageHandler: CommonWebSocketMessageHandler
    ): WebSocketMessageHandler
}