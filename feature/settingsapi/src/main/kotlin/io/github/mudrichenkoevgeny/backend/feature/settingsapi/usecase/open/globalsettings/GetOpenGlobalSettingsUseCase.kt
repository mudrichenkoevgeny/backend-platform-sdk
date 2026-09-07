package io.github.mudrichenkoevgeny.backend.feature.settingsapi.usecase.open.globalsettings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.OpenGlobalSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetOpenGlobalSettingsUseCase @Inject constructor(
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
     * - Provides only public-facing settings as defined in [OpenGlobalSettings].
     *
     * **Workflow:**
     * 1. Fetches current settings snapshot via [globalSettingsProvider].
     *
     * @return [AppResult] containing the [OpenGlobalSettings].
     */
    operator fun invoke(): AppResult<OpenGlobalSettings> {
        return AppResult.Success(globalSettingsProvider.getOpenGlobalSettings())
    }
}