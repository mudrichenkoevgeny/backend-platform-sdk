package io.github.mudrichenkoevgeny.backend.core.common.route

import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import io.github.mudrichenkoevgeny.backend.core.common.config.common.model.createTestCommonConfig
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.healthcheck.HealthCheck
import io.github.mudrichenkoevgeny.backend.core.common.healthcheck.HealthCheckSeverity
import io.github.mudrichenkoevgeny.backend.core.common.healthcheck.HealthCheckerManager
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppSystemResult
import io.github.mudrichenkoevgeny.backend.core.common.server.KtorServer
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class ManagementHealthRouterTest {

    companion object {
        private lateinit var server: EmbeddedServer<*, *>
        private val client = HttpClient(CIO)
        private val commonConfig = createTestCommonConfig(
            ktorServerHost = "127.0.0.1",
            ktorServerPort = 8080,
            ktorManagementPort = 8081
        )
        val mockHealthCheck = mockk<HealthCheck>()
        val mockLogger = mockk<AppLogger>(relaxed = true)

        val healthCheckerManager = HealthCheckerManager(
            healthChecks = setOf(mockHealthCheck),
            appLogger = mockLogger
        )

        @BeforeAll
        @JvmStatic
        fun setUp() {
            val router = ManagementHealthRouter(commonConfig, healthCheckerManager)
            server = KtorServer.create(commonConfig) {
                routing {
                    router.register(this)
                }
            }.start(wait = false)
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            server.stop(1000, 2000)
        }
    }

    @BeforeEach
    fun resetMocks() {
        clearMocks(mockHealthCheck)
        every { mockHealthCheck.severity } returns HealthCheckSeverity.CRITICAL
    }

    @Test
    fun `live endpoint returns OK`() = runBlocking {
        val path = "${ManagementHealthRouter.BASE_PATH}${ManagementHealthRouter.LIVE_PATH}"
        val response = client.get("http://127.0.0.1:8081$path")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `ready endpoint returns OK when health checks pass`() = runBlocking {
        coEvery { mockHealthCheck.check() } returns AppSystemResult.Success(Unit)
        val path = "${ManagementHealthRouter.BASE_PATH}${ManagementHealthRouter.READY_PATH}"
        val response = client.get("http://127.0.0.1:8081$path")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `ready endpoint returns 503 when health checks fail`() = runBlocking {
        coEvery { mockHealthCheck.check() } returns AppSystemResult.Error(CommonError.Internal(RuntimeException("Fail")))
        val path = "${ManagementHealthRouter.BASE_PATH}${ManagementHealthRouter.READY_PATH}"
        val response = client.get("http://127.0.0.1:8081$path")
        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
    }
}