package io.github.mudrichenkoevgeny.backend.core.security.usecase.system.settings

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * System use case for seeding default security management configurations into persistent storage.
 *
 * Runs during application bootstrap to ensure default password complexity policies, token expiration durations,
 * account lockout parameters, and MFA configurations exist in system settings.
 */
@Singleton
public class SeedSecuritySettingsUseCase @Inject constructor(
    private val securitySettingsProvider: SecuritySettingsProvider
) {
    /**
     * Seeds missing default security management settings and policies into persistent storage.
     *
     * **Allowed Account Statuses:** Any (System initialization).
     *
     * **Security:**
     * - System-level initialization component; no caller authentication or user context required.
     *
     * **Workflow:**
     * 1. Invokes [SecuritySettingsProvider.initialize] to register missing security management settings.
     * 2. Populates default password policies, token TTLs, and lockout thresholds.
     *
     * @return [AppResult.Success] on successful seeding, or an [AppError] on storage error.
     */
    public suspend operator fun invoke(): AppResult<Unit> = securitySettingsProvider.initialize()
}
