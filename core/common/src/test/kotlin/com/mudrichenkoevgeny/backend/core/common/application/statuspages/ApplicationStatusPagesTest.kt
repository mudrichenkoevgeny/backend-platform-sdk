/*
package io.github.mudrichenkoevgeny.backend.core.common.application.statuspages

import io.github.mudrichenkoevgeny.backend.core.common.error.constants.CommonErrorCodes
import io.github.mudrichenkoevgeny.backend.core.common.error.model.ApiError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.validation.ValidationException
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

class ConfigureStatusPagesTest {

    private lateinit var appErrorParser: AppErrorParser
    private lateinit var appLogger: AppLogger

    @BeforeEach
    fun setUp() {
        appErrorParser = mockk()
        appLogger = mockk()
    }

    @Test
    fun `should respond with proper error for ValidationException`() = testApplication {
        // GIVEN
        val expectedError = CommonError.MissingRequiredField("test_field")
        val apiError = ApiError(
            id = "",
            code = expectedError.code,
            message = "Field is missing"
        )
        coEvery { appErrorParser.getApiError(expectedError) } returns apiError
        every { appLogger.logBusinessError(expectedError) } just runs

        application {
            install(ContentNegotiation) { json() }
            configureStatusPages(appErrorParser, appLogger)
            routing {
                get("/test") { throw ValidationException(expectedError) }
            }
        }

        // WHEN
        val response = client.get("/test")

        // THEN
        assertEquals(HttpStatusCode.BadRequest, response.status)
        coVerify { appErrorParser.getApiError(expectedError) }
        verify { appLogger.logBusinessError(expectedError) }
    }

    @Test
    fun `should respond with InvalidJsonBody for ContentTransformationException`() = testApplication {
        // GIVEN
        val apiError = ApiError(
            id = "",
            code = CommonErrorCodes.INVALID_JSON_BODY,
            message = "Invalid JSON"
        )
        coEvery { appErrorParser.getApiError(any<CommonError.InvalidJsonBody>()) } returns apiError
        every { appLogger.logBusinessError(any()) } just Runs

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

        // WHEN
        val response = client.post("/test") {
            contentType(ContentType.Application.Json)
            setBody("{invalid json}")
        }

        // THEN
        assertEquals(HttpStatusCode.BadRequest, response.status)
        coVerify { appErrorParser.getApiError(any<CommonError.InvalidJsonBody>()) }
        verify { appLogger.logBusinessError(any<CommonError.InvalidJsonBody>()) }
    }

    @Test
    fun `should respond with BadRequest for BadRequestException`() = testApplication {
        // GIVEN
        val apiError = ApiError(
            id = "",
            code = CommonErrorCodes.BAD_REQUEST,
            message = "Bad request"
        )
        coEvery { appErrorParser.getApiError(any<CommonError.BadRequest>()) } returns apiError
        every { appLogger.logBusinessError(any()) } just runs

        application {
            install(ContentNegotiation) { json() }
            configureStatusPages(appErrorParser, appLogger)
            routing {
                get("/test") { throw BadRequestException("bad request") }
            }
        }

        // WHEN
        val response = client.get("/test")

        // THEN
        assertEquals(HttpStatusCode.BadRequest, response.status)
        coVerify { appErrorParser.getApiError(any<CommonError.BadRequest>()) }
        verify { appLogger.logBusinessError(any()) }
    }


    @Test
    fun `should log and respond for generic Throwable`() = testApplication {
        // GIVEN
        val apiError = ApiError(
            id = "",
            code = CommonErrorCodes.THROWABLE,
            message = "Internal error"
        )
        coEvery { appErrorParser.getApiError(any<CommonError.Throwable>()) } returns apiError
        every { appLogger.logSystemError(any(), any(), any()) } just runs

        application {
            install(ContentNegotiation) { json() }
            configureStatusPages(appErrorParser, appLogger)
            routing {
                get("/test") { throw RuntimeException("boom") }
            }
        }

        // WHEN
        val response = client.get("/test")

        // THEN
        assertEquals(HttpStatusCode.InternalServerError, response.status)
        coVerify { appErrorParser.getApiError(any<CommonError.Throwable>()) }
        verify { appLogger.logSystemError(any(), any(), any()) }
    }
}*/
