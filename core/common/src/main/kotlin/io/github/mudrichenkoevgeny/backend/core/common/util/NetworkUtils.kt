package io.github.mudrichenkoevgeny.backend.core.common.util

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse

/**
 * Tries to deserialize the HTTP response body as type [T], returning `null` on any failure instead of throwing.
 *
 * This is useful for best‑effort parsing of optional payloads or logging endpoints, where a failure
 * to parse should not propagate as an exception.
 */
suspend inline fun <reified T> HttpResponse.bodySafe(): T? = try {
    body<T>()
} catch (_: Exception) {
    null
}