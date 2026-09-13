package io.github.mudrichenkoevgeny.backend.feature.user.domain.model.client

import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientDeviceId
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientDeviceInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientType

fun createTestClientInfo(
    deviceInfo: ClientDeviceInfo = ClientDeviceInfo(
        deviceId = ClientDeviceId.generate(),
        deviceName = "Test Device",
        clientType = ClientType.ANDROID,
        language = "en",
        appVersion = "1.0.0",
        operationSystemVersion = "Android 14"
    ),
    userAgent: String? = "TestUserAgent/1.0",
    ipAddress: String? = "127.0.0.1",
    host: String? = "localhost",
    origin: String? = "http://localhost",
    apiVersion: String? = "1.0"
) = ClientInfo(
    deviceInfo = deviceInfo,
    userAgent = userAgent,
    ipAddress = ipAddress,
    host = host,
    origin = origin,
    apiVersion = apiVersion
)
