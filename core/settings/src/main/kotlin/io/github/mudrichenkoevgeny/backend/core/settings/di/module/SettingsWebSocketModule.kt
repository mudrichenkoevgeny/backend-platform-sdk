package io.github.mudrichenkoevgeny.backend.core.settings.di.module

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.messagehandler.WebSocketMessageHandler
import io.github.mudrichenkoevgeny.backend.core.settings.network.websockets.messagehandler.SettingsWebSocketMessageHandler

/**
 * Dagger multibindings for settings-related WebSocket message handlers.
 *
 * Contributes [SettingsWebSocketMessageHandler] into the application-wide set of
 * [WebSocketMessageHandler] implementations.
 */
@Module
interface SettingsWebSocketModule {
    @Binds
    @IntoSet
    fun bindSettingsWebSocketMessageHandler(
        settingsWebSocketMessageHandler: SettingsWebSocketMessageHandler
    ): WebSocketMessageHandler
}