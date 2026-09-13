package io.github.mudrichenkoevgeny.backend.core.common.network.request.handler

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.shared.foundation.core.common.validation.NotBlankStringField
import io.github.mudrichenkoevgeny.shared.foundation.core.common.validation.NotEmptyCollectionField
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class DtoValidatorTest {

    @Test
    fun `validateDto does nothing when no validation annotations`() {
        TestNoAnnotationsDto("x").validateDto()
    }

    @Test
    fun `validateDto throws MissingRequiredField using property name`() {
        val ex = assertThrows(RequestHandlingException::class.java) {
            TestRequiredDto(null).validateDto()
        }
        val error = ex.error as CommonError.MissingRequiredField
        assertEquals("field", error.fieldName)
    }

    @Test
    fun `validateDto throws MissingRequiredField using SerialName`() {
        val ex = assertThrows(RequestHandlingException::class.java) {
            TestSerialNameDto(null).validateDto()
        }
        val error = ex.error as CommonError.MissingRequiredField
        assertEquals("wire_name", error.fieldName)
    }

    @Test
    fun `validateDto throws BlankStringField when string blank`() {
        val ex = assertThrows(RequestHandlingException::class.java) {
            TestNotBlankDto("   ").validateDto()
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
            TestNotEmptyDto(emptyList()).validateDto()
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
        TestRequiredDto("ok").validateDto()
        TestNotBlankDto("a").validateDto()
        TestNotEmptyDto(listOf("a")).validateDto()
    }
}
