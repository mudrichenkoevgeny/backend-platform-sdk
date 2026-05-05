package io.github.mudrichenkoevgeny.backend.feature.user.service.phone

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * // todo not implemented
 * Placeholder [PhoneService] implementation.
 *
 * Currently prints to stdout and returns success. A host app is expected to replace this
 * with a real SMS provider integration.
 */
@Singleton
class PhoneServiceImpl @Inject constructor() : PhoneService {

    override suspend fun sendVerificationCode(phoneNumber: String, code: String, language: String?): AppResult<Unit> {
        return AppResult.Error(CommonError.ServiceUnavailable())
    }

    override suspend fun sendAlreadyRegisteredPhoneNumber(
        phoneNumber: String,
        ipAddress: String?,
        deviceName: String?,
        language: String?
    ): AppResult<Unit> {
        return AppResult.Error(CommonError.ServiceUnavailable())
    }
}