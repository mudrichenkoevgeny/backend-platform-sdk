package io.github.mudrichenkoevgeny.backend.feature.securityapi.usecase.management.settings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.ManagementSecuritySettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetManagementSecuritySettingsUseCase @Inject constructor(
    private val securitySettingsProvider: SecuritySettingsProvider
) {
    /**
     * Retrieves a management snapshot of the effective security settings and policies.
     *
     * **Authorization:**
     * - **Public Access:** Allowed.
     *
     * **Security:**
     * - Provides management security parameters.
     *
     * **Workflow:**
     * 1. Fetches the current effective management security settings via [securitySettingsProvider].
     *
     * @return [AppResult] containing the [ManagementSecuritySettings].
     */
    operator fun invoke(): AppResult<ManagementSecuritySettings> {
        return AppResult.Success(securitySettingsProvider.getManagementSecuritySettings())
    }
}