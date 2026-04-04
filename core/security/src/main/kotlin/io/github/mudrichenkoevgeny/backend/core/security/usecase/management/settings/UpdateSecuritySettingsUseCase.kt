package io.github.mudrichenkoevgeny.backend.core.security.usecase.management.settings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.SecuritySettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateSecuritySettingsUseCase @Inject constructor(
    private val securitySettingsProvider: SecuritySettingsProvider
) {
    suspend fun execute(securitySettings: SecuritySettings): AppResult<Unit> {
        return securitySettingsProvider.updateSecuritySettings(securitySettings)
    }
}