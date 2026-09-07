package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.settings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.OpenAuthSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetOpenAuthSettingsUseCase @Inject constructor(
    private val authSettingsProvider: AuthSettingsProvider
) {
    /**
     * Retrieves public-facing authentication settings for clients.
     *
     * **Allowed Account Statuses:** Any (Public access).
     *
     * **Workflow:**
     * 1. Accesses the current authentication configuration via [AuthSettingsProvider].
     * 2. Returns settings including enabled providers and password requirements.
     *
     * @return [AppResult.Success] containing [OpenAuthSettings].
     */
    operator fun invoke(): AppResult<OpenAuthSettings> {
        return AppResult.Success(authSettingsProvider.getOpenAuthSettings())
    }
}
