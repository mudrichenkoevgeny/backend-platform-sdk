package io.github.mudrichenkoevgeny.backend.feature.user.auth.verifier

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GoogleAuthVerifierTest {

    @Test
    fun `verify returns ExternalIdMismatch when web client id is missing`() = runBlocking {
        val verifier = GoogleAuthVerifier(webClientId = null)

        val result = verifier.verify(token = "any")

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is UserError.ExternalIdMismatch)
        assertNull((error as UserError.ExternalIdMismatch).throwable)
    }

    @Test
    fun `verify returns ExternalIdMismatch when token is invalid`() = runBlocking {
        val verifier = GoogleAuthVerifier(webClientId = "client-id")

        val result = verifier.verify(token = "invalid-token")

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is UserError.ExternalIdMismatch)
        assertNotNull((error as UserError.ExternalIdMismatch).throwable)
    }
}

