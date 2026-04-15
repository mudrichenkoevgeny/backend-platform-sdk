package io.github.mudrichenkoevgeny.backend.core.common.network.request.handler

import io.github.mudrichenkoevgeny.shared.foundation.core.common.validation.NotBlankStringField
import io.github.mudrichenkoevgeny.shared.foundation.core.common.validation.RequiredField
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.request.receive

/**
 * Receives the request body as type [T] and performs field validation using annotations.
 * 1. Uses [receive] to deserialize the JSON body.
 * 2. Calls [validateDto] to check for [RequiredField], [NotBlankStringField], etc.
 * @return The validated instance of type [T].
 * @throws ContentTransformationException if the JSON is malformed or types mismatch.
 * @throws RequestHandlingException if any field validation fails.
 */
suspend inline fun <reified T : Any> ApplicationCall.validateRequest(): T {
    val request = receive<T>()
    request.validateDto()
    return request
}