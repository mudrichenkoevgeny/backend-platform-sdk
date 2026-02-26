package io.github.mudrichenkoevgeny.backend.feature.user.di.module

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.messagehandler.WebSocketMessageHandler
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.messagehandler.UserWebSocketMessageHandler

@Module
interface UserWebSocketModule {
    @Binds
    @IntoSet
    fun bindUserWebSocketMessageHandler(
        userWebSocketMessageHandler: UserWebSocketMessageHandler
    ): WebSocketMessageHandler
}