package io.github.mudrichenkoevgeny.backend.feature.user.route.management.auth.login

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.auth.createTestAuthData
import io.github.mudrichenkoevgeny.backend.feature.user.network.application.setupOpenTestEnvironment
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.route.BaseRouterTest
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.login.LoginByEmailUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.login.LoginByTotpRecoveryCodeUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.login.LoginByTotpUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.github.mudrichenkoevgeny.shared.foundation.core.security.network.model.verifytotp.VerifyTotpPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
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

    @BeforeEach
    fun setUp() {
        clearMocks(
            loginByEmailUseCase,
            loginByTotpUseCase,
            loginByTotpRecoveryCodeUseCase
        )
    }

    @Test
    fun `login by email - success`() = testApplication {
        val request = LoginByEmailRequest("staff@example.com", "password123")
        val authData = createTestAuthData()

        coEvery {
            loginByEmailUseCase(
                email = request.email,
                password = request.password,
                requestContext = any<RequestContext>(),
                allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN),
                allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)
            )
        } returns AppResult.Success(authData)

        setupOpenTestEnvironment(router)

        val client = createClient {
            install(ClientContentNegotiation) {
                json(FoundationJson)
            }
        }

        val response = client.post(SelfManagementLoginRoutes.LOGIN_BY_EMAIL) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `login by totp - success`() = testApplication {
        val request = VerifyTotpPayload("mfa_token_123", "123456")
        val authData = createTestAuthData()

        coEvery {
            loginByTotpUseCase(
                requestContext = any<RequestContext>(),
                mfaToken = request.mfaToken,
                code = request.code,
                allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN),
                allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)
            )
        } returns AppResult.Success(authData)

        setupOpenTestEnvironment(router)

        val client = createClient {
            install(ClientContentNegotiation) {
                json(FoundationJson)
            }
        }

        val response = client.post(SelfManagementLoginRoutes.LOGIN_BY_TOTP) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `login by totp recovery code - success`() = testApplication {
        val request = VerifyTotpPayload("mfa_token_123", "RECOVERY_CODE_123")
        val authData = createTestAuthData()

        coEvery {
            loginByTotpRecoveryCodeUseCase(
                requestContext = any<RequestContext>(),
                mfaToken = request.mfaToken,
                code = request.code,
                allowedRoles = setOf(UserRole.STAFF, UserRole.ADMIN),
                allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)
            )
        } returns AppResult.Success(authData)

        setupOpenTestEnvironment(router)

        val client = createClient {
            install(ClientContentNegotiation) {
                json(FoundationJson)
            }
        }

        val response = client.post(SelfManagementLoginRoutes.LOGIN_BY_TOTP_RECOVERY_CODE) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }
}
