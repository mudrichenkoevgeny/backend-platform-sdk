package io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.model

import io.github.mudrichenkoevgeny.backend.core.common.route.ApiScope
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import kotlinx.serialization.Serializable

/**
 * Message payload published to Redis Pub/Sub for broadcasting WebSocket events across the cluster.
 *
 * Each Ktor instance listens for these messages and dispatches the embedded [frame]
 * to the appropriate locally-connected clients based on [targetType] and target identifiers.
 *
 * @param targetType Defines the audience of the message (all users, specific user, single socket, etc.).
 * @param targetId The identifier for the target. Null when [targetType] is [TargetType.ALL] or [TargetType.SCOPE].
 * @param apiScope The required API scope of the target clients. Null if the message is not scoped.
 * @param frame The actual payload frame that will be serialized to JSON and sent to the WebSocket clients.
 */
@Serializable
data class WebSocketPubSubMessage(
    val targetType: TargetType,
    val targetId: String?,
    val apiScope: ApiScope?,
    val frame: SocketFrame
) {
    /**
     * Defines the granularity of the message recipient.
     */
    enum class TargetType {
        /** Message should be delivered to all connected clients. */
        ALL,
        /** Message should be delivered to all clients connected under a specific [ApiScope]. */
        SCOPE,
        /** Message should be delivered to all active sessions of a specific User ID. */
        USER,
        /** Message should be delivered to a specific User Session ID. */
        SESSION,
        /** Message should be delivered directly to a specific socket connection ID. */
        SOCKET
    }
}