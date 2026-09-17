package io.github.mudrichenkoevgeny.backend.core.common.route

import io.github.mudrichenkoevgeny.backend.core.common.config.common.model.CommonConfig
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.healthcheck.HealthCheckerManager
import io.github.mudrichenkoevgeny.backend.core.common.result.AppSystemResult
import io.github.mudrichenkoevgeny.backend.core.common.server.KtorServer
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.routing.routing

class ManagementHealthRouterTest {

    companion object {
        private lateinit var server: EmbeddedServer<*, *>
        private val client = HttpClient(CIO)
        private val commonConfig = mockk<CommonConfig> {
            coEvery { ktorServerHost } returns "127.0.0.1"
            coEvery { ktorServerPort } returns 8080
            coEvery { ktorManagementPort } returns 8081
            coEvery { ktorShutdownGracePeriodMs } returns 3000
            coEvery { ktorShutdownTimeoutMs } returns 10000
        }
        val healthCheckerManager = mockk<HealthCheckerManager>()

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

    @Test
    fun `live endpoint returns OK`() = runBlocking {
        val path = "${ManagementHealthRouter.BASE_PATH}${ManagementHealthRouter.LIVE_PATH}"
        val response = client.get("http://127.0.0.1:8081$path")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `ready endpoint returns OK when health checks pass`() = runBlocking {
        coEvery { healthCheckerManager.checkCriticalHealth() } returns AppSystemResult.Success(Unit)
        val path = "${ManagementHealthRouter.BASE_PATH}${ManagementHealthRouter.READY_PATH}"
        val response = client.get("http://127.0.0.1:8081$path")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `ready endpoint returns 503 when health checks fail`() = runBlocking {
        coEvery { healthCheckerManager.checkCriticalHealth() } returns AppSystemResult.Error(CommonError.Internal(RuntimeException("Fail")))
        val path = "${ManagementHealthRouter.BASE_PATH}${ManagementHealthRouter.READY_PATH}"
        val response = client.get("http://127.0.0.1:8081$path")
        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
    }
}