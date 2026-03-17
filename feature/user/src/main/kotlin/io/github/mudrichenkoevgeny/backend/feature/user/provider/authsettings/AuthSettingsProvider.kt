package io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AuthSettings
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AvailableAuthProviders

/**
 * Provides runtime authentication settings for the user feature.
 *
 * The provider is responsible for:
 * - seeding system settings with defaults during startup ([initialize]);
 * - returning the current effective settings for request handling ([getSettings]);
 * - persisting updates to auth-provider availability ([updateAvailableAuthProviders]).
 */
interface AuthSettingsProvider {
    /**
     * Registers defaults required by this feature in the system settings storage.
     *
     * @return [AppResult.Success] on successful registration, or [AppResult.Error] on failure
     */
    suspend fun initialize(): AppResult<Unit>

    /**
     * Returns the current effective [AuthSettings].
     *
     * @return [AppResult.Success] with the settings. Errors are not expected for in-memory reads.
     */
    fun getSettings(): AppResult<AuthSettings>

    /**
     * Persists the enabled auth providers used by clients.
     *
     * @param availableAuthProviders enabled providers split by UI priority
     * @return [AppResult.Success] when stored, or [AppResult.Error] when persistence failed
     */
    suspend fun updateAvailableAuthProviders(availableAuthProviders: AvailableAuthProviders): AppResult<Unit>
}