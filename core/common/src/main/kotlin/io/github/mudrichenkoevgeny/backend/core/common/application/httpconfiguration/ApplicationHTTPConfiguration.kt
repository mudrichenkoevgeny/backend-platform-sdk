package io.github.mudrichenkoevgeny.backend.core.common.application.httpconfiguration

import io.github.mudrichenkoevgeny.backend.core.common.config.enums.AppEnvironment
import io.github.mudrichenkoevgeny.backend.core.common.constants.NetworkConstants
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders

fun Application.configureHTTP(
    environment: AppEnvironment,
    allowedOrigins: List<String>
) {
    install(CORS) {
        allowedOrigins.forEach { origin ->
            val host = origin.replace(Regex("https?://"), "")
            allowHost(host, schemes = listOf("http", "https"))
        }

        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)

        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(NetworkConstants.TRACE_HEADER_NAME)
        exposeHeader(NetworkConstants.TRACE_HEADER_NAME)

        allowCredentials = true

        maxAgeInSeconds = NetworkConstants.CORS_MAX_AGE_SECONDS
    }

    install(DefaultHeaders) {
        header(HttpHeaders.Server, NetworkConstants.SERVER_MASK_HEADER_VALUE)
        header(NetworkConstants.X_CONTENT_TYPE_OPTIONS_HEADER_NAME, NetworkConstants.NOSNIFF_HEADER_VALUE)
        header(NetworkConstants.X_FRAME_OPTIONS_HEADER_NAME, NetworkConstants.DENY_HEADER_VALUE)
        header(NetworkConstants.X_XSS_PROTECTION_HEADER_NAME, NetworkConstants.XSS_BLOCK_HEADER_VALUE)
        header(NetworkConstants.CONTENT_SECURITY_POLICY_HEADER_NAME, NetworkConstants.CSP_API_HEADER_VALUE)
        header(NetworkConstants.REFERRER_POLICY_HEADER_NAME, NetworkConstants.NO_REFERRER_HEADER_VALUE)
        header(NetworkConstants.PERMISSION_POLICY_HEADER_NAME, NetworkConstants.PERMISSION_POLICY_HEADER_VALUE)
        if (environment != AppEnvironment.DEV) {
            header(NetworkConstants.STRICT_TRANSPORT_SECURITY_HEADER_NAME, NetworkConstants.HSTS_ONE_YEAR_HEADER_VALUE)
        }
    }
}