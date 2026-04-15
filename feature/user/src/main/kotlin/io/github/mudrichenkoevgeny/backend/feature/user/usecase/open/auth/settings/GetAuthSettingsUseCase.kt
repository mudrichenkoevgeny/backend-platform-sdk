package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.settings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.PublicAuthSettings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case: fetch authentication settings for clients (e.g. available auth providers).
 *
 * Delegates to [AuthSettingsProvider.getPublicAuthSettings].
 */
@Singleton
class GetAuthSettingsUseCase @Inject constructor(
    private val authSettingsProvider: AuthSettingsProvider
) {
    operator fun invoke(): AppResult<PublicAuthSettings> {
        return authSettingsProvider.getPublicAuthSettings()
    }
}
