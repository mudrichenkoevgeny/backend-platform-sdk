package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.configuration

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProvider
import io.github.mudrichenkoevgeny.backend.core.storage.settings.provider.StorageSettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.configuration.OpenUserConfiguration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetOpenUserConfigurationUseCase @Inject constructor(
    private val globalSettingsProvider: GlobalSettingsProvider,
    private val securitySettingsProvider: SecuritySettingsProvider,
    private val storageSettingsProvider: StorageSettingsProvider,
    private val authSettingsProvider: AuthSettingsProvider
) {
    /**
     * Fetches the combined user-feature configuration for public clients.
     *
     * **Allowed Account Statuses:** Any (Public access).
     *
     * **Workflow:**
     * 1. Aggregates general platform settings from [GlobalSettingsProvider].
     * 2. Collects security-related constraints from [SecuritySettingsProvider].
     * 3. Collects storage configuration limits from [StorageSettingsProvider].
     * 4. Retrieves available authentication methods and provider settings from [AuthSettingsProvider].
     *
     * @return [AppResult.Success] containing the aggregated [OpenUserConfiguration].
     */
    operator fun invoke(): AppResult<OpenUserConfiguration> {
        return AppResult.Success(
            OpenUserConfiguration(
                openGlobalSettings = globalSettingsProvider.getOpenGlobalSettings(),
                openSecuritySettings = securitySettingsProvider.getOpenSecuritySettings(),
                openStorageSettings = storageSettingsProvider.getOpenStorageSettings(),
                openAuthSettings = authSettingsProvider.getOpenAuthSettings()
            )
        )
    }
}