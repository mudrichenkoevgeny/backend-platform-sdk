package io.github.mudrichenkoevgeny.backend.core.settings.global.mapper

import io.github.mudrichenkoevgeny.backend.core.settings.global.model.GlobalSettings
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.network.response.GlobalSettingsResponse

/**
 * Maps internal [GlobalSettings] model to the shared network response contract.
 */
fun GlobalSettings.toGlobalSettingsResponse() = GlobalSettingsResponse(
    privacyPolicyUrl = privacyPolicyUrl,
    termsOfServiceUrl = termsOfServiceUrl,
    contactSupportEmail = contactSupportEmail
)