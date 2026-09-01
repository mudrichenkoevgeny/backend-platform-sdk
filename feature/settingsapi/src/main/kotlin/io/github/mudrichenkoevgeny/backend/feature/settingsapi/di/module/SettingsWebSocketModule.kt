package io.github.mudrichenkoevgeny.backend.feature.settingsapi.di.module

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import io.github.mudrichenkoevgeny.backend.feature.settingsapi.network.websockets.messagehandler.SettingsWebSocketMessageHandler
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.messagehandler.WebSocketMessageHandler

/**
 * Dagger module that contributes settings-related [WebSocketMessageHandler] implementations.
 *
 * Adds [SettingsWebSocketMessageHandler] into the global handler set via multibindings.
 */
@Module
interface SettingsWebSocketModule {
    @Binds
    @IntoSet
    fun bindSettingsWebSocketMessageHandler(
        settingsWebSocketMessageHandler: SettingsWebSocketMessageHandler
    ): WebSocketMessageHandler
}
