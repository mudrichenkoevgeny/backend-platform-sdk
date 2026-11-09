package io.github.mudrichenkoevgeny.backend.feature.user.usecase.system.auth.settings

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * System use case for seeding default authentication configurations into persistent storage.
 *
 * Runs during application bootstrap to ensure platform-wide JWT expiration policies, refresh token rotation grace
 * periods, lockout thresholds, and TOTP settings exist in system settings.
 */
@Singleton
public class SeedAuthSettingsUseCase @Inject constructor(
    private val authSettingsProvider: AuthSettingsProvider
) {
    /**
     * Seeds missing default authentication settings and policies into persistent storage.
     *
     * **Allowed Account Statuses:** Any (System initialization).
     *
     * **Security:**
     * - System-level initialization component; no caller authentication or user context required.
     *
     * **Workflow:**
     * 1. Invokes [AuthSettingsProvider.initialize] to register missing default authentication settings.
     * 2. Populates JWT token TTLs, refresh token rotation grace periods, and lockout thresholds.
     *
     * @return [AppResult.Success] on successful seeding, or an [AppError] on storage failure.
     */
    public suspend operator fun invoke(): AppResult<Unit> = authSettingsProvider.initialize()
}
