package io.github.mudrichenkoevgeny.backend.core.common.application.httpconfiguration

import io.github.mudrichenkoevgeny.backend.core.common.config.model.AppEnvironment
import io.github.mudrichenkoevgeny.backend.core.common.network.contract.CommonNetworkHttpHeaderValues
import io.github.mudrichenkoevgeny.backend.core.common.network.contract.CommonNetworkHttpHeaders
import io.github.mudrichenkoevgeny.backend.core.common.network.cors.CorsConfig
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.contract.CommonHttpHeaders
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
        allowHeader(CommonHttpHeaders.TRACE_HEADER_NAME)
        exposeHeader(CommonHttpHeaders.TRACE_HEADER_NAME)

        allowCredentials = true

        maxAgeInSeconds = CorsConfig.CORS_MAX_AGE_SECONDS
    }

    install(DefaultHeaders) {
        header(HttpHeaders.Server, CommonNetworkHttpHeaderValues.SERVER_MASK_HEADER_VALUE)
        header(CommonNetworkHttpHeaders.X_CONTENT_TYPE_OPTIONS_HEADER_NAME, CommonNetworkHttpHeaderValues.NOSNIFF_HEADER_VALUE)
        header(CommonNetworkHttpHeaders.X_FRAME_OPTIONS_HEADER_NAME, CommonNetworkHttpHeaderValues.DENY_HEADER_VALUE)
        header(CommonNetworkHttpHeaders.X_XSS_PROTECTION_HEADER_NAME, CommonNetworkHttpHeaderValues.XSS_BLOCK_HEADER_VALUE)
        header(CommonNetworkHttpHeaders.CONTENT_SECURITY_POLICY_HEADER_NAME, CommonNetworkHttpHeaderValues.CSP_API_HEADER_VALUE)
        header(CommonNetworkHttpHeaders.REFERRER_POLICY_HEADER_NAME, CommonNetworkHttpHeaderValues.NO_REFERRER_HEADER_VALUE)
        header(CommonNetworkHttpHeaders.PERMISSION_POLICY_HEADER_NAME, CommonNetworkHttpHeaderValues.PERMISSION_POLICY_HEADER_VALUE)
        if (environment != AppEnvironment.DEV) {
            header(CommonNetworkHttpHeaders.STRICT_TRANSPORT_SECURITY_HEADER_NAME, CommonNetworkHttpHeaderValues.HSTS_ONE_YEAR_HEADER_VALUE)
        }
    }
}