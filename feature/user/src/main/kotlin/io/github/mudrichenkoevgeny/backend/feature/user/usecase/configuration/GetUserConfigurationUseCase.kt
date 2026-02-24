package io.github.mudrichenkoevgeny.backend.feature.user.usecase.configuration

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.model.configuration.UserConfiguration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetUserConfigurationUseCase @Inject constructor(
    private val globalSettingsProvider: GlobalSettingsProvider,
    private val securitySettingsProvider: SecuritySettingsProvider,
    private val authSettingsProvider: AuthSettingsProvider
) {
    fun execute(): AppResult<UserConfiguration> {
        val globalSettingsResult = globalSettingsProvider.getSettings()
        if (globalSettingsResult is AppResult.Error) {
            return AppResult.Error(globalSettingsResult.error)
        }

        val securitySettingsResult = securitySettingsProvider.getSettings()
        if (securitySettingsResult is AppResult.Error) {
            return AppResult.Error(securitySettingsResult.error)
        }

        val authSettingsResult = authSettingsProvider.getSettings()
        if (authSettingsResult is AppResult.Error) {
            return AppResult.Error(authSettingsResult.error)
        }

        return AppResult.Success(
            UserConfiguration(
                globalSettings = (globalSettingsResult as AppResult.Success).data,
                securitySettings = (securitySettingsResult as AppResult.Success).data,
                authSettings = (authSettingsResult as AppResult.Success).data
            )
        )
    }
}