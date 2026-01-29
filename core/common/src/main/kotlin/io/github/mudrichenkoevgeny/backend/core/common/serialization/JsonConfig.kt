package io.github.mudrichenkoevgeny.backend.core.common.serialization

import kotlinx.serialization.json.Json

val DefaultJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    coerceInputValues = true
}