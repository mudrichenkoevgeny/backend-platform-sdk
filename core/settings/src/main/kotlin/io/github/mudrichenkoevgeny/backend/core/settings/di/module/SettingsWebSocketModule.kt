package io.github.mudrichenkoevgeny.backend.core.settings.di.module

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.messagehandler.WebSocketMessageHandler
import io.github.mudrichenkoevgeny.backend.core.settings.network.websockets.messagehandler.SettingsWebSocketMessageHandler

@Module
interface SettingsWebSocketModule {
    @Binds
    @IntoSet
    fun bindSettingsWebSocketMessageHandler(
        settingsWebSocketMessageHandler: SettingsWebSocketMessageHandler
    ): WebSocketMessageHandler
}