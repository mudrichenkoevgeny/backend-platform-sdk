package io.github.mudrichenkoevgeny.backend.core.database.redisclient

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
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