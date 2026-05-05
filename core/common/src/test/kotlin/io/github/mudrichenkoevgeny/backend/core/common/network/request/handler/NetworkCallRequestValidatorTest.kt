package io.github.mudrichenkoevgeny.backend.core.common.network.request.handler

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.shared.foundation.core.common.validation.NotBlankStringField
import io.github.mudrichenkoevgeny.shared.foundation.core.common.validation.NotEmptyCollectionField
import io.github.mudrichenkoevgeny.shared.foundation.core.common.validation.RequiredField
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class NetworkCallRequestValidatorTest {

    @Serializable
    data class TestRequest(
        @SerialName("required_field") @RequiredField val requiredField: String?,
        @SerialName("not_blank_field") @NotBlankStringField val notBlankField: String,
        @SerialName("not_empty_list") @NotEmptyCollectionField val listField: List<String>
    )

    private val call = mockk<ApplicationCall>(relaxed = true)

    @Test
    fun `validateRequest throws RequestHandlingException when required field is null`() {
        val request = TestRequest(null, "ok", listOf("item"))
        coEvery { call.receive<TestRequest>() } returns request

        val exception = assertThrows(RequestHandlingException::class.java) {
            runBlocking { call.validateRequest<TestRequest>() }
        }
        val error = exception.error as CommonError.MissingRequiredField
        assertEquals("required_field", error.fieldName)
    }

    @Test
    fun `validateRequest throws RequestHandlingException when not blank string is blank`() {
        val request = TestRequest("present", "   ", listOf("item"))
        coEvery { call.receive<TestRequest>() } returns request

        val exception = assertThrows(RequestHandlingException::class.java) {
            runBlocking { call.validateRequest<TestRequest>() }
        }
        val error = exception.error as CommonError.BlankStringField
        assertEquals("not_blank_field", error.fieldName)
    }

    @Test
    fun `validateRequest throws RequestHandlingException when not empty collection is empty`() {
        val request = TestRequest("present", "ok", emptyList())
        coEvery { call.receive<TestRequest>() } returns request

        val exception = assertThrows(RequestHandlingException::class.java) {
            runBlocking { call.validateRequest<TestRequest>() }
        }
        val error = exception.error as CommonError.EmptyCollectionField
        assertEquals("not_empty_list", error.fieldName)
    }

    @Test
    fun `validateRequest returns request when all fields are valid`() {
        val request = TestRequest("present", "ok", listOf("item"))
        coEvery { call.receive<TestRequest>() } returns request

        val result = runBlocking { call.validateRequest<TestRequest>() }

        assertEquals(request, result)
    }
}

