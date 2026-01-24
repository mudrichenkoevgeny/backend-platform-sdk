package com.mudrichenkoevgeny.backend.feature.user.service.email

import com.mudrichenkoevgeny.backend.core.common.result.AppResult

interface EmailService {
    fun sendVerificationCode(email: String, code: String): AppResult<Unit>
    fun sendResetPasswordVerificationCode(email: String, code: String): AppResult<Unit>

    fun sendAlreadyRegisteredEmail(email: String, ipAddress: String?, deviceName: String?): AppResult<Unit>
    fun sendSuccessfulRegistrationEmail(email: String): AppResult<Unit>
    fun sendSuccessfulLoginEmail(email: String, ipAddress: String?, deviceName: String?): AppResult<Unit>
    fun sendPasswordSuccessfullyChangedEmail(email: String, ipAddress: String?, deviceName: String?): AppResult<Unit>

    fun fakeSendEmail(): AppResult<Unit>
}