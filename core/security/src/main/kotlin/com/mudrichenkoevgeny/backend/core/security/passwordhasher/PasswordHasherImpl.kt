package com.mudrichenkoevgeny.backend.core.security.passwordhasher

import com.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import com.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import com.mudrichenkoevgeny.backend.core.common.result.AppResult
import com.password4j.Password
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasswordHasherImpl @Inject constructor(
    private val appLogger: AppLogger
): PasswordHasher {
    override fun hash(password: String): AppResult<String> {
        return try {
            val passwordHash = Password.hash(password)
                .addRandomSalt()
                .withArgon2()
                .result
            AppResult.Success(passwordHash)
        } catch (t: Throwable) {
            appLogger.logError(CommonError.System(t))
            throw t
        }
    }

    override fun verify(password: String, storedHash: String): AppResult<Boolean> {
        return try {
            val checkResult = Password.check(password, storedHash).withArgon2()
            AppResult.Success(checkResult)
        } catch (t: Throwable) {
            appLogger.logError(CommonError.System(t))
            throw t
        }
    }

    override fun isPasswordValid(password: String?, hash: String?): AppResult<Boolean> {
        if (password.isNullOrEmpty() || hash.isNullOrEmpty()) {
            return AppResult.Success(false)
        }

        return verify(password, hash)
    }

    override fun isPasswordValidFakeCheck(password: String?): AppResult<Unit> {
        isPasswordValid(password, PASSWORD_FAKE_HASH)
        return AppResult.Success(Unit)
    }

    companion object {
        const val PASSWORD_FAKE_HASH = "$2a$10N9qo8uLOickgx2ZMRZoMyeIjZAgNIvB.q.2G9S7vV.YtUuVjR5KTu" // todo generate in tests
    }
}