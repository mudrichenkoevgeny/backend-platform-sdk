package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.settings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.ManagementGlobalSettings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Management use case for retrieving an effective snapshot of global platform settings.
 *
 * Exposes platform contact emails, policy URLs, minimum supported client versions, and feature flags to staff and admin clients.
 */
@Singleton
public class GetManagementGlobalSettingsUseCase @Inject constructor(
    private val globalSettingsProvider: GlobalSettingsProvider
) {
    /**
     * Retrieves a management snapshot of global platform settings.
     *
     * **Authorization:**
     * - **Public Access:** Allowed.
     *
     * **Security:**
     * - Provides management-facing settings as defined in [ManagementGlobalSettings].
     *
     * **Workflow:**
     * 1. Fetches current management settings snapshot via [globalSettingsProvider].
     *
     * @return [AppResult] containing the [ManagementGlobalSettings].
     */
    public operator fun invoke(): AppResult<ManagementGlobalSettings> =
        AppResult.Success(globalSettingsProvider.getManagementGlobalSettings())
}
