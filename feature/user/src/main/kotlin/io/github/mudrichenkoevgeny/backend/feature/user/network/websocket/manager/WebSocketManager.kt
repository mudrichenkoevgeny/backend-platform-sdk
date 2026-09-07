package io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager

import io.github.mudrichenkoevgeny.backend.core.common.route.ApiScope
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.ktor.server.websocket.DefaultWebSocketServerSession

/**
 * Central manager for active WebSocket connections and real-time frame dispatching.
 *
 * Coordinates socket registration, lifecycle tracking, and targeted frame dispatching across
 * API scopes, authenticated users, specific user sessions, and individual socket descriptors.
 */
interface WebSocketManager {

    /**
     * Registers a new [webSocketSession] within the manager and manages its lifecycle until closure.
     *
     * @param webSocketSession The raw Ktor server session instance.
     * @param apiScope The operational scope ([ApiScope.OPEN] or [ApiScope.MANAGEMENT]) defining the audience.
     * @param userId Optional [UserId] if the socket connection was authenticated via JWT.
     * @param userRole Optional [UserRole] associated with the authenticated principal.
     * @param userSessionId Optional [UserSessionId] tracking the persistent session backing the token.
     * @param userSessionExpiresAt Optional Unix epoch timestamp (in seconds/milliseconds) marking session expiry.
     */
    suspend fun register(
        webSocketSession: DefaultWebSocketServerSession,
        apiScope: ApiScope,
        userId: UserId?,
        userRole: UserRole?,
        userSessionId: UserSessionId?,
        userSessionExpiresAt: Long?
    )

    /**
     * Broadcasts a [SocketFrame] to all active socket connections across all scopes unconditionally.
     *
     * @param frame The envelope and payload to transmit.
     */
    suspend fun sendMessageToAll(frame: SocketFrame)

    /**
     * Broadcasts a [SocketFrame] exclusively to sockets matching the specified [ApiScope].
     *
     * Ensures strict isolation between public clients and administrative management workflows.
     *
     * @param scope Target boundary ([ApiScope.OPEN] or [ApiScope.MANAGEMENT]).
     * @param frame The envelope and payload to transmit.
     */
    suspend fun sendMessageToScope(scope: ApiScope, frame: SocketFrame)

    /**
     * Broadcasts a [SocketFrame] across all concurrent connections belonging to a specific [UserId].
     *
     * @param userId The recipient's user identifier.
     * @param frame The envelope and payload to transmit.
     */
    suspend fun sendMessageToUser(userId: UserId, frame: SocketFrame)

    /**
     * Broadcasts a [SocketFrame] to all connections mapped to an active [UserSessionId].
     *
     * @param userSessionId Identifier of the specific login session.
     * @param frame The envelope and payload to transmit.
     */
    suspend fun sendMessageToUserSession(userSessionId: UserSessionId, frame: SocketFrame)

    /**
     * Sends a [SocketFrame] to a single connection identified by its internal [socketId].
     *
     * @param socketId The unique connection session ID.
     * @param frame The envelope and payload to transmit.
     */
    suspend fun sendMessageToSocket(socketId: String, frame: SocketFrame)

    /**
     * Initiates an orderly shutdown and disconnection of a socket identified by [socketId].
     *
     * @param socketId The unique connection session ID to terminate.
     */
    suspend fun disconnectSocket(socketId: String)
}