package io.github.mudrichenkoevgeny.backend.core.common.network.websocket.manager

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.messagehandler.CommonWebSocketMessageHandler
import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.messagehandler.WebSocketMessageHandler
import io.github.mudrichenkoevgeny.backend.core.common.network.websocket.messagehandler.WebSocketMessageHandlerResult
import io.github.mudrichenkoevgeny.backend.core.common.validation.ValidationException
import io.github.mudrichenkoevgeny.backend.core.common.validation.validateDto
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.contract.CommonApiFields
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.SocketFrame
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.isActive
import kotlinx.serialization.SerializationException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class KtorWebSocketManager @Inject constructor(
    private val appLogger: AppLogger,
    private val commonHandler: CommonWebSocketMessageHandler,
    private val webSocketMessageHandlers: Set<@JvmSuppressWildcards WebSocketMessageHandler>
) : WebSocketManager {
    private val sessions = ConcurrentHashMap<DefaultWebSocketServerSession, String>()

    override suspend fun register(session: DefaultWebSocketServerSession, userId: String?) {
        sessions[session] = userId ?: ANONYMOUS_USER_ID
        try {
            session.handleIncoming(userId)
        } finally {
            sessions.remove(session)
        }
    }

    override suspend fun sendMessageToAllUsers(frame: SocketFrame) {
        val json = FoundationJson.encodeToString(frame)
        sessions.keys.forEach { it.sendMessage(json) }
    }

    override suspend fun sendMessageToUser(userId: String, frame: SocketFrame) {
        val json = FoundationJson.encodeToString(frame)
        sessions.filterValues { it == userId }.keys.forEach { it.sendMessage(json) }
    }

    private suspend fun DefaultWebSocketServerSession.handleIncoming(userId: String?) {
        try {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()

                    val socketFrame = try {
                        val decoded = FoundationJson.decodeFromString<SocketFrame>(text)
                        decoded.validateDto()
                        decoded
                    } catch (e: Exception) {
                        handleSocketError(e)
                        continue
                    }

                    handleSocketFrame(socketFrame, userId)
                }
            }
        } catch (e: Exception) {
            if (e !is ClosedReceiveChannelException && e !is CancellationException) {
                appLogger.logError(CommonError.Internal(e))
            }
        }
    }

    private suspend fun DefaultWebSocketServerSession.handleSocketFrame(socketFrame: SocketFrame, userId: String?) {
        val commonResult = commonHandler.handle(socketFrame, userId)
        if (commonResult is WebSocketMessageHandlerResult.Handled) {
            sendMessage(FoundationJson.encodeToString(commonResult.socketFrame))
            return
        }

        var isSocketFrameHandled = false
        for (handler in webSocketMessageHandlers) {
            val result = handler.handle(socketFrame, userId)
            if (result is WebSocketMessageHandlerResult.Handled) {
                sendMessage(FoundationJson.encodeToString(result.socketFrame))
                isSocketFrameHandled = true
                break
            }
        }

        if (!isSocketFrameHandled) {
            appLogger.logError(CommonError.InvalidFieldValue(CommonApiFields.TYPE))
        }
    }

    private fun handleSocketError(e: Throwable) {
        val appError = when (e) {
            is ValidationException -> e.error
            is SerializationException -> CommonError.InvalidJsonBody(e.message)
            else -> CommonError.Unknown(e.message)
        }
        appLogger.logError(appError)
    }

    private suspend fun DefaultWebSocketServerSession.sendMessage(json: String) {
        try {
            if (isActive) {
                send(Frame.Text(json))
            }
        } catch (_: Exception) {
            sessions.remove(this)
        }
    }

    companion object {
        private const val ANONYMOUS_USER_ID = "ANONYMOUS_USER_ID"
    }
}