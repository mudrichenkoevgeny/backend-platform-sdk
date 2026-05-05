package io.github.mudrichenkoevgeny.backend.core.security.passwordhasher

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import com.password4j.Password
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordhash.PasswordHash
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [PasswordHasher] implementation based on Password4j.
 *
 * Uses Argon2 with a random salt for hashing and verification. Any unexpected exception is logged
 * via [AppLogger] and then rethrown (this keeps failures visible to the caller and preserves stack
 * traces).
 */
@Singleton
class PasswordHasherImpl @Inject constructor(
    private val appLogger: AppLogger
): PasswordHasher {
    override fun hash(password: String): AppResult<PasswordHash> {
        return try {
            val passwordHash = Password.hash(password)
                .addRandomSalt()
                .withArgon2()
                .result
            AppResult.Success(PasswordHash(passwordHash))
        } catch (t: Throwable) {
            appLogger.logError(CommonError.Internal(t))
            throw t
        }
    }

    override fun verify(password: String, storedPasswordHash: PasswordHash): AppResult<Boolean> {
        return try {
            val checkResult = Password.check(password, storedPasswordHash.value).withArgon2()
            AppResult.Success(checkResult)
        } catch (t: Throwable) {
            appLogger.logError(CommonError.Internal(t))
            throw t
        }
    }

    override fun isPasswordValid(password: String?, passwordHash: PasswordHash?): AppResult<Boolean> {
        if (password.isNullOrEmpty() || passwordHash?.value.isNullOrEmpty()) {
            return AppResult.Success(false)
        }

        return verify(password, passwordHash)
    }

    override fun isPasswordValidFakeCheck(password: String?): AppResult<Unit> {
        return try {
            Password.check(password ?: "", fakeVerificationHash).withArgon2()
            AppResult.Success(Unit)
        } catch (t: Throwable) {
            appLogger.logError(CommonError.Internal(t))
            throw t
        }
    }

    /**
     * A valid Argon2 hash produced by Password4j. Generated at runtime to avoid coupling to a specific
     * encoded hash format constant and to guarantee compatibility with the verifier.
     */
    private val fakeVerificationHash: String by lazy(LazyThreadSafetyMode.PUBLICATION) {
        Password.hash("fake-password")
            .addRandomSalt()
            .withArgon2()
            .result
    }
}