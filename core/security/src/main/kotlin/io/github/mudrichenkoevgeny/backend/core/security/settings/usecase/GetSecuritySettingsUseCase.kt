package io.github.mudrichenkoevgeny.backend.core.security.settings.usecase

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.settings.model.SecuritySettings
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetSecuritySettingsUseCase @Inject constructor(
    private val securitySettingsProvider: SecuritySettingsProvider
) {
    fun execute(): AppResult<SecuritySettings> {
        return securitySettingsProvider.getSettings()
    }
}