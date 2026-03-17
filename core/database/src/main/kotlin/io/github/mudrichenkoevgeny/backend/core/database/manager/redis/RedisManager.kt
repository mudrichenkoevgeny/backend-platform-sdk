package io.github.mudrichenkoevgeny.backend.core.database.manager.redis

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.AppSystemResult

/**
 * Redis operations and lifecycle: key-value commands with optional TTL, availability check, warmup, and shutdown.
 * All suspend functions use the same connection; errors are wrapped in [AppResult] or [AppSystemResult].
 */
interface RedisManager {

    suspend fun setWithExpiration(key: String, value: String, expirationSeconds: Long): AppResult<Unit>
    suspend fun incrementWithExpiration(key: String, expirationSeconds: Long): AppResult<Long>
    suspend fun get(key: String): AppResult<String?>
    suspend fun getTtl(key: String): AppResult<Long>
    suspend fun exists(key: String): AppResult<Boolean>
    suspend fun delete(key: String): AppResult<Unit>

    suspend fun isAvailable(): AppSystemResult<Boolean>
    suspend fun warmup(): AppSystemResult<Unit>
    fun shutdown(): AppSystemResult<Unit>
}