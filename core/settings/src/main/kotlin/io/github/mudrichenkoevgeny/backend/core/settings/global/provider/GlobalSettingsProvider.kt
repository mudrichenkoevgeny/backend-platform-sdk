package io.github.mudrichenkoevgeny.backend.core.settings.global.provider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.global.model.GlobalSettings

interface GlobalSettingsProvider {
    suspend fun initialize(): AppResult<Unit>
    fun getSettings(): AppResult<GlobalSettings>
    suspend fun updatePrivacyPolicyUrl(url: String): AppResult<Unit>
    suspend fun updateTermsOfServiceUrl(url: String): AppResult<Unit>
    suspend fun updateContactSupportEmail(email: String): AppResult<Unit>
}