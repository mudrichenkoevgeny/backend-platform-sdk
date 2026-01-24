package com.mudrichenkoevgeny.backend.core.database.manager.redis

import com.mudrichenkoevgeny.backend.core.common.result.AppResult
import com.mudrichenkoevgeny.backend.core.common.result.AppSystemResult

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