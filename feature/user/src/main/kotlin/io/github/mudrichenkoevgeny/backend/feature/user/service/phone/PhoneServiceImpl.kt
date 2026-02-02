package io.github.mudrichenkoevgeny.backend.feature.user.service.phone

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneServiceImpl @Inject constructor() : PhoneService {
    override fun sendVerificationCode(phoneNumber: String, code: String): AppResult<Unit> {
        println("PhoneService: sendVerificationCode | $phoneNumber | $code")
        return AppResult.Success(Unit)
        // todo
    }

    override fun sendAlreadyRegisteredPhoneNumber(
        phoneNumber: String,
        ipAddress: String?,
        deviceName: String?
    ): AppResult<Unit> {
        println("PhoneService: sendAlreadyRegisteredPhoneNumber | $phoneNumber")
        return AppResult.Success(Unit)
        // todo
    }
}