package io.github.mudrichenkoevgeny.backend.feature.user.route.open.auth.login

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.auth.createTestAuthData
import io.github.mudrichenkoevgeny.backend.feature.user.network.application.setupOpenTestEnvironment
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.route.BaseRouterTest
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.login.LoginByEmailUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.login.LoginByExternalAuthProviderUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.login.LoginByPhoneUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.login.LoginByTotpRecoveryCodeUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.login.LoginByTotpUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.login.SendLoginConfirmationToPhoneUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.otpconfirmation.OtpConfirmation
import io.github.mudrichenkoevgeny.shared.foundation.core.security.network.model.verifytotp.VerifyTotpPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.auth.login.LoginByEmailRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.auth.login.LoginByExternalAuthProviderRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.auth.login.LoginByPhoneRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.confirmation.SendConfirmationToPhoneRequest
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.route.open.auth.login.OpenLoginRoutes
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OpenLoginRouterTest : BaseRouterTest() {

    private val loginByEmailUseCase = mockk<LoginByEmailUseCase>()
    private val loginByPhoneUseCase = mockk<LoginByPhoneUseCase>()
    private val loginByExternalAuthProviderUseCase = mockk<LoginByExternalAuthProviderUseCase>()
    private val loginByTotpUseCase = mockk<LoginByTotpUseCase>()
    private val loginByTotpRecoveryCodeUseCase = mockk<LoginByTotpRecoveryCodeUseCase>()
    private val sendLoginConfirmationToPhoneUseCase = mockk<SendLoginConfirmationToPhoneUseCase>()

    private val openLoginRouter = OpenLoginRouter(
        appLogger = appLogger,
        appErrorParser = appErrorParser,
        loginByEmailUseCase = loginByEmailUseCase,
        loginByPhoneUseCase = loginByPhoneUseCase,
        loginByExternalAuthProviderUseCase = loginByExternalAuthProviderUseCase,
        loginByTotpUseCase = loginByTotpUseCase,
        loginByTotpRecoveryCodeUseCase = loginByTotpRecoveryCodeUseCase,
        sendLoginConfirmationToPhoneUseCase = sendLoginConfirmationToPhoneUseCase
    )

    @BeforeEach
    fun setUp() {
        clearMocks(
            loginByEmailUseCase,
            loginByPhoneUseCase,
            loginByExternalAuthProviderUseCase,
            loginByTotpUseCase,
            loginByTotpRecoveryCodeUseCase,
            sendLoginConfirmationToPhoneUseCase
        )
    }

    @Test
    fun `login by email - success`() = testApplication {
        val request = LoginByEmailRequest("test@example.com", "password123")
        val authData = createTestAuthData()

        coEvery { loginByEmailUseCase(request.email, request.password, any<RequestContext>(), any(), any()) } returns AppResult.Success(authData)

        setupOpenTestEnvironment(openLoginRouter)

        val client = createClient {
            install(ClientContentNegotiation) {
                json(FoundationJson)
            }
        }

        val response = client.post(OpenLoginRoutes.LOGIN_BY_EMAIL) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `login by phone - success`() = testApplication {
        val request = LoginByPhoneRequest("+1234567890", "123456")
        val authData = createTestAuthData()

        coEvery { loginByPhoneUseCase(request.phoneNumber, request.confirmationCode, any<RequestContext>(), any(), any()) } returns AppResult.Success(authData)

        setupOpenTestEnvironment(openLoginRouter)

        val client = createClient {
            install(ClientContentNegotiation) {
                json(FoundationJson)
            }
        }

        val response = client.post(OpenLoginRoutes.LOGIN_BY_PHONE) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `login by external auth provider - success`() = testApplication {
        val request = LoginByExternalAuthProviderRequest("GOOGLE", "external_token_123")
        val authData = createTestAuthData()

        coEvery { loginByExternalAuthProviderUseCase(any(), request.externalProviderToken, any<RequestContext>(), any(), any()) } returns AppResult.Success(authData)

        setupOpenTestEnvironment(openLoginRouter)

        val client = createClient {
            install(ClientContentNegotiation) {
                json(FoundationJson)
            }
        }

        val response = client.post(OpenLoginRoutes.LOGIN_BY_EXTERNAL_AUTH_PROVIDER) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `login by totp - success`() = testApplication {
        val request = VerifyTotpPayload("mfa_token_123", "123456")
        val authData = createTestAuthData()

        coEvery { loginByTotpUseCase(any<RequestContext>(), request.mfaToken, request.code, any(), any()) } returns AppResult.Success(authData)

        setupOpenTestEnvironment(openLoginRouter)

        val client = createClient {
            install(ClientContentNegotiation) {
                json(FoundationJson)
            }
        }

        val response = client.post(OpenLoginRoutes.LOGIN_BY_TOTP) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `login by totp recovery code - success`() = testApplication {
        val request = VerifyTotpPayload("mfa_token_123", "RECOVERY_CODE_123")
        val authData = createTestAuthData()

        coEvery { loginByTotpRecoveryCodeUseCase(any<RequestContext>(), request.mfaToken, request.code, any(), any()) } returns AppResult.Success(authData)

        setupOpenTestEnvironment(openLoginRouter)

        val client = createClient {
            install(ClientContentNegotiation) {
                json(FoundationJson)
            }
        }

        val response = client.post(OpenLoginRoutes.LOGIN_BY_TOTP_RECOVERY_CODE) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `send login confirmation to phone - success`() = testApplication {
        val request = SendConfirmationToPhoneRequest("+1234567890")
        val otpConfirmation = OtpConfirmation(retryAfterSeconds = 60, numberOfSymbols = 6, expirationSeconds = 300)

        coEvery { sendLoginConfirmationToPhoneUseCase(request.phoneNumber, any<RequestContext>()) } returns AppResult.Success(otpConfirmation)

        setupOpenTestEnvironment(openLoginRouter)

        val client = createClient {
            install(ClientContentNegotiation) {
                json(FoundationJson)
            }
        }

        val response = client.post(OpenLoginRoutes.SEND_LOGIN_CONFIRMATION_TO_PHONE) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }
}
