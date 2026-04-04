package io.github.mudrichenkoevgeny.backend.core.security.usecase.open.settings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.SecuritySettings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads effective [io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.SecuritySettings] for API/UI consumption.
 */
@Singleton
class GetSecuritySettingsUseCase @Inject constructor(
    private val securitySettingsProvider: SecuritySettingsProvider
) {
    /**
     * Returns effective security settings snapshot.
     */
    fun execute(): AppResult<SecuritySettings> {
        return securitySettingsProvider.getSettings()
    }
}