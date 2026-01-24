package com.mudrichenkoevgeny.backend.feature.user.service.email

import com.mudrichenkoevgeny.backend.core.common.result.AppResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmailServiceImpl @Inject constructor() : EmailService {
    override fun sendVerificationCode(email: String, code: String): AppResult<Unit> {
        println("EmailService: sendVerificationCode | $email | $code")
        return AppResult.Success(Unit)
        // todo
    }

    override fun sendResetPasswordVerificationCode(email: String, code: String): AppResult<Unit> {
        println("EmailService: sendResetPasswordVerificationCode | $email | $code")
        return AppResult.Success(Unit)
        // todo
    }

    override fun sendAlreadyRegisteredEmail(email: String, ipAddress: String?, deviceName: String?): AppResult<Unit> {
        println("EmailService: sendAlreadyRegisteredEmail | $email")
        return AppResult.Success(Unit)
        // todo
    }

    override fun sendSuccessfulRegistrationEmail(email: String): AppResult<Unit> {
        println("EmailService: sendSuccessfulRegistrationEmail | $email")
        return AppResult.Success(Unit)
        // todo
    }

    override fun sendSuccessfulLoginEmail(email: String, ipAddress: String?, deviceName: String?): AppResult<Unit> {
        println("EmailService: sendSuccessfulLoginEmail | $email")
        return AppResult.Success(Unit)
        // todo
    }

    override fun sendPasswordSuccessfullyChangedEmail(
        email: String,
        ipAddress: String?,
        deviceName: String?
    ): AppResult<Unit> {
        println("EmailService: sendPasswordSuccessfullyChangedEmail | $email")
        return AppResult.Success(Unit)
        // todo
    }

    override fun fakeSendEmail(): AppResult<Unit> {
        println("EmailService: fakeSendEmail")
        // todo
        return AppResult.Success(Unit)
    }
}