package io.github.mudrichenkoevgeny.backend.core.security.passwordhasher

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult

interface PasswordHasher {
    fun hash(password: String): AppResult<String>
    fun verify(password: String, storedHash: String): AppResult<Boolean>
    fun isPasswordValid(password: String?, hash: String?): AppResult<Boolean>
    fun isPasswordValidFakeCheck(password: String?): AppResult<Unit>
}