package io.github.mudrichenkoevgeny.backend.core.common.application.httpconfiguration

import io.github.mudrichenkoevgeny.backend.core.common.config.enums.AppEnvironment
import io.github.mudrichenkoevgeny.backend.core.common.constants.CommonNetworkConstants
import io.github.mudrichenkoevgeny.shared.foundation.core.common.constants.CommonNetworkFoundationConstants
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
        allowHeader(CommonNetworkFoundationConstants.TRACE_HEADER_NAME)
        exposeHeader(CommonNetworkFoundationConstants.TRACE_HEADER_NAME)

        allowCredentials = true

        maxAgeInSeconds = CommonNetworkConstants.CORS_MAX_AGE_SECONDS
    }

    install(DefaultHeaders) {
        header(HttpHeaders.Server, CommonNetworkConstants.SERVER_MASK_HEADER_VALUE)
        header(CommonNetworkConstants.X_CONTENT_TYPE_OPTIONS_HEADER_NAME, CommonNetworkConstants.NOSNIFF_HEADER_VALUE)
        header(CommonNetworkConstants.X_FRAME_OPTIONS_HEADER_NAME, CommonNetworkConstants.DENY_HEADER_VALUE)
        header(CommonNetworkConstants.X_XSS_PROTECTION_HEADER_NAME, CommonNetworkConstants.XSS_BLOCK_HEADER_VALUE)
        header(CommonNetworkConstants.CONTENT_SECURITY_POLICY_HEADER_NAME, CommonNetworkConstants.CSP_API_HEADER_VALUE)
        header(CommonNetworkConstants.REFERRER_POLICY_HEADER_NAME, CommonNetworkConstants.NO_REFERRER_HEADER_VALUE)
        header(CommonNetworkConstants.PERMISSION_POLICY_HEADER_NAME, CommonNetworkConstants.PERMISSION_POLICY_HEADER_VALUE)
        if (environment != AppEnvironment.DEV) {
            header(CommonNetworkConstants.STRICT_TRANSPORT_SECURITY_HEADER_NAME, CommonNetworkConstants.HSTS_ONE_YEAR_HEADER_VALUE)
        }
    }
}