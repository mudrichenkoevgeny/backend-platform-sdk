package io.github.mudrichenkoevgeny.backend.core.security.settings.usecase

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeedSecuritySettingsUseCase @Inject constructor(
    private val securitySettingsProvider: SecuritySettingsProvider
) {
    suspend fun execute(): AppResult<Unit> = securitySettingsProvider.initialize()
}