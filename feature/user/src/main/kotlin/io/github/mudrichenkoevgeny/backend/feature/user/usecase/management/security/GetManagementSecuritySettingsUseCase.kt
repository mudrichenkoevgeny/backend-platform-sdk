package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.security

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.ManagementSecuritySettings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Management use case for retrieving an effective snapshot of global security settings and password policies.
 *
 * Exposes current password complexity rules, session confirmation policies, and lockout configurations to administrative clients.
 */
@Singleton
public class GetManagementSecuritySettingsUseCase @Inject constructor(
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
    public operator fun invoke(): AppResult<ManagementSecuritySettings> =
        AppResult.Success(securitySettingsProvider.getManagementSecuritySettings())
}
