package com.mudrichenkoevgeny.backend.core.database.redisclient

import com.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import com.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LettuceRedisClientCreator @Inject constructor(
    private val appLogger: AppLogger
): RedisClientCreator {

    override fun create(url: String, timeoutSeconds: Long): RedisClient {
        return try {
            val uri = RedisURI.create(url).apply {
                timeout = Duration.ofSeconds(timeoutSeconds)
            }

            RedisClient.create(uri)
        } catch (t: Throwable) {
            appLogger.logError(CommonError.System(t))
            throw t
        }
    }
}