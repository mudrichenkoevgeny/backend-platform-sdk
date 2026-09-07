package io.github.mudrichenkoevgeny.backend.feature.securityapi.usecase.open.settings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.OpenSecuritySettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetOpenSecuritySettingsUseCase @Inject constructor(
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
     * - Provides open security parameters.
     *
     * **Workflow:**
     * 1. Fetches the current effective open security settings via [securitySettingsProvider].
     *
     * @return [AppResult] containing the [OpenSecuritySettings].
     */
    operator fun invoke(): AppResult<OpenSecuritySettings> {
        return AppResult.Success(securitySettingsProvider.getOpenSecuritySettings())
    }
}