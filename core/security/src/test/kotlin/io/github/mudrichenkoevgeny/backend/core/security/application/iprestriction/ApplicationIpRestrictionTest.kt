package io.github.mudrichenkoevgeny.backend.core.security.application.iprestriction

import io.github.mudrichenkoevgeny.backend.core.common.application.statuspages.configureStatusPages
import io.github.mudrichenkoevgeny.backend.core.common.config.common.model.createTestCommonConfig
import io.github.mudrichenkoevgeny.backend.core.common.config.model.AppInstanceMode
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppErrorParserConfig
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.CommonErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.RecordingSecuritySettingsProvider
import io.github.mudrichenkoevgeny.backend.core.security.validator.iprestriction.IpRestrictionPolicyValidator
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.iprestriction.IpRestrictionPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.error.naming.SecurityErrorCodes
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ApplicationIpRestrictionTest {

    private val commonConfig = createTestCommonConfig(
        ktorServerPort = 8080,
        ktorManagementPort = 9090
    )
    private val securitySettingsProvider = RecordingSecuritySettingsProvider()
    private val validator = IpRestrictionPolicyValidator()
    private val mockLogger = mockk<AppLogger>(relaxed = true)
    private val errorParser = CommonErrorParser(AppErrorParserConfig())

    @Test
    fun `allows request when ip restriction is disabled`() = testApplication {
        securitySettingsProvider.openIpRestrictionPolicy = IpRestrictionPolicy(
            isBlacklistEnabled = false,
            blacklist = emptyList(),
            isWhitelistEnabled = false,
            whitelist = emptyList()
        )

        application {
            configureStatusPages(errorParser, mockLogger)
            configureIpRestriction(commonConfig, securitySettingsProvider, validator)
            routing {
                get("/test") {
                    call.respondText("ok")
                }
            }
        }

        val response = client.get("/test")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("ok", response.bodyAsText())
    }

    @Test
    fun `rejects request with 403 Forbidden when ip is blacklisted on open contour`() = testApplication {
        securitySettingsProvider.openIpRestrictionPolicy = IpRestrictionPolicy(
            isBlacklistEnabled = true,
            blacklist = listOf("127.0.0.1", "localhost"),
            isWhitelistEnabled = false,
            whitelist = emptyList()
        )

        application {
            configureStatusPages(errorParser, mockLogger)
            configureIpRestriction(commonConfig, securitySettingsProvider, validator)
            routing {
                get("/test") {
                    call.respondText("ok")
                }
            }
        }

        val response = client.get("/test")
        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue(response.bodyAsText().contains(SecurityErrorCodes.IP_NOT_ALLOWED))
    }

    @Test
    fun `uses management ip policy when path starts with management`() = testApplication {
        securitySettingsProvider.openIpRestrictionPolicy = IpRestrictionPolicy(
            isBlacklistEnabled = false,
            blacklist = emptyList(),
            isWhitelistEnabled = false,
            whitelist = emptyList()
        )
        securitySettingsProvider.managementIpRestrictionPolicy = IpRestrictionPolicy(
            isBlacklistEnabled = true,
            blacklist = listOf("127.0.0.1"),
            isWhitelistEnabled = false,
            whitelist = emptyList()
        )

        application {
            configureStatusPages(errorParser, mockLogger)
            configureIpRestriction(commonConfig, securitySettingsProvider, validator)
            routing {
                get("/management/test") {
                    call.respondText("ok")
                }
            }
        }

        val response = client.get("/management/test")
        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue(response.bodyAsText().contains(SecurityErrorCodes.IP_NOT_ALLOWED))
    }

    @Test
    fun `uses management ip policy when instance mode is MANAGEMENT`() = testApplication {
        val managementConfig = createTestCommonConfig(
            instanceMode = AppInstanceMode.MANAGEMENT,
            ktorServerPort = 8080,
            ktorManagementPort = 9090
        )
        securitySettingsProvider.openIpRestrictionPolicy = IpRestrictionPolicy(
            isBlacklistEnabled = false,
            blacklist = emptyList(),
            isWhitelistEnabled = false,
            whitelist = emptyList()
        )
        securitySettingsProvider.managementIpRestrictionPolicy = IpRestrictionPolicy(
            isBlacklistEnabled = true,
            blacklist = listOf("127.0.0.1"),
            isWhitelistEnabled = false,
            whitelist = emptyList()
        )

        application {
            configureStatusPages(errorParser, mockLogger)
            configureIpRestriction(managementConfig, securitySettingsProvider, validator)
            routing {
                get("/custom/path") {
                    call.respondText("ok")
                }
            }
        }

        val response = client.get("/custom/path")
        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue(response.bodyAsText().contains(SecurityErrorCodes.IP_NOT_ALLOWED))
    }
}
