package io.github.mudrichenkoevgeny.backend.core.security.config.model

import io.github.mudrichenkoevgeny.backend.core.security.domain.model.securitysettings.createTestManagementSecuritySettings
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.ManagementSecuritySettings

fun createTestSecurityConfig(
    authRealm: String = "test-realm",
    totpEncryptionSecret: String = "test-secret",
    managementSecuritySettings: ManagementSecuritySettings = createTestManagementSecuritySettings()
) = SecurityConfig(
    authRealm = authRealm,
    totpEncryptionSecret = totpEncryptionSecret,
    managementSecuritySettings = managementSecuritySettings
)
