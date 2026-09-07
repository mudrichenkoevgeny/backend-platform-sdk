package io.github.mudrichenkoevgeny.backend.feature.settingsapi.usecase.management.globalsettings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.ManagementGlobalSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetManagementGlobalSettingsUseCase @Inject constructor(
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
    operator fun invoke(): AppResult<ManagementGlobalSettings> {
        return AppResult.Success(globalSettingsProvider.getManagementGlobalSettings())
    }
}