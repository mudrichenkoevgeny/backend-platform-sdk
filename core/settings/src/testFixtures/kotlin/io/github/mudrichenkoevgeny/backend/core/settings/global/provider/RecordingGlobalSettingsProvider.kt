package io.github.mudrichenkoevgeny.backend.core.settings.global.provider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientType
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.ManagementGlobalSettings
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.OpenGlobalSettings

class RecordingGlobalSettingsProvider : GlobalSettingsProvider {
    var initializeCalled: Boolean = false

    override suspend fun initialize(): AppResult<Unit> {
        initializeCalled = true
        return AppResult.Success(Unit)
    }

    override fun getOpenGlobalSettings(): OpenGlobalSettings {
        error("Not used")
    }

    override fun getManagementGlobalSettings(): ManagementGlobalSettings {
        error("Not used")
    }

    override fun getPrivacyPolicyUrl(): String? = null
    override fun getTermsOfServiceUrl(): String? = null
    override fun getContactSupportEmail(): String? = null
    override fun getMinSupportedAppVersions(): Map<ClientType, String> = emptyMap()
    override fun getIsTracingEnabled(): Boolean = false
    override fun getIsMetricsEnabled(): Boolean = false
    override fun getIsVerboseLoggingEnabled(): Boolean = false

    override suspend fun updateManagementGlobalSettings(managementGlobalSettings: ManagementGlobalSettings): AppResult<Unit> =
        AppResult.Success(Unit)
}
