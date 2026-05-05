package io.github.mudrichenkoevgeny.backend.feature.security.api.usecase.open.settings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.SecuritySettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetSecuritySettingsUseCase @Inject constructor(
    private val securitySettingsProvider: SecuritySettingsProvider
) {
    /**
     * Retrieves a public snapshot of the effective security settings and policies.
     *
     * **Authorization:**
     * - **Public Access:** Allowed.
     *
     * **Security:**
     * - No authentication required.
     * - Provides security parameters .
     *
     * **Workflow:**
     * 1. Fetches the current effective security settings via [securitySettingsProvider].
     *
     * @return [AppResult] containing the [SecuritySettings].
     */
    operator fun invoke(): AppResult<SecuritySettings> {
        return AppResult.Success(securitySettingsProvider.getSettings())
    }
}