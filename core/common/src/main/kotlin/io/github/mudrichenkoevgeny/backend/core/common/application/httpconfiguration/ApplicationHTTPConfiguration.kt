package io.github.mudrichenkoevgeny.backend.core.common.application.httpconfiguration

import io.github.mudrichenkoevgeny.backend.core.common.config.model.AppEnvironment
import io.github.mudrichenkoevgeny.backend.core.common.network.contract.CommonNetworkHttpHeaderValues
import io.github.mudrichenkoevgeny.backend.core.common.network.contract.CommonNetworkHttpHeaders
import io.github.mudrichenkoevgeny.backend.core.common.network.cors.CorsConfig
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders

fun Application.configureHTTP(
    environment: AppEnvironment,
    allowedOrigins: List<String>
) {
    install(CORS) {
        if (environment == AppEnvironment.DEV) {
            anyHost()
        } else {
            allowedOrigins.forEach { origin ->
                val uri = Url(origin)
                allowHost(uri.host, schemes = listOf(uri.protocol.name))
            }
        }

        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)

        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Accept)
        allowHeader(HttpHeaders.AcceptLanguage)
        allowHeadersPrefixed(CommonNetworkHttpHeaders.X_PREFIX)

        allowCredentials = true
        allowNonSimpleContentTypes = true
        maxAgeInSeconds = CorsConfig.CORS_MAX_AGE_SECONDS
    }

    install(DefaultHeaders) {
        header(HttpHeaders.Server, CommonNetworkHttpHeaderValues.SERVER_MASK_HEADER_VALUE)
        header(CommonNetworkHttpHeaders.X_CONTENT_TYPE_OPTIONS_HEADER_NAME, CommonNetworkHttpHeaderValues.NOSNIFF_HEADER_VALUE)
        header(CommonNetworkHttpHeaders.X_FRAME_OPTIONS_HEADER_NAME, CommonNetworkHttpHeaderValues.DENY_HEADER_VALUE)
        header(CommonNetworkHttpHeaders.X_XSS_PROTECTION_HEADER_NAME, CommonNetworkHttpHeaderValues.XSS_BLOCK_HEADER_VALUE)
        val csp = if (environment == AppEnvironment.DEV) {
            CommonNetworkHttpHeaderValues.CSP_DEV_HEADER_VALUE
        } else {
            CommonNetworkHttpHeaderValues.CSP_API_HEADER_VALUE
        }
        header(CommonNetworkHttpHeaders.CONTENT_SECURITY_POLICY_HEADER_NAME, csp)
        header(CommonNetworkHttpHeaders.REFERRER_POLICY_HEADER_NAME, CommonNetworkHttpHeaderValues.NO_REFERRER_HEADER_VALUE)
        header(CommonNetworkHttpHeaders.PERMISSION_POLICY_HEADER_NAME, CommonNetworkHttpHeaderValues.PERMISSION_POLICY_HEADER_VALUE)
        if (environment != AppEnvironment.DEV) {
            header(CommonNetworkHttpHeaders.STRICT_TRANSPORT_SECURITY_HEADER_NAME, CommonNetworkHttpHeaderValues.HSTS_ONE_YEAR_HEADER_VALUE)
        }
    }
}