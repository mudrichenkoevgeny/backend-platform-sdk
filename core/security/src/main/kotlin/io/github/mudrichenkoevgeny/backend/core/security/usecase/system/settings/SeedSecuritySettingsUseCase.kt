package io.github.mudrichenkoevgeny.backend.core.security.usecase.system.settings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeedSecuritySettingsUseCase @Inject constructor(
    private val securitySettingsProvider: SecuritySettingsProvider
) {
    /**
     * Seeds initial security configurations and defaults into the system storage.
     *
     * **Workflow:**
     * 1. Invokes securitySettingsProvider.initialize() to ensure all required security
     *    settings and policies are registered if they do not already exist.
     *
     * **Execution Context:**
     * - Intended for use during application bootstrap or system initialization.
     * - Does not require a user context as it operates at the system level.
     *
     * @return [AppResult] indicating whether the initialization was successful.
     */
    suspend operator fun invoke(): AppResult<Unit> = securitySettingsProvider.initialize()
}