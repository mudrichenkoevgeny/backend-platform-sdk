package io.github.mudrichenkoevgeny.backend.feature.user.config.model

import io.github.mudrichenkoevgeny.backend.feature.user.config.seed.AdminAccount
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.auth.settings.createTestManagementAuthSettings
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.resend.model.ResendConfig
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.unione.model.UniOneConfig
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.ManagementAuthSettings

fun createTestUserConfig(
    jwtSecret: String = "test_jwt_secret_key_32_bytes_min!",
    adminAccountsList: List<AdminAccount> = emptyList(),
    managementAuthSettings: ManagementAuthSettings = createTestManagementAuthSettings(),
    googleWebClientId: String? = null,
    uniOneConfig: UniOneConfig? = null,
    resendConfig: ResendConfig? = null
) = UserConfig(
    jwtSecret = jwtSecret,
    adminAccountsList = adminAccountsList,
    managementAuthSettings = managementAuthSettings,
    googleWebClientId = googleWebClientId,
    uniOneConfig = uniOneConfig,
    resendConfig = resendConfig
)
