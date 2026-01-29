package io.github.mudrichenkoevgeny.backend.core.database.factory.redis

import io.lettuce.core.RedisClient

interface RedisFactory {
    fun create(): RedisClient
}