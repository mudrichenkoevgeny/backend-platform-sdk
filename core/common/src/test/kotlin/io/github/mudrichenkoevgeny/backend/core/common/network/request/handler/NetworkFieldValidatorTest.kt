package io.github.mudrichenkoevgeny.backend.core.common.network.request.handler

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class FieldValueValidatorTest {

    @Test
    fun `validateFieldValue returns parsed value when parser succeeds`() {
        val input = "123"
        val fieldName = "count"

        val result: Int = input.validateFieldValue(fieldName) { it.toIntOrNull() }

        assertEquals(123, result)
    }

    @Test
    fun `validateFieldValue throws RequestHandlingException with InvalidFieldValue when parser returns null`() {
        val input = "not_a_number"
        val fieldName = "age"

        val ex = assertThrows(RequestHandlingException::class.java) {
            input.validateFieldValue<Int>(fieldName) { it.toIntOrNull() }
        }

        val error = ex.error as CommonError.InvalidFieldValue
        assertEquals(fieldName, error.fieldName)
    }

    @Test
    fun `validateFieldValue works with complex transformation`() {
        val input = "ACTIVE"
        val fieldName = "status"

        val result = input.validateFieldValue(fieldName) {
            runCatching { TestEnum.valueOf(it) }.getOrNull()
        }

        assertEquals(TestEnum.ACTIVE, result)
    }

    private enum class TestEnum {
        ACTIVE, INACTIVE
    }
}