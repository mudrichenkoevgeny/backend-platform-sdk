package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.configuration

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.configuration.UserConfiguration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetUserConfigurationUseCase @Inject constructor(
    private val globalSettingsProvider: GlobalSettingsProvider,
    private val securitySettingsProvider: SecuritySettingsProvider,
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
     * 3. Retrieves available authentication methods and provider settings from [AuthSettingsProvider].
     *
     * @return [AppResult.Success] containing the aggregated [UserConfiguration].
     */
    operator fun invoke(): AppResult<UserConfiguration> {
        return AppResult.Success(
            UserConfiguration(
                globalSettings = globalSettingsProvider.getSettings(),
                securitySettings = securitySettingsProvider.getSettings(),
                authSettings = authSettingsProvider.getPublicAuthSettings()
            )
        )
    }
}
