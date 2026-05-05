package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.auth.settings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.ManagementAuthSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetManagementAuthSettingsUseCase @Inject constructor(
    private val authSettingsProvider: AuthSettingsProvider
) {
    /**
     * Retrieves authentication settings.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE], [UserAccountStatus.READ_ONLY]
     * for the management caller.
     *
     * **Security:**
     * - Requires the management caller (STAFF or ADMIN) to be in an allowed status.
     * - Access is restricted to management roles, but no additional specific functional permissions are required.
     *
     * **Workflow:**
     * 1. Fetches the comprehensive [ManagementAuthSettings] via [authSettingsProvider].
     *
     * @return [AppResult] containing the [ManagementAuthSettings].
     */
    operator fun invoke(): AppResult<ManagementAuthSettings> {
        return AppResult.Success(authSettingsProvider.getManagementAuthSettings())
    }
}
