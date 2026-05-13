package io.github.mudrichenkoevgeny.backend.feature.securityapi.api.route.management

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.securityapi.api.usecase.management.settings.UpdateSecuritySettingsUseCase
import io.github.mudrichenkoevgeny.backend.feature.securityapi.api.usecase.open.settings.GetSecuritySettingsUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.user.createTestUserDetails
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.network.application.setupManagementTestEnvironment
import io.github.mudrichenkoevgeny.backend.feature.user.network.route.BaseRouterTest
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.otpconfirmation.OtpConfirmation
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.PasswordPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.SecuritySettings
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.securitysettings.toSecuritySettingsPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.securityapi.network.route.management.security.settings.ManagementSecuritySettingsRoutes
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ManagementSecuritySettingsRouterTest : BaseRouterTest() {

    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val auditErrorConverter = mockk<AuditErrorConverter>()
    private val updateSecuritySettingsUseCase = mockk<UpdateSecuritySettingsUseCase>()
    private val getSecuritySettingsUseCase = mockk<GetSecuritySettingsUseCase>()

    private val router = ManagementSecuritySettingsRouter(
        authenticationProvider = authProvider,
        appLogger = appLogger,
        appErrorParser = appErrorParser,
        auditLogger = auditLogger,
        auditErrorConverter = auditErrorConverter,
        updateSecuritySettingsUseCase = updateSecuritySettingsUseCase,
        getSecuritySettingsUseCase = getSecuritySettingsUseCase
    )

    @BeforeEach
    fun setUp() {
        clearMocks(updateSecuritySettingsUseCase, getSecuritySettingsUseCase, auditErrorConverter)
    }

    private fun sampleSettings() = SecuritySettings(
        recentAuthenticationValiditySeconds = 300,
        recentAuthenticationValiditySecondsForManagement = 60,
        passwordPolicy = PasswordPolicy(
            minLength = 12,
            requireLetter = true,
            requireUpperCase = true,
            requireLowerCase = true,
            requireDigit = true,
            requireSpecialChar = true,
            commonPasswords = emptySet()
        ),
        otpConfirmation = OtpConfirmation(
            retryAfterSeconds = 60,
            numberOfSymbols = 6,
            expirationSeconds = 300
        ),
        mfaTokenExpirationSeconds = 600
    )

    @Test
    fun `update security settings - success when admin`() = testApplication {
        val token = setupManagementTestEnvironment(router)
        val jsonClient = createClient {
            install(ClientContentNegotiation) {
                json(FoundationJson)
            }
        }

        authProvider.shouldReturnSuccess(createTestUserDetails(role = UserRole.ADMIN))
        coEvery { updateSecuritySettingsUseCase(any(), any()) } returns AppResult.Success(Unit)

        val response = jsonClient.put(ManagementSecuritySettingsRoutes.UPDATE_SECURITY_SETTINGS) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(sampleSettings().toSecuritySettingsPayload())
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `update security settings - forbidden when user role`() = testApplication {
        val token = setupManagementTestEnvironment(router)
        val jsonClient = createClient {
            install(ClientContentNegotiation) {
                json(FoundationJson)
            }
        }
        val error = UserError.UserForbidden()

        authProvider.shouldReturnError(AppResult.Error(error))
        every { auditErrorConverter.convert(error) } returns AuditErrorLogData(AuditStatus.DENIED, emptySet())

        val response = jsonClient.put(ManagementSecuritySettingsRoutes.UPDATE_SECURITY_SETTINGS) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(sampleSettings().toSecuritySettingsPayload())
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `update security settings - unauthorized when invalid token`() = testApplication {
        val token = setupManagementTestEnvironment(router)
        val jsonClient = createClient {
            install(ClientContentNegotiation) {
                json(FoundationJson)
            }
        }

        val error = UserError.InvalidAccessToken()
        authProvider.shouldReturnError(AppResult.Error(error))

        every { auditErrorConverter.convert(error) } returns AuditErrorLogData(AuditStatus.DENIED, emptySet())

        val response = jsonClient.put(ManagementSecuritySettingsRoutes.UPDATE_SECURITY_SETTINGS) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(sampleSettings().toSecuritySettingsPayload())
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}