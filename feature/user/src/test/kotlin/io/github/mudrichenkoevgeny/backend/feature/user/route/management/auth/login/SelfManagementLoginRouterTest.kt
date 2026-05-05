package io.github.mudrichenkoevgeny.backend.feature.user.route.management.auth.login

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.network.application.setupOpenTestEnvironment
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.route.BaseRouterTest
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.login.LoginByEmailUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.login.LoginByTotpRecoveryCodeUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.login.LoginByTotpUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
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
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.route.management.auth.login.SelfManagementLoginRoutes
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

class SelfManagementLoginRouterTest : BaseRouterTest() {

    private val loginByEmailUseCase = mockk<LoginByEmailUseCase>()
    private val loginByTotpUseCase = mockk<LoginByTotpUseCase>()
    private val loginByTotpRecoveryCodeUseCase = mockk<LoginByTotpRecoveryCodeUseCase>()

    private val router = SelfManagementLoginRouter(
        appLogger = appLogger,
        appErrorParser = appErrorParser,
        loginByEmailUseCase = loginByEmailUseCase,
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
        clearMocks(loginByEmailUseCase, loginByTotpUseCase, loginByTotpRecoveryCodeUseCase)
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
            loginByEmailUseCase(
                email = TEST_EMAIL,
                password = TEST_PASSWORD,
                requestContext = any<RequestContext>()
            )
        } returns AppResult.Success(testAuthData)

        val response = jsonClient.post(SelfManagementLoginRoutes.LOGIN_BY_EMAIL) {
            contentType(ContentType.Application.Json)
            setBody(LoginByEmailRequest(email = TEST_EMAIL, password = TEST_PASSWORD))
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
                mfaToken = TEST_MFA_TOKEN,
                code = TEST_CODE
            )
        } returns AppResult.Success(testAuthData)

        val response = jsonClient.post(SelfManagementLoginRoutes.LOGIN_BY_TOTP) {
            contentType(ContentType.Application.Json)
            setBody(VerifyTotpPayload(mfaToken = TEST_MFA_TOKEN, code = TEST_CODE))
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
                mfaToken = TEST_MFA_TOKEN,
                code = TEST_CODE,
                requestContext = any<RequestContext>()
            )
        } returns AppResult.Success(testAuthData)

        val response = jsonClient.post(SelfManagementLoginRoutes.LOGIN_BY_TOTP_RECOVERY_CODE) {
            contentType(ContentType.Application.Json)
            setBody(VerifyTotpPayload(mfaToken = TEST_MFA_TOKEN, code = TEST_CODE))
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    companion object {
        private const val TEST_EMAIL = "test@example.com"
        private const val TEST_PASSWORD = "password123"
        private const val TEST_MFA_TOKEN = "mfa-token-abc"
        private const val TEST_CODE = "123456"
    }
}