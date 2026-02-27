package io.github.mudrichenkoevgeny.backend.sample.lifecycle

import io.github.mudrichenkoevgeny.backend.core.audit.service.AuditService
import io.github.mudrichenkoevgeny.backend.core.database.manager.database.DatabaseManager
import io.github.mudrichenkoevgeny.backend.core.database.manager.redis.RedisManager
import io.ktor.server.engine.EmbeddedServer
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppShutdownHookImpl @Inject constructor(
    private val databaseManager: DatabaseManager,
    private val redisManager: RedisManager,
    private val auditService: AuditService
) : AppShutdownHook {

    override fun register(server: EmbeddedServer<*, *>) {
        Runtime.getRuntime().addShutdownHook(Thread {
            runBlocking {
                try {
                    server.stop(GRACE_PERIOD_MS, TIMEOUT_MS)

                    withTimeoutOrNull(TIMEOUT_MS) {
                        auditService.awaitAll()
                    }

                    databaseManager.shutdown()
                    redisManager.shutdown()
                } catch (e: Exception) {
                    println("Error during shutdown: ${e.message}")
                    e.printStackTrace()
                }
            }
        })
    }

    companion object {
        private const val GRACE_PERIOD_MS = 3000L
        private const val TIMEOUT_MS = 5000L
    }
}