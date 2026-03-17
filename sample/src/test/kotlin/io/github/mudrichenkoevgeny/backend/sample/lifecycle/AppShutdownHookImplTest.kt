package io.github.mudrichenkoevgeny.backend.sample.lifecycle

import io.github.mudrichenkoevgeny.backend.core.audit.service.AuditService
import io.github.mudrichenkoevgeny.backend.core.common.result.AppSystemResult
import io.github.mudrichenkoevgeny.backend.core.database.manager.database.DatabaseManager
import io.github.mudrichenkoevgeny.backend.core.database.manager.redis.RedisManager
import io.ktor.server.engine.EmbeddedServer
import io.mockk.coVerify
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class AppShutdownHookImplTest {

    @Test
    fun `register adds shutdown hook that stops server waits audit and shuts down resources`() = runBlocking {
        val registrar = RecordingRegistrar()

        val server = mockk<EmbeddedServer<*, *>>()
        val databaseManager = mockk<DatabaseManager>()
        val redisManager = mockk<RedisManager>()
        val auditService = mockk<AuditService>()

        every { server.stop(any(), any()) } just runs
        every { databaseManager.shutdown() } just runs
        every { redisManager.shutdown() } returns AppSystemResult.Success(Unit)
        coEvery { auditService.awaitAll() } just runs

        val hook = AppShutdownHookImpl(
            databaseManager = databaseManager,
            redisManager = redisManager,
            auditService = auditService,
            shutdownHookRegistrar = registrar
        )

        hook.register(server)

        val registeredThread = registrar.lastHook
        assertNotNull(registeredThread)

        registeredThread!!.start()
        registeredThread.join(2_000)

        verify(exactly = 1) { server.stop(any(), any()) }
        verify(exactly = 1) { databaseManager.shutdown() }
        verify(exactly = 1) { redisManager.shutdown() }
        coVerify(exactly = 1) { auditService.awaitAll() }
    }

    private class RecordingRegistrar : ShutdownHookRegistrar {
        var lastHook: Thread? = null

        override fun addShutdownHook(hook: Thread) {
            lastHook = hook
        }
    }
}

