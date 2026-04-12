package io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.extractClientInfo
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.messagehandler.WebSocketMessageHandler
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.messagehandler.WebSocketMessageHandlerResult
import io.github.mudrichenkoevgeny.backend.core.common.validation.ValidationException
import io.github.mudrichenkoevgeny.backend.core.common.validation.validateDto
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.WebSocketSessionContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.sessionlistener.WebSocketSessionListener
import io.github.mudrichenkoevgeny.shared.foundation.core.common.mapper.websocket.mergeClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.contract.CommonApiFields
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.contract.CommonWebSocketCloseReasons
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.SerializationException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.Uuid

/**
 * Default [WebSocketManager] implementation backed by Ktor's WebSocket server APIs.
 *
 * Manages registration, routing of incoming frames to [WebSocketMessageHandler]s,
 * lifecycle notifications for [WebSocketSessionListener]s and error logging.
 */
@Singleton
class KtorWebSocketManager @Inject constructor(
    private val appLogger: AppLogger,
    private val webSocketMessageHandlers: Set<@JvmSuppressWildcards WebSocketMessageHandler>,
    private val webSocketSessionListeners: Set<@JvmSuppressWildcards WebSocketSessionListener>
) : WebSocketManager {

    private val webSocketSessionToContext = ConcurrentHashMap<DefaultWebSocketServerSession, WebSocketSessionContext>()
    private val socketIdToWebSocketSession = ConcurrentHashMap<String, DefaultWebSocketServerSession>()
    private val userIdToWebSocketSessions = ConcurrentHashMap<String, MutableSet<DefaultWebSocketServerSession>>()
    private val userSessionIdToWebSocketSessions = ConcurrentHashMap<String, MutableSet<DefaultWebSocketServerSession>>()

    override suspend fun register(
        webSocketSession: DefaultWebSocketServerSession,
        userId: UserId?,
        userRole: UserRole?,
        userSessionId: UserSessionId?,
        userSessionExpiresAt: Long?
    ) {
        val socketId = Uuid.random().toHexDashString()
        val clientInfo = webSocketSession.call.extractClientInfo()

        val context = WebSocketSessionContext(
            socketSessionId = socketId,
            userId = userId,
            userRole = userRole,
            userSessionId = userSessionId,
            clientInfo = clientInfo
        )

        webSocketSessionToContext[webSocketSession] = context
        socketIdToWebSocketSession[socketId] = webSocketSession

        userId?.let {
            userIdToWebSocketSessions
                .computeIfAbsent(it.asHexDashString()) { ConcurrentHashMap.newKeySet() }
                .add(webSocketSession)
        }
        userSessionId?.let {
            userSessionIdToWebSocketSessions
                .computeIfAbsent(it.asHexDashString()) { ConcurrentHashMap.newKeySet() }
                .add(webSocketSession)
        }

        webSocketSessionListeners.forEach { webSocketSessionListener ->
            webSocketSessionListener.onSessionRegistered(
                webSocketManager = this,
                session = webSocketSession,
                context = context,
                expiresAt = userSessionExpiresAt
            )
        }

        try {
            webSocketSession.handleIncoming(context)
        } finally {
            cleanup(webSocketSession, context)
            webSocketSessionListeners.forEach { webSocketSessionListener ->
                webSocketSessionListener.onSessionClosed(context)
            }
        }
    }

    override suspend fun sendMessageToAll(frame: SocketFrame) {
        webSocketSessionToContext.keys.forEach { session ->
            sendMessageToSession(session, frame)
        }
    }

    override suspend fun sendMessageToUser(userId: UserId, frame: SocketFrame) {
        userIdToWebSocketSessions[userId.asHexDashString()]?.forEach { session ->
            sendMessageToSession(session, frame)
        }
    }

    override suspend fun sendMessageToUserSession(userSessionId: UserSessionId, frame: SocketFrame) {
        userSessionIdToWebSocketSessions[userSessionId.asHexDashString()]?.forEach { session ->
            sendMessageToSession(session, frame)
        }
    }

    override suspend fun sendMessageToSocket(socketId: String, frame: SocketFrame) {
        socketIdToWebSocketSession[socketId]?.let { session ->
            sendMessageToSession(session, frame)
        }
    }

    override suspend fun disconnectSocket(socketId: String) {
        socketIdToWebSocketSession[socketId]?.close(
            CloseReason(
                code = CloseReason.Codes.NORMAL,
                message = CommonWebSocketCloseReasons.NORMAL
            )
        )
    }

    private suspend fun DefaultWebSocketServerSession.handleIncoming(context: WebSocketSessionContext) {
        try {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    val socketFrame = try {
                        FoundationJson.decodeFromString<SocketFrame>(text).apply { validateDto() }
                    } catch (e: Exception) {
                        handleSocketError(e)
                        continue
                    }
                    processSocketFrame(this, socketFrame, context)
                }
            }
        } catch (e: Exception) {
            if (e !is ClosedReceiveChannelException && e !is CancellationException) {
                appLogger.logError(CommonError.Internal(e))
            }
        }
    }

    private suspend fun processSocketFrame(
        session: DefaultWebSocketServerSession,
        socketFrame: SocketFrame,
        context: WebSocketSessionContext
    ) {
        var isHandled = false
        for (handler in webSocketMessageHandlers) {
            val result = handler.handle(socketFrame, context)
            if (result !is WebSocketMessageHandlerResult.NotHandled) {
                processResult(session, context, result)
                isHandled = true
                break
            }
        }

        if (!isHandled) {
            appLogger.logError(CommonError.InvalidFieldValue(CommonApiFields.TYPE))
        }
    }

    private suspend fun processResult(
        session: DefaultWebSocketServerSession,
        context: WebSocketSessionContext,
        result: WebSocketMessageHandlerResult
    ) {
        when (result) {
            is WebSocketMessageHandlerResult.InitializeClient -> {
                // todo wait for shared update
//                val existing = context.clientInfo ?: emptyClientInfoForWebSocketMerge()
//                context.clientInfo = result.payload.mergeClientInfo(existing)
//                sendMessageToSession(session, result.socketFrame)
            }
            is WebSocketMessageHandlerResult.SendSocketFrame -> sendMessageToSession(session, result.socketFrame)
            is WebSocketMessageHandlerResult.Error -> appLogger.logError(result.appError)
            else -> Unit
        }
    }

    private fun cleanup(session: DefaultWebSocketServerSession, context: WebSocketSessionContext) {
        webSocketSessionToContext.remove(session)
        socketIdToWebSocketSession.remove(context.socketSessionId)

        context.userId?.asHexDashString()?.let { uid ->
            userIdToWebSocketSessions[uid]?.let { sessions ->
                sessions.remove(session)
                if (sessions.isEmpty()) userIdToWebSocketSessions.remove(uid)
            }
        }

        context.userSessionId?.asHexDashString()?.let { sid ->
            userSessionIdToWebSocketSessions[sid]?.let { sessions ->
                sessions.remove(session)
                if (sessions.isEmpty()) userSessionIdToWebSocketSessions.remove(sid)
            }
        }
    }

    private suspend fun sendMessageToSession(session: DefaultWebSocketServerSession, frame: SocketFrame) {
        try {
            if (session.isActive) {
                session.send(Frame.Text(FoundationJson.encodeToString(frame)))
            }
        } catch (_: Exception) {}
    }

    private fun handleSocketError(e: Throwable) {
        val appError = when (e) {
            is ValidationException -> e.error
            is SerializationException -> CommonError.InvalidJsonBody(e.message)
            else -> CommonError.Unknown(e.message)
        }
        appLogger.logError(appError)
    }
}