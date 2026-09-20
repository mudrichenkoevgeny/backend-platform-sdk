package io.github.mudrichenkoevgeny.backend.core.settings.domain.model.globalsettings

import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientType
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.ManagementGlobalSettings
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.OpenGlobalSettings

fun createTestManagementGlobalSettings(
    privacyPolicyUrl: String? = "https://example.com/privacy",
    termsOfServiceUrl: String? = "https://example.com/terms",
    contactSupportEmail: String? = "support@example.com",
    maintenanceUntilEpochMillis: Long? = null,
    minSupportedAppVersions: Map<ClientType, String> = emptyMap(),
    isTracingEnabled: Boolean = false,
    isMetricsEnabled: Boolean = false,
    isVerboseLoggingEnabled: Boolean = false
) = ManagementGlobalSettings(
    privacyPolicyUrl = privacyPolicyUrl,
    termsOfServiceUrl = termsOfServiceUrl,
    contactSupportEmail = contactSupportEmail,
    maintenanceUntilEpochMillis = maintenanceUntilEpochMillis,
    minSupportedAppVersions = minSupportedAppVersions,
    isTracingEnabled = isTracingEnabled,
    isMetricsEnabled = isMetricsEnabled,
    isVerboseLoggingEnabled = isVerboseLoggingEnabled
)

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
