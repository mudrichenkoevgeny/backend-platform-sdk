package io.github.mudrichenkoevgeny.backend.core.security.config.factory

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.security.config.envkeys.SecurityEnvKeys
import io.github.mudrichenkoevgeny.backend.core.security.config.model.SecurityConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityConfigFactoryImpl @Inject constructor(
    private val envReader: EnvReader
): SecurityConfigFactory {

    override fun create(): SecurityConfig {
        val authenticationConfirmationValidityMinutes = envReader
            .getByKey(SecurityEnvKeys.AUTHENTICATION_CONFIRMATION_VALIDITY_MINUTES).toLong()

        return SecurityConfig(
            authenticationConfirmationValidityMinutes = authenticationConfirmationValidityMinutes,
        )
    }
}