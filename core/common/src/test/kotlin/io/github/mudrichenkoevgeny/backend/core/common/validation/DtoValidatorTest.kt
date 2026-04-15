package io.github.mudrichenkoevgeny.backend.core.common.validation

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.RequestHandlingException
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.validateDto
import io.github.mudrichenkoevgeny.shared.foundation.core.common.validation.NotBlankStringField
import io.github.mudrichenkoevgeny.shared.foundation.core.common.validation.NotEmptyCollectionField
import io.github.mudrichenkoevgeny.shared.foundation.core.common.validation.RequiredField
import kotlinx.serialization.SerialName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class DtoValidatorTest {

    data class NoAnnotationsDto(val value: String)

    data class RequiredDto(@RequiredField val field: String?)

    data class SerialNameDto(
        @SerialName("wire_name")
        @RequiredField
        val kotlinName: String?
    )

    data class NotBlankDto(@NotBlankStringField val text: String)

    data class NotEmptyDto(@NotEmptyCollectionField val items: List<String>)

    @Test
    fun `validateDto does nothing when no validation annotations`() {
        NoAnnotationsDto("x").validateDto()
    }

    @Test
    fun `validateDto throws MissingRequiredField using property name`() {
        val ex = assertThrows(RequestHandlingException::class.java) {
            RequiredDto(null).validateDto()
        }
        val error = ex.error as CommonError.MissingRequiredField
        assertEquals("field", error.fieldName)
    }

    @Test
    fun `validateDto throws MissingRequiredField using SerialName`() {
        val ex = assertThrows(RequestHandlingException::class.java) {
            SerialNameDto(null).validateDto()
        }
        val error = ex.error as CommonError.MissingRequiredField
        assertEquals("wire_name", error.fieldName)
    }

    @Test
    fun `validateDto throws BlankStringField when string blank`() {
        val ex = assertThrows(RequestHandlingException::class.java) {
            NotBlankDto("   ").validateDto()
        }
        val error = ex.error as CommonError.BlankStringField
        assertEquals("text", error.fieldName)
    }

    @Test
    fun `validateDto throws BlankStringField when value not string`() {
        data class WrongTypeDto(@NotBlankStringField val count: Int)

        val ex = assertThrows(RequestHandlingException::class.java) {
            WrongTypeDto(1).validateDto()
        }
        assertEquals(CommonError.BlankStringField::class, ex.error::class)
    }

    @Test
    fun `validateDto throws EmptyCollectionField when collection empty`() {
        val ex = assertThrows(RequestHandlingException::class.java) {
            NotEmptyDto(emptyList()).validateDto()
        }
        val error = ex.error as CommonError.EmptyCollectionField
        assertEquals("items", error.fieldName)
    }

    @Test
    fun `validateDto throws EmptyCollectionField when value not collection`() {
        data class WrongTypeDto(@NotEmptyCollectionField val text: String)

        val ex = assertThrows(RequestHandlingException::class.java) {
            WrongTypeDto("x").validateDto()
        }
        assertEquals(CommonError.EmptyCollectionField::class, ex.error::class)
    }

    @Test
    fun `validateDto completes when all annotated fields valid`() {
        RequiredDto("ok").validateDto()
        NotBlankDto("a").validateDto()
        NotEmptyDto(listOf("a")).validateDto()
    }
}
