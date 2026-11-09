package io.github.mudrichenkoevgeny.backend.feature.user.mapper.auth

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.RequestHandlingException
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserApiFields
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.request.auth.login.LoginByExternalAuthProviderRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LoginByExternalAuthProviderParamsMapperTest {
    @Test
    fun `toLoginByExternalAuthProviderParams maps all fields correctly when displayName is provided`() {
        val request = LoginByExternalAuthProviderRequest(
            authProvider = "GOOGLE",
            externalProviderToken = "token_123",
            displayName = "John Doe"
        )

        val params = request.toLoginByExternalAuthProviderParams()

        assertEquals(UserAuthProvider.GOOGLE, params.authProvider)
        assertEquals("token_123", params.token)
        assertEquals("John Doe", params.displayName)
    }

    @Test
    fun `toLoginByExternalAuthProviderParams maps correctly when displayName is null`() {
        val request = LoginByExternalAuthProviderRequest(
            authProvider = "APPLE",
            externalProviderToken = "token_456",
            displayName = null
        )

        val params = request.toLoginByExternalAuthProviderParams()

        assertEquals(UserAuthProvider.APPLE, params.authProvider)
        assertEquals("token_456", params.token)
        assertNull(params.displayName)
    }

    @Test
    fun `toLoginByExternalAuthProviderParams throws RequestHandlingException when authProvider is invalid`() {
        val request = LoginByExternalAuthProviderRequest(
            authProvider = "INVALID_PROVIDER",
            externalProviderToken = "token_789"
        )

        val exception = assertThrows<RequestHandlingException> {
            request.toLoginByExternalAuthProviderParams()
        }

        val error = exception.error as CommonError.InvalidFieldValue
        assertEquals(UserApiFields.AUTH_PROVIDER, error.fieldName)
    }
}
