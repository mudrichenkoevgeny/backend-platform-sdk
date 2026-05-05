package io.github.mudrichenkoevgeny.backend.feature.user.auth.verifier

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.naming.CommonErrorArgs
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GoogleAuthVerifierTest {

    private companion object {
        private const val ANY_TOKEN = "any"
        private const val INVALID_TOKEN = "invalid-token"
        private const val WEB_CLIENT_ID = "client-id"
    }

    @Test
    fun `verify returns ExternalIdMismatch when web client id is missing`() = runBlocking {
        val verifier = GoogleAuthVerifier(webClientId = null)

        val result = verifier.verify(token = ANY_TOKEN)

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error as UserError.ExternalIdentifierLinkageFailed

        assertNull(error.secretArgs?.get(CommonErrorArgs.MESSAGE))
    }

    @Test
    fun `verify returns ExternalIdMismatch when token is invalid`() = runBlocking {
        val verifier = GoogleAuthVerifier(webClientId = WEB_CLIENT_ID)

        val result = verifier.verify(token = INVALID_TOKEN)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.ExternalIdentifierLinkageFailed)
    }

    @Test
    fun `provider is correctly set to GOOGLE`() {
        val verifier = GoogleAuthVerifier(webClientId = WEB_CLIENT_ID)
        assertEquals(UserAuthProvider.GOOGLE, verifier.provider)
    }
}