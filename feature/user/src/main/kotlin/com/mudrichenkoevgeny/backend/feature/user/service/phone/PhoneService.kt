package io.github.mudrichenkoevgeny.backend.feature.user.service.phone

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult

interface PhoneService {
    fun sendVerificationCode(phoneNumber: String, code: String): AppResult<Unit>
    fun sendAlreadyRegisteredPhoneNumber(phoneNumber: String, ipAddress: String?, deviceName: String?): AppResult<Unit>
}