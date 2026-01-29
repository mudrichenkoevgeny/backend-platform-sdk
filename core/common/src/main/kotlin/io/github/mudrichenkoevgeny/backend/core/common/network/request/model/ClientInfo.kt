package io.github.mudrichenkoevgeny.backend.core.common.network.request.model

import io.github.mudrichenkoevgeny.backend.core.common.constants.NetworkConstants
import io.github.mudrichenkoevgeny.backend.core.common.model.UserDeviceId
import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.origin
import io.ktor.server.request.host
import io.ktor.server.request.userAgent

data class ClientInfo(
    val clientType: String?,
    val userAgent: String?,
    val ipAddress: String?,
    val language: String?,
    val host: String?,
    val origin: String?,
    val deviceId: UserDeviceId?,
    val deviceName: String?
)

fun ApplicationCall.extractClientInfo(): ClientInfo {
    return ClientInfo(
        clientType = request.headers[NetworkConstants.CLIENT_TYPE_HEADER_NAME],
        userAgent = request.userAgent(),
        ipAddress = request.origin.remoteAddress,
        language = request.headers[HttpHeaders.AcceptLanguage],
        host = request.host(),
        origin = request.headers[HttpHeaders.Origin],
        deviceId = request.headers[NetworkConstants.DEVICE_ID_HEADER_NAME]?.let { deviceId ->
            UserDeviceId(deviceId)
        },
        deviceName = request.headers[NetworkConstants.DEVICE_NAME_HEADER_NAME]
    )
}