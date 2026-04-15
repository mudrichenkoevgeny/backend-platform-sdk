package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.login

import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.ClientInfo
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter.RateLimitEnforcer
import io.github.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import io.github.mudrichenkoevgeny.backend.feature.user.auth.verifier.ExternalAuthVerifier
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.auth.AuthManager
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AuthSettings
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AvailableAuthProviders
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.login.LoginByExternalAuthProviderUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LoginByExternalAuthProviderUseCaseTest {

    private val rateLimiterEnforcer = mockk<RateLimitEnforcer>()
    private val userAuditLogger = mockk<UserAuditLogger>(relaxed = true)
    private val authSettingsProvider = mockk<AuthSettingsProvider>()
    private val authManager = mockk<AuthManager>()

    @Test
    fun `execute returns ExternalIdMismatch when provider not supported`() = runBlocking {
        val verifier = mockk<ExternalAuthVerifier>()
        every { verifier.provider } returns io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider.GOOGLE
        val useCase = LoginByExternalAuthProviderUseCase(
            rateLimiterEnforcer = rateLimiterEnforcer,
            userAuditLogger = userAuditLogger,
            externalAuthVerifiers = setOf(verifier),
            authSettingsProvider = authSettingsProvider,
            authManager = authManager
        )
        val ctx = requestContext()
        coEvery { rateLimiterEnforcer.enforce(any(), any(), any(), any(), any(), any()) } returns AppResult.Success(Unit)
        every { authSettingsProvider.getSettings() } returns AppResult.Success(
            AuthSettings(availableAuthProviders = AvailableAuthProviders(primary = emptyList(), secondary = emptyList()))
        )

        val result = useCase.execute(
            authProviderKey = "unknown",
            token = "token",
            requestContext = ctx
        )

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.ExternalIdMismatch)
    }

    private fun requestContext() = RequestContext(
        traceId = null,
        userId = null,
        sessionId = null,
        clientInfo = CLIENT_INFO
    )

    private companion object {
        val CLIENT_INFO = ClientInfo(
            clientType = null,
            userAgent = null,
            ipAddress = null,
            language = null,
            host = null,
            origin = null,
            deviceId = null,
            deviceName = null,
            appVersion = null,
            operationSystemVersion = null
        )
    }
}
