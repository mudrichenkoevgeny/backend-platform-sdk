package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.auth.settings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.ManagementAuthSettings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case: fetch authentication settings for management flows.
 *
 * Delegates to [AuthSettingsProvider.getManagementAuthSettings].
 */
@Singleton
class GetManagementAuthSettingsUseCase @Inject constructor(
    private val authSettingsProvider: AuthSettingsProvider
) {
    fun execute(): AppResult<ManagementAuthSettings> {
        return authSettingsProvider.getManagementAuthSettings()
    }
}
