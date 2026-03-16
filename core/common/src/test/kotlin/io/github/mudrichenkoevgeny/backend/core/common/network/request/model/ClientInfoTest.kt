package io.github.mudrichenkoevgeny.backend.core.common.network.request.model

import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.UserClientType
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.contract.CommonHttpHeaders
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ClientInfoTest {

    @Test
    fun `extractClientInfo maps headers to ClientInfo`() = testApplication {
        application {
            routing {
                get("/client-info") {
                    val info = call.extractClientInfo()
                    val parts = listOf(
                        info.clientType?.name ?: "null",
                        info.userAgent ?: "null",
                        info.ipAddress ?: "null",
                        info.language ?: "null",
                        info.host ?: "null",
                        info.origin ?: "null",
                        info.deviceId?.value ?: "null",
                        info.deviceName ?: "null",
                        info.appVersion ?: "null",
                        info.operationSystemVersion ?: "null",
                    )
                    call.respondText(parts.joinToString("|"))
                }
            }
        }

        val response = client.get("/client-info") {
            header(CommonHttpHeaders.CLIENT_TYPE_HEADER_NAME, UserClientType.WEB.serialName)
            header(HttpHeaders.UserAgent, "Mozilla")
            header(HttpHeaders.AcceptLanguage, "en-US")
            header(HttpHeaders.Origin, "https://example.com")
            header(CommonHttpHeaders.DEVICE_ID_HEADER_NAME, "device-1")
            header(CommonHttpHeaders.DEVICE_NAME_HEADER_NAME, "Pixel")
            header(CommonHttpHeaders.APP_VERSION_HEADER_NAME, "1.2.3")
            header(CommonHttpHeaders.OPERATION_SYSTEM_VERSION_HEADER_NAME, "Android 15")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val parts = response.bodyAsText().split("|")

        assertEquals(UserClientType.WEB.name, parts[0])
        assertEquals("Mozilla", parts[1])
        assertTrue(parts[2].isNotBlank())
        assertEquals("en-US", parts[3])
        assertTrue(parts[4].isNotBlank())
        assertEquals("https://example.com", parts[5])
        assertEquals("device-1", parts[6])
        assertEquals("Pixel", parts[7])
        assertEquals("1.2.3", parts[8])
        assertEquals("Android 15", parts[9])
    }

    @Test
    fun `extractClientInfo returns nulls when headers are missing`() = testApplication {
        application {
            routing {
                get("/client-info-empty") {
                    val info = call.extractClientInfo()
                    val parts = listOf(
                        info.clientType?.name ?: "null",
                        info.userAgent ?: "null",
                        info.language ?: "null",
                        info.origin ?: "null",
                        info.deviceId?.value ?: "null",
                        info.deviceName ?: "null",
                        info.appVersion ?: "null",
                        info.operationSystemVersion ?: "null",
                    )
                    call.respondText(parts.joinToString("|"))
                }
            }
        }

        val response = client.get("/client-info-empty")

        assertEquals(HttpStatusCode.OK, response.status)
        val parts = response.bodyAsText().split("|")

        assertEquals("null", parts[0])
        assertTrue(parts[1].isNotBlank())
        assertEquals("null", parts[2])
        assertEquals("null", parts[3])
        assertEquals("null", parts[4])
        assertEquals("null", parts[5])
        assertEquals("null", parts[6])
        assertEquals("null", parts[7])
    }
}