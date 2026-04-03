package io.github.mudrichenkoevgeny.backend.core.common.network.request.model

import io.github.mudrichenkoevgeny.backend.core.common.model.UserDeviceId
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.clienttype.ClientType
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.contract.CommonHttpHeaders
import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.host
import io.ktor.server.request.userAgent

/**
 * Structured information about the calling client inferred from HTTP headers and connection data.
 *
 * Used for logging, analytics and feature flags without leaking raw header access across the codebase.
 */
data class ClientInfo(
    val clientType: ClientType?,
    val userAgent: String?,
    val ipAddress: String?,
    val language: String?,
    val host: String?,
    val origin: String?,
    val deviceId: UserDeviceId?,
    val deviceName: String?,
    val appVersion: String?,
    val operationSystemVersion: String?,
    val apiVersion: String? = null
)

/**
 * Extracts a [ClientInfo] from this [ApplicationCall] using standard and shared headers.
 */
fun ApplicationCall.extractClientInfo(): ClientInfo {
    return ClientInfo(
        clientType = request.headers[CommonHttpHeaders.CLIENT_TYPE_HEADER_NAME]?.let { ClientType.fromValueOrNull(it) },
        userAgent = request.userAgent(),
        ipAddress = request.local.remoteHost,
        language = request.headers[HttpHeaders.AcceptLanguage],
        host = request.host(),
        origin = request.headers[HttpHeaders.Origin],
        deviceId = request.headers[CommonHttpHeaders.DEVICE_ID_HEADER_NAME]?.let { deviceId ->
            UserDeviceId(deviceId)
        },
        deviceName = request.headers[CommonHttpHeaders.DEVICE_NAME_HEADER_NAME],
        appVersion = request.headers[CommonHttpHeaders.APP_VERSION_HEADER_NAME],
        operationSystemVersion = request.headers[CommonHttpHeaders.OPERATION_SYSTEM_VERSION_HEADER_NAME],
        apiVersion = request.headers[CommonHttpHeaders.API_VERSION_HEADER_NAME]
    )
}