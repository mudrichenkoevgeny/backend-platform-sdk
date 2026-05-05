package io.github.mudrichenkoevgeny.backend.feature.user.route.open.auth.login

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
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
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.data.AuthData
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.AccessToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.RefreshToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.SessionToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
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
import kotlin.time.Clock

class OpenLoginRouterTest : BaseRouterTest() {

    private val loginByEmailUseCase = mockk<LoginByEmailUseCase>()
    private val loginByPhoneUseCase = mockk<LoginByPhoneUseCase>()
    private val loginByExternalAuthProviderUseCase = mockk<LoginByExternalAuthProviderUseCase>()
    private val sendLoginConfirmationToPhoneUseCase = mockk<SendLoginConfirmationToPhoneUseCase>()
    private val loginByTotpUseCase = mockk<LoginByTotpUseCase>()
    private val loginByTotpRecoveryCodeUseCase = mockk<LoginByTotpRecoveryCodeUseCase>()

    private val router = OpenLoginRouter(
        appLogger = appLogger,
        appErrorParser = appErrorParser,
        loginByEmailUseCase = loginByEmailUseCase,
        loginByPhoneUseCase = loginByPhoneUseCase,
        loginByExternalAuthProviderUseCase = loginByExternalAuthProviderUseCase,
        sendLoginConfirmationToPhoneUseCase = sendLoginConfirmationToPhoneUseCase,
        loginByTotpUseCase = loginByTotpUseCase,
        loginByTotpRecoveryCodeUseCase = loginByTotpRecoveryCodeUseCase
    )

    private val testAuthData = AuthData(
        userDetails = UserDetails(
            id = UserId.generate(),
            role = UserRole.USER,
            accountStatus = UserAccountStatus.ACTIVE,
            accountStatusBeforeDeletion = null,
            authorityLevel = 0,
            permissionCodes = emptySet(),
            isTotpEnabled = false,
            createdAt = Clock.System.now()
        ),
        sessionToken = SessionToken(
            accessToken = AccessToken("access-token"),
            refreshToken = RefreshToken("refresh-token"),
            expiresAt = Clock.System.now()
        )
    )

    @BeforeEach
    fun setUp() {
        clearMocks(
            loginByEmailUseCase,
            loginByPhoneUseCase,
            loginByExternalAuthProviderUseCase,
            sendLoginConfirmationToPhoneUseCase,
            loginByTotpUseCase,
            loginByTotpRecoveryCodeUseCase
        )
    }

    @Test
    fun `login by email - success`() = testApplication {
        setupOpenTestEnvironment(router)
        val jsonClient = createClient {
            install(ClientContentNegotiation) {
                json(FoundationJson)
            }
        }

        coEvery {
            loginByEmailUseCase(any(), any(), any<RequestContext>())
        } returns AppResult.Success(testAuthData)

        val response = jsonClient.post(OpenLoginRoutes.LOGIN_BY_EMAIL) {
            contentType(ContentType.Application.Json)
            setBody(LoginByEmailRequest(email = "test@test.com", password = "password"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `login by phone - success`() = testApplication {
        setupOpenTestEnvironment(router)
        val jsonClient = createClient {
            install(ClientContentNegotiation) {
                json(FoundationJson)
            }
        }

        coEvery {
            loginByPhoneUseCase(any(), any(), any<RequestContext>())
        } returns AppResult.Success(testAuthData)

        val response = jsonClient.post(OpenLoginRoutes.LOGIN_BY_PHONE) {
            contentType(ContentType.Application.Json)
            setBody(LoginByPhoneRequest(phoneNumber = "79991234567", confirmationCode = "123456"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `login by external auth provider - success`() = testApplication {
        setupOpenTestEnvironment(router)
        val jsonClient = createClient {
            install(ClientContentNegotiation) {
                json(FoundationJson)
            }
        }

        coEvery {
            loginByExternalAuthProviderUseCase(any(), any(), any<RequestContext>())
        } returns AppResult.Success(testAuthData)

        val response = jsonClient.post(OpenLoginRoutes.LOGIN_BY_EXTERNAL_AUTH_PROVIDER) {
            contentType(ContentType.Application.Json)
            setBody(LoginByExternalAuthProviderRequest(authProvider = "google", token = "external-token"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `send login confirmation to phone - success`() = testApplication {
        setupOpenTestEnvironment(router)
        val jsonClient = createClient {
            install(ClientContentNegotiation) {
                json(FoundationJson)
            }
        }

        val otpConfirmation = OtpConfirmation(
            retryAfterSeconds = 60,
            numberOfSymbols = 6,
            expirationSeconds = 300
        )

        coEvery {
            sendLoginConfirmationToPhoneUseCase(any(), any<RequestContext>())
        } returns AppResult.Success(otpConfirmation)

        val response = jsonClient.post(OpenLoginRoutes.SEND_LOGIN_CONFIRMATION_TO_PHONE) {
            contentType(ContentType.Application.Json)
            setBody(SendConfirmationToPhoneRequest(phoneNumber = "79991234567"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `login by totp - success`() = testApplication {
        setupOpenTestEnvironment(router)
        val jsonClient = createClient {
            install(ClientContentNegotiation) {
                json(FoundationJson)
            }
        }

        coEvery {
            loginByTotpUseCase(
                requestContext = any<RequestContext>(),
                mfaToken = any(),
                code = any()
            )
        } returns AppResult.Success(testAuthData)

        val response = jsonClient.post(OpenLoginRoutes.LOGIN_BY_TOTP) {
            contentType(ContentType.Application.Json)
            setBody(VerifyTotpPayload(mfaToken = "mfa-token", code = "123456"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `login by totp recovery code - success`() = testApplication {
        setupOpenTestEnvironment(router)
        val jsonClient = createClient {
            install(ClientContentNegotiation) {
                json(FoundationJson)
            }
        }

        coEvery {
            loginByTotpRecoveryCodeUseCase(
                requestContext = any<RequestContext>(),
                mfaToken = any(),
                code = any()
            )
        } returns AppResult.Success(testAuthData)

        val response = jsonClient.post(OpenLoginRoutes.LOGIN_BY_TOTP_RECOVERY_CODE) {
            contentType(ContentType.Application.Json)
            setBody(VerifyTotpPayload(mfaToken = "mfa-token", code = "recovery-123"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }
}