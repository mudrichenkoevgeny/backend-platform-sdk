package io.github.mudrichenkoevgeny.backend.core.security.usecase.system.settings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds security-related defaults into the system settings storage.
 *
 * Typically executed during application bootstrap.
 */
@Singleton
class SeedSecuritySettingsUseCase @Inject constructor(
    private val securitySettingsProvider: SecuritySettingsProvider
) {
    /**
     * Registers defaults for missing security settings.
     */
    suspend fun execute(): AppResult<Unit> = securitySettingsProvider.initialize()
}