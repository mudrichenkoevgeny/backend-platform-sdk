package com.mudrichenkoevgeny.backend.feature.user.service.phone

import com.mudrichenkoevgeny.backend.core.common.result.AppResult

interface PhoneService {
    fun sendVerificationCode(phoneNumber: String, code: String): AppResult<Unit>
}