package io.github.mudrichenkoevgeny.backend.core.common.result

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class AppResultTest {

    @Test
    fun `fold returns onSuccess result for Success`() {
        val result: AppResult<Int> = AppResult.Success(42)

        val folded = result.fold(
            onSuccess = { it * 2 },
            onFailure = { -1 }
        )

        assertEquals(84, folded)
    }

    @Test
    fun `fold returns onFailure result for Error`() {
        val error = CommonError.Unknown("boom")
        val result: AppResult<Int> = AppResult.Error(error)

        val folded = result.fold(
            onSuccess = { it * 2 },
            onFailure = { -1 }
        )

        assertEquals(-1, folded)
    }

    @Test
    fun `mapNotNullOrError unwraps non null value`() {
        val result: AppResult<String?> = AppResult.Success("value")
        val fallback = CommonError.Unknown("fallback")

        val mapped = result.mapNotNullOrError(fallback)

        require(mapped is AppResult.Success)
        assertEquals("value", mapped.data)
    }

    @Test
    fun `mapNotNullOrError converts null to error`() {
        val result: AppResult<String?> = AppResult.Success(null)
        val fallback = CommonError.Unknown("fallback")

        val mapped = result.mapNotNullOrError(fallback)

        require(mapped is AppResult.Error)
        assertSame(fallback, mapped.error)
    }

    @Test
    fun `mapNotNullOrError propagates existing error`() {
        val fallback = CommonError.Unknown("fallback")
        val original = CommonError.Unknown("original")
        val result: AppResult<String?> = AppResult.Error(original)

        val mapped = result.mapNotNullOrError(fallback)

        require(mapped is AppResult.Error)
        assertSame(original, mapped.error)
    }

    @Test
    fun `onSuccess runs block only for success`() {
        val success = AppResult.Success(10)
        val error = AppResult.Error(CommonError.Unknown("err"))

        var counter = 0
        success.onSuccess { counter += it }
        error.onSuccess { counter += 100 }

        assertEquals(10, counter)
    }

    @Test
    fun `onError runs block only for error`() {
        val success = AppResult.Success(10)
        val originalError = CommonError.Unknown("err")
        val error = AppResult.Error(originalError)

        var captured: CommonError.Unknown? = null
        success.onError { captured = it as CommonError.Unknown }
        error.onError { captured = it as CommonError.Unknown }

        assertSame(originalError, captured)
    }

    @Test
    fun `flatMapSuccess maps success and flattens`() {
        val result: AppResult<Int> = AppResult.Success(2)

        val mapped: AppResult<String> = result.flatMapSuccess { value ->
            AppResult.Success("v=$value")
        }

        require(mapped is AppResult.Success)
        assertEquals("v=2", mapped.data)
    }

    @Test
    fun `flatMapSuccess propagates error without calling transform`() {
        val error = CommonError.Unknown("err")
        val result: AppResult<Int> = AppResult.Error(error)

        var called = false
        val mapped: AppResult<String> = result.flatMapSuccess {
            called = true
            AppResult.Success("ignored")
        }

        require(mapped is AppResult.Error)
        assertSame(error, mapped.error)
        assertEquals(false, called)
    }
}

