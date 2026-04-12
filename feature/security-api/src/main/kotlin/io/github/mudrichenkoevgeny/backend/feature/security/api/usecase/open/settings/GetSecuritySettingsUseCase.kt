package io.github.mudrichenkoevgeny.backend.feature.security.api.usecase.open.settings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.SecuritySettings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads effective [SecuritySettings] for API/UI consumption.
 */
@Singleton
class GetSecuritySettingsUseCase @Inject constructor(
    private val securitySettingsProvider: SecuritySettingsProvider
) {
    /**
     * Returns effective security settings snapshot.
     */
    operator fun invoke(): AppResult<SecuritySettings> {
        return securitySettingsProvider.getSettings()
    }
}