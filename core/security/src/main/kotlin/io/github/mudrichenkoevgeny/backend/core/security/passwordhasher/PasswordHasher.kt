package io.github.mudrichenkoevgeny.backend.core.security.passwordhasher

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordhash.PasswordHash

/**
 * Hashes and verifies user passwords.
 *
 * The interface is used by auth/user flows to:
 * - hash a raw password before persisting it
 * - verify a password against a stored hash
 * - perform safe checks when password/hash values may be absent
 */
interface PasswordHasher {
    /**
     * Computes a secure hash for the given raw [password].
     *
     * @return [AppResult.Success] with the encoded hash.
     */
    fun hash(password: String): AppResult<PasswordHash>

    /**
     * Verifies that the provided raw [password] matches the [storedPasswordHash].
     *
     * @return [AppResult.Success] with `true` when password matches, otherwise `false`.
     */
    fun verify(password: String, storedPasswordHash: PasswordHash): AppResult<Boolean>

    /**
     * Convenience check when [password] and/or [passwordHash] can be null or blank.
     *
     * @return [AppResult.Success] with `false` if any argument is missing; otherwise delegates to
     * [verify].
     */
    fun isPasswordValid(password: String?, passwordHash: PasswordHash?): AppResult<Boolean>

    /**
     * Performs a "fake" password verification to make timing and CPU cost closer to a real check.
     *
     * This is useful in flows where you want to avoid leaking information (e.g. "user does not
     * exist") based on how fast the password verification finishes.
     *
     * @return [AppResult.Success] when the fake check finishes.
     */
    fun isPasswordValidFakeCheck(password: String?): AppResult<Unit>
}