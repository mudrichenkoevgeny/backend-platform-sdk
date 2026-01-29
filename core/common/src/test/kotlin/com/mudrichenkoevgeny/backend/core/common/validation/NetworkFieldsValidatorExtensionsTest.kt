/*
package io.github.mudrichenkoevgeny.backend.core.common.validation

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ValidateRequestTest {

    @Serializable
    data class TestRequest(
        @SerialName("required_field") @RequiredField val requiredField: String?,
        @SerialName("not_blank_field") @NotBlankStringField val notBlankField: String,
        @SerialName("not_empty_list") @NotEmptyCollectionField val listField: List<String>
    )

    private val call = mockk<ApplicationCall>(relaxed = true)

    @Test
    fun `should throw ValidationException if required field is null`() {
        // GIVEN
        val request = TestRequest(null, "ok", listOf("item"))
        coEvery { call.receive<TestRequest>() } returns request

        // WHEN + THEN
        val exception = assertThrows(ValidationException::class.java) {
            runBlocking { call.validateRequest<TestRequest>() }
        }
        assertTrue(exception.error is CommonError.MissingRequiredField)
        assertEquals("required_field", (exception.error as CommonError.MissingRequiredField).fieldName)
    }

    @Test
    fun `should throw ValidationException if not blank string is blank`() {
        // GIVEN
        val request = TestRequest("present", "   ", listOf("item"))
        coEvery { call.receive<TestRequest>() } returns request

        // WHEN + THEN
        val exception = assertThrows(ValidationException::class.java) {
            runBlocking { call.validateRequest<TestRequest>() }
        }
        assertTrue(exception.error is CommonError.BlankStringField)
        assertEquals("not_blank_field", (exception.error as CommonError.BlankStringField).fieldName)
    }

    @Test
    fun `should throw ValidationException if not empty collection is empty`() {
        // GIVEN
        val request = TestRequest("present", "ok", emptyList())
        coEvery { call.receive<TestRequest>() } returns request

        // WHEN + THEN
        val exception = assertThrows(ValidationException::class.java) {
            runBlocking { call.validateRequest<TestRequest>() }
        }
        assertTrue(exception.error is CommonError.EmptyCollectionField)
        assertEquals("not_empty_list", (exception.error as CommonError.EmptyCollectionField).fieldName)
    }

    @Test
    fun `should return request when all fields are valid`() {
        // GIVEN
        val request = TestRequest("present", "ok", listOf("item"))
        coEvery { call.receive<TestRequest>() } returns request

        // WHEN
        val result = runBlocking { call.validateRequest<TestRequest>() }

        // THEN
        assertEquals(request, result)
    }
}*/
