package io.github.mudrichenkoevgeny.backend.feature.user.service.email

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult

interface EmailService {
    suspend fun sendVerificationCode(email: String, code: String, language: String?): AppResult<Unit>
    suspend fun sendResetPasswordVerificationCode(email: String, code: String, language: String?): AppResult<Unit>

    suspend fun sendAlreadyRegisteredEmail(email: String, ipAddress: String?, deviceName: String?, language: String?): AppResult<Unit>
    suspend fun sendSuccessfulRegistrationEmail(email: String): AppResult<Unit>
    suspend fun sendSuccessfulLoginEmail(email: String, ipAddress: String?, deviceName: String?): AppResult<Unit>
    suspend fun sendPasswordSuccessfullyChangedEmail(email: String, ipAddress: String?, deviceName: String?): AppResult<Unit>

    suspend fun fakeSendEmail(): AppResult<Unit>
}