package io.github.mudrichenkoevgeny.backend.core.settings.config.model

import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientType
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.OpenGlobalSettings

fun createTestOpenGlobalSettings(
    privacyPolicyUrl: String? = "https://example.com/privacy",
    termsOfServiceUrl: String? = "https://example.com/terms",
    contactSupportEmail: String? = "support@example.com",
    maintenanceUntilEpochMillis: Long? = null,
    minSupportedAppVersions: Map<ClientType, String> = emptyMap()
) = OpenGlobalSettings(
    privacyPolicyUrl = privacyPolicyUrl,
    termsOfServiceUrl = termsOfServiceUrl,
    contactSupportEmail = contactSupportEmail,
    maintenanceUntilEpochMillis = maintenanceUntilEpochMillis,
    minSupportedAppVersions = minSupportedAppVersions
)
