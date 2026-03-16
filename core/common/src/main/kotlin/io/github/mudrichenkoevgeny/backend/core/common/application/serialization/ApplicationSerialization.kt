package io.github.mudrichenkoevgeny.backend.core.common.application.serialization

import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation

/**
 * Installs Ktor [ContentNegotiation] with the shared [FoundationJson] configuration for JSON.
 *
 * This ensures consistent serialization settings (naming, defaults, polymorphism, etc.)
 * across all services using the SDK.
 */
fun Application.configureSerialization() {
    install(ContentNegotiation) { json(FoundationJson) }
}