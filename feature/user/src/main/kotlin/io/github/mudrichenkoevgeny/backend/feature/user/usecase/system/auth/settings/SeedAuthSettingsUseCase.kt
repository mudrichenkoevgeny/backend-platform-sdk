package io.github.mudrichenkoevgeny.backend.feature.user.usecase.system.auth.settings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Synchronizes default authentication settings from static configuration to the dynamic settings storage.
 *
 * @return [AppResult.Success] if all settings are registered.
 */
@Singleton
public class SeedAuthSettingsUseCase @Inject constructor(
    private val authSettingsProvider: AuthSettingsProvider
) {
    /**
     * Initializes and seeds authentication settings.
     */
    public suspend operator fun invoke(): AppResult<Unit> = authSettingsProvider.initialize()
}
