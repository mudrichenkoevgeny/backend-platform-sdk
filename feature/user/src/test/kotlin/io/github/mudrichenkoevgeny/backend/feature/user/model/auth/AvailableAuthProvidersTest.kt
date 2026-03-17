package io.github.mudrichenkoevgeny.backend.feature.user.model.auth

import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.ExternalAuthProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AvailableAuthProvidersTest {

    @Test
    fun `supportedExternalProviders returns only external providers that are enabled`() {
        val googleAuthProvider = ExternalAuthProvider.Google.userAuthProvider
        val appleAuthProvider = ExternalAuthProvider.Apple.userAuthProvider

        val model = AvailableAuthProviders(
            primary = listOf(googleAuthProvider),
            secondary = emptyList()
        )

        assertEquals(setOf(ExternalAuthProvider.Google), model.supportedExternalProviders)

        val model2 = AvailableAuthProviders(
            primary = emptyList(),
            secondary = listOf(appleAuthProvider)
        )

        assertEquals(setOf(ExternalAuthProvider.Apple), model2.supportedExternalProviders)
    }

    @Test
    fun `supportedExternalProviders returns empty set when no external providers are enabled`() {
        val model = AvailableAuthProviders(
            primary = emptyList(),
            secondary = emptyList()
        )

        assertEquals(emptySet<ExternalAuthProvider>(), model.supportedExternalProviders)
    }
}

