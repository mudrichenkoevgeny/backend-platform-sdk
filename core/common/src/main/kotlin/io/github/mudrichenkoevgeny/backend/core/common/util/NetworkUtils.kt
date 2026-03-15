package io.github.mudrichenkoevgeny.backend.core.common.util

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse

suspend inline fun <reified T> HttpResponse.bodySafe(): T? = try {
    body<T>()
} catch (_: Exception) {
    null
}