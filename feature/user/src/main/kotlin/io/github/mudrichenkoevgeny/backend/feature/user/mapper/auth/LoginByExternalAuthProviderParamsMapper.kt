package io.github.mudrichenkoevgeny.backend.feature.user.mapper.auth

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.RequestHandlingException
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.auth.login.LoginByExternalAuthProviderParams
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserApiFields
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.auth.login.LoginByExternalAuthProviderRequest

/**
 * Converts a [LoginByExternalAuthProviderRequest] DTO into a domain-layer [LoginByExternalAuthProviderParams] holder.
 *
 * Performs strict validation and parsing for [UserAuthProvider].
 *
 * @receiver [LoginByExternalAuthProviderRequest] HTTP request payload.
 * @return [LoginByExternalAuthProviderParams] domain model containing mapped authentication fields.
 * @throws [RequestHandlingException] if [LoginByExternalAuthProviderRequest.authProvider] cannot be parsed.
 */
fun LoginByExternalAuthProviderRequest.toLoginByExternalAuthProviderParams(): LoginByExternalAuthProviderParams {
    val parsedAuthProvider = UserAuthProvider.fromValueOrNull(authProvider)
        ?: throw RequestHandlingException(
            CommonError.InvalidFieldValue(
                fieldName = UserApiFields.AUTH_PROVIDER
            )
        )

    return LoginByExternalAuthProviderParams(
        authProvider = parsedAuthProvider,
        token = externalProviderToken,
        displayName = displayName
    )
}
