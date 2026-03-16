package io.github.mudrichenkoevgeny.backend.core.common.application.statuspages

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.validation.ValidationException
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.model.ApiErrorResponse
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ApplicationStatusPagesTest {

    private lateinit var appErrorParser: AppErrorParser
    private lateinit var appLogger: AppLogger

    @BeforeEach
    fun setUp() {
        appErrorParser = mockk()
        appLogger = mockk()
    }

    @Test
    fun `validation exception mapped to corresponding CommonError`() = testApplication {
        val expectedError = CommonError.MissingRequiredField("test_field")
        val apiError = ApiErrorResponse(
            id = "",
            code = expectedError.code,
            message = "Field is missing",
        )
        every { appLogger.logError(expectedError) } just runs
        coEvery { appErrorParser.getApiErrorResponse(expectedError) } returns apiError

        application {
            install(ContentNegotiation) { json() }
            configureStatusPages(appErrorParser, appLogger)
            routing {
                get("/test") { throw ValidationException(expectedError) }
            }
        }

        val response = client.get("/test")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        coVerify { appErrorParser.getApiErrorResponse(expectedError) }
        verify { appLogger.logError(expectedError) }
    }

    @Test
    fun `invalid json body mapped to InvalidJsonBody`() = testApplication {
        val apiError = ApiErrorResponse(
            id = "",
            code = CommonError.InvalidJsonBody(null).code,
            message = "Invalid JSON",
        )
        every { appLogger.logError(any<CommonError.InvalidJsonBody>()) } just Runs
        coEvery { appErrorParser.getApiErrorResponse(any<CommonError.InvalidJsonBody>()) } returns apiError

        application {
            install(ContentNegotiation) { json() }
            configureStatusPages(appErrorParser, appLogger)
            routing {
                post("/test") {
                    val body = call.receive<Map<String, String>>()
                    call.respond(status = HttpStatusCode.OK, message = body)
                }
            }
        }

        val response = client.post("/test") {
            contentType(ContentType.Application.Json)
            setBody("{invalid json}")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        coVerify { appErrorParser.getApiErrorResponse(any<CommonError.InvalidJsonBody>()) }
        verify { appLogger.logError(any<CommonError.InvalidJsonBody>()) }
    }

    @Test
    fun `bad request exception mapped to BadRequest error`() = testApplication {
        val apiError = ApiErrorResponse(
            id = "",
            code = CommonError.BadRequest(null).code,
            message = "Bad request",
        )
        every { appLogger.logError(any<CommonError.BadRequest>()) } just runs
        coEvery { appErrorParser.getApiErrorResponse(any<CommonError.BadRequest>()) } returns apiError

        application {
            install(ContentNegotiation) { json() }
            configureStatusPages(appErrorParser, appLogger)
            routing {
                get("/bad-request") { throw BadRequestException("bad request") }
            }
        }

        val response = client.get("/bad-request")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        coVerify { appErrorParser.getApiErrorResponse(any<CommonError.BadRequest>()) }
        verify { appLogger.logError(any<CommonError.BadRequest>()) }
    }

    @Test
    fun `generic throwable mapped to Internal error`() = testApplication {
        val apiError = ApiErrorResponse(
            id = "",
            code = CommonError.Internal(RuntimeException("boom")).code,
            message = "Internal error",
        )
        every { appLogger.logError(any<CommonError.Internal>()) } just runs
        coEvery { appErrorParser.getApiErrorResponse(any<CommonError.Internal>()) } returns apiError

        application {
            install(ContentNegotiation) { json() }
            configureStatusPages(appErrorParser, appLogger)
            routing {
                get("/throw") { throw RuntimeException("boom") }
            }
        }

        val response = client.get("/throw")

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        coVerify { appErrorParser.getApiErrorResponse(any<CommonError.Internal>()) }
        verify { appLogger.logError(any<CommonError.Internal>()) }
    }
}