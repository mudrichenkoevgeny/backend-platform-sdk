package io.github.mudrichenkoevgeny.backend.core.common.network.request.handler

import io.ktor.http.Parameters
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.ApplicationRequest
import io.mockk.every
import io.mockk.mockk

fun createTestApplicationCallWithQuery(queryParameters: Parameters): ApplicationCall {
    val request = mockk<ApplicationRequest>()
    every { request.queryParameters } returns queryParameters
    val call = mockk<ApplicationCall>()
    every { call.request } returns request
    return call
}

enum class TestSort(val wire: String) {
    DEFAULT("default"),
    CUSTOM("custom");

    companion object {
        fun fromWireOrNull(raw: String): TestSort? = entries.find { it.wire == raw }
    }
}
