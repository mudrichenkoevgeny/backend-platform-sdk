package io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.validation.ValidationException
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.messagehandler.WebSocketMessageHandler
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.sessionlistener.WebSocketSessionListener
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.SerializationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KtorWebSocketManagerTest {

    private val appLogger = mockk<AppLogger>(relaxed = true)
    private val handlers: Set<WebSocketMessageHandler> = emptySet()
    private val listeners: Set<WebSocketSessionListener> = emptySet()

    private val manager = KtorWebSocketManager(appLogger, handlers, listeners)

    @Test
    fun `handleSocketError logs correct appError for ValidationException`() {
        val validationError = CommonError.MissingRequiredField("field")
        val validationException = ValidationException(validationError)

        invokeHandleSocketError(validationException)

        verify { appLogger.logError(validationError) }
    }

    @Test
    fun `handleSocketError logs InvalidJsonBody for SerializationException`() {
        val message = "bad json"
        val serializationException = SerializationException(message)

        invokeHandleSocketError(serializationException)

        verify {
            appLogger.logError(
                withArg { error ->
                    assertEquals(CommonError.InvalidJsonBody::class, error::class)
                }
            )
        }
    }

    @Test
    fun `handleSocketError logs Unknown for generic Throwable`() {
        val ex = RuntimeException("boom")

        invokeHandleSocketError(ex)

        verify {
            appLogger.logError(
                withArg { error ->
                    assertEquals(CommonError.Unknown::class, error::class)
                }
            )
        }
    }

    private fun invokeHandleSocketError(throwable: Throwable) {
        val method = KtorWebSocketManager::class.java.getDeclaredMethod("handleSocketError", Throwable::class.java)
        method.isAccessible = true
        method.invoke(manager, throwable)
    }
}