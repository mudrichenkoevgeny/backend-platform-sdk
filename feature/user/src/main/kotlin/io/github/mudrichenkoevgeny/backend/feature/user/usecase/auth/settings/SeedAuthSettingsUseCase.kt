package io.github.mudrichenkoevgeny.backend.feature.user.usecase.auth.settings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeedAuthSettingsUseCase @Inject constructor(
    private val authSettingsProvider: AuthSettingsProvider
) {
    suspend fun execute(): AppResult<Unit> {
        return authSettingsProvider.initialize()
    }
}