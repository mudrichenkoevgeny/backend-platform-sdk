package io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.messagehandler

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.WebSocketSessionContext
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientType
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.contract.CommonApiFields
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.contract.CommonWebSocketEventTypes
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.WebSocketInitializePayload
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.uuid.Uuid

class CommonWebSocketMessageHandlerTest {

    private val handler = CommonWebSocketMessageHandler()
    private val dummyContext = WebSocketSessionContext(
        socketSessionId = "socket-1",
        clientInfo = null,
        userId = null,
        userRole = null,
        userSessionId = null,
    )

    @Test
    fun `PING produces PONG frame`() = runSuspend {
        val frame = SocketFrame(
            id = Uuid.random().toHexDashString(),
            type = CommonWebSocketEventTypes.PING,
            timestamp = System.currentTimeMillis(),
            payload = null,
        )

        val result = handler.handle(frame, dummyContext)

        val send = Assertions.assertInstanceOf(WebSocketMessageHandlerResult.SendSocketFrame::class.java, result)
        assertEquals(CommonWebSocketEventTypes.PONG, send.socketFrame.type)
    }

    @Test
    fun `PONG is treated as handled`() = runSuspend {
        val frame = socketFrame(type = CommonWebSocketEventTypes.PONG)

        val result = handler.handle(frame, dummyContext)

        assertEquals(WebSocketMessageHandlerResult.Handled, result)
    }

    @Test
    fun `INITIALIZE without payload returns MissingRequiredField error`() = runSuspend {
        val frame = socketFrame(
            type = CommonWebSocketEventTypes.INITIALIZE,
            payload = null,
        )

        val result = handler.handle(frame, dummyContext)

        val error = Assertions.assertInstanceOf(WebSocketMessageHandlerResult.Error::class.java, result)
        Assertions.assertInstanceOf(CommonError.MissingRequiredField::class.java, error.appError)
        val missing = error.appError as CommonError.MissingRequiredField
        assertEquals(CommonApiFields.PAYLOAD, missing.fieldName)
    }

    @Test
    fun `INITIALIZE with invalid payload returns InvalidJsonBody error`() = runSuspend {
        val frame = socketFrame(
            type = CommonWebSocketEventTypes.INITIALIZE,
            payload = JsonNull,
        )

        val result = handler.handle(frame, dummyContext)

        val error = Assertions.assertInstanceOf(WebSocketMessageHandlerResult.Error::class.java, result)
        Assertions.assertInstanceOf(CommonError.InvalidJsonBody::class.java, error.appError)
    }

    @Test
    fun `INITIALIZE with valid payload returns InitializeClient with success frame`() = runSuspend {
        val payload = WebSocketInitializePayload(
            clientType = ClientType.WEB.serialName,
            language = "en",
            deviceId = "device-1",
            deviceName = "Pixel",
            appVersion = "1.2.3",
            operationSystemVersion = "Android 15",
            apiVersion = "1"
        )
        val payloadElement = FoundationJson.encodeToJsonElement(WebSocketInitializePayload.serializer(), payload)

        val frame = socketFrame(
            type = CommonWebSocketEventTypes.INITIALIZE,
            payload = payloadElement,
        )

        val result = handler.handle(frame, dummyContext)

        val init = Assertions.assertInstanceOf(WebSocketMessageHandlerResult.InitializeClient::class.java, result)
        assertEquals(CommonWebSocketEventTypes.INITIALIZED_SUCCESS, init.socketFrame.type)
        assertEquals(payload, init.payload)
    }

    @Test
    fun `unknown type is not handled`() = runSuspend {
        val frame = socketFrame(type = "UNKNOWN_TYPE")

        val result = handler.handle(frame, dummyContext)

        assertEquals(WebSocketMessageHandlerResult.NotHandled, result)
    }

    private fun socketFrame(
        type: String,
        payload: JsonElement? = buildJsonObject { },
    ): SocketFrame =
        SocketFrame(
            id = Uuid.random().toHexDashString(),
            type = type,
            timestamp = System.currentTimeMillis(),
            payload = payload,
        )

    private fun runSuspend(block: suspend () -> Unit) {
        runBlocking { block() }
    }
}