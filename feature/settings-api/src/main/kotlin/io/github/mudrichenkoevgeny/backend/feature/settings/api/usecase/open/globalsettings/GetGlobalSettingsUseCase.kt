package io.github.mudrichenkoevgeny.backend.feature.settings.api.usecase.open.globalsettings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.GlobalSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetGlobalSettingsUseCase @Inject constructor(
    private val globalSettingsProvider: GlobalSettingsProvider
) {
    /**
     * Retrieves a public snapshot of global platform settings.
     *
     * **Authorization:**
     * - **Public Access:** Allowed.
     *
     * **Security:**
     * - No authentication required.
     * - Provides only public-facing settings as defined in [GlobalSettings].
     *
     * **Workflow:**
     * 1. Fetches current settings snapshot via [globalSettingsProvider].
     *
     * @return [AppResult] containing the [GlobalSettings].
     */
    operator fun invoke(): AppResult<GlobalSettings> {
        return AppResult.Success(globalSettingsProvider.getSettings())
    }
}