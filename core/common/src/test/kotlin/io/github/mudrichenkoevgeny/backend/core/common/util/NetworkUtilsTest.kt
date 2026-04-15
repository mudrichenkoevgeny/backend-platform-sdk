package io.github.mudrichenkoevgeny.backend.core.common.util

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.HttpClient
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class NetworkUtilsTest {

    @Test
    fun `bodySafe returns deserialized body on success`() = runBlocking {
        val engine = MockEngine { _ ->
            respond(
                content = "ok",
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "text/plain")
            )
        }
        val client = HttpClient(engine)

        val response: HttpResponse = client.get("http://test")

        val body: String? = response.bodySafe()
        assertEquals("ok", body)
    }

    @Test
    fun `bodySafe returns null on failure`() = runBlocking {
        val engine = MockEngine { _ ->
            respond(
                content = "not json",
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json")
            )
        }
        val client = HttpClient(engine)

        data class Payload(val value: Int)

        val response: HttpResponse = client.get("http://test")

        val body: Payload? = response.bodySafe()
        assertNull(body)
    }
}

