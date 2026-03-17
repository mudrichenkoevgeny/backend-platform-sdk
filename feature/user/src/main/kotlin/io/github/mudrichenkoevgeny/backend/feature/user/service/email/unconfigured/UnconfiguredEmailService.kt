package io.github.mudrichenkoevgeny.backend.feature.user.service.email.unconfigured

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.EmailService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fallback [EmailService] implementation used when no real provider is configured.
 *
 * All methods return a consistent internal error so callers can fail fast and the host app
 * can detect misconfiguration early.
 */
@Singleton
class UnconfiguredEmailService @Inject constructor() : EmailService {
    private val error = CommonError.Internal(
        throwable = IllegalStateException(
            "Email service is not configured"
        )
    )
    override suspend fun sendVerificationCode(email: String, code: String, language: String?): AppResult<Unit> =
        AppResult.Error(error)

    override suspend fun sendResetPasswordVerificationCode(email: String, code: String, language: String?): AppResult<Unit> =
        AppResult.Error(error)

    override suspend fun sendAlreadyRegisteredEmail(
        email: String,
        ipAddress: String?,
        deviceName: String?,
        language: String?
    ): AppResult<Unit> =
        AppResult.Error(error)

    override suspend fun sendSuccessfulRegistrationEmail(email: String): AppResult<Unit> =
        AppResult.Error(error)

    override suspend fun sendSuccessfulLoginEmail(
        email: String,
        ipAddress: String?,
        deviceName: String?
    ): AppResult<Unit> =
        AppResult.Error(error)

    override suspend fun sendPasswordSuccessfullyChangedEmail(
        email: String,
        ipAddress: String?,
        deviceName: String?
    ): AppResult<Unit> =
        AppResult.Error(error)

    override suspend fun fakeSendEmail(): AppResult<Unit> =
        AppResult.Error(error)
}