package io.github.mudrichenkoevgeny.backend.core.common.network.request.model

import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientDeviceInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientType
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.toClientDeviceIdOrNull
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.contract.CommonHttpHeaders
import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.host
import io.ktor.server.request.userAgent

/**
 * Extracts a [ClientInfo] from this [ApplicationCall] using standard and shared headers.
 */
fun ApplicationCall.extractClientInfo(): ClientInfo {
    return ClientInfo(
        deviceInfo = ClientDeviceInfo(
            deviceId = request.headers[CommonHttpHeaders.DEVICE_ID_HEADER_NAME]?.toClientDeviceIdOrNull(),
            deviceName = request.headers[CommonHttpHeaders.DEVICE_NAME_HEADER_NAME],
            clientType = request.headers[CommonHttpHeaders.CLIENT_TYPE_HEADER_NAME]?.let { clientType ->
                ClientType.fromValueOrNull(clientType)
            },
            language = request.headers[HttpHeaders.AcceptLanguage],
            appVersion = request.headers[CommonHttpHeaders.APP_VERSION_HEADER_NAME],
            operationSystemVersion = request.headers[CommonHttpHeaders.OPERATION_SYSTEM_VERSION_HEADER_NAME]
        ),
        userAgent = request.userAgent(),
        ipAddress = request.local.remoteHost,
        host = request.host(),
        origin = request.headers[HttpHeaders.Origin],
        apiVersion = request.headers[CommonHttpHeaders.API_VERSION_HEADER_NAME]
    )
}