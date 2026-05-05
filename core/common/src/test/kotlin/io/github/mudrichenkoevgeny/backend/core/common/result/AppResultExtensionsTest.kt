package io.github.mudrichenkoevgeny.backend.core.common.result

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppErrorSeverity
import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AppResultExtensionsTest {

    private val testError = object : AppError {
        override val errorId = ErrorId.generate()
        override val code = "test_error"
        override val publicArgs = null
        override val secretArgs = null
        override val httpStatusCode = HttpStatusCode.BadRequest
        override val appErrorSeverity = AppErrorSeverity.LOW
    }

    @Test
    fun `mapNotNullOrError returns success when data is not null`() {
        val result: AppResult<String?> = AppResult.Success("data")
        val mapped = result.mapNotNullOrError(testError)

        assertTrue(mapped is AppResult.Success)
        assertEquals("data", (mapped as AppResult.Success).data)
    }

    @Test
    fun `mapNotNullOrError returns error when data is null`() {
        val result: AppResult<String?> = AppResult.Success(null)
        val mapped = result.mapNotNullOrError(testError)

        assertTrue(mapped is AppResult.Error)
        assertEquals(testError, (mapped as AppResult.Error).error)
    }

    @Test
    fun `mapSuccess transforms data`() {
        val result = AppResult.Success(10)
        val mapped = result.mapSuccess { it * 2 }

        assertEquals(20, (mapped as AppResult.Success).data)
    }

    @Test
    fun `flatMapSuccess chains results`() {
        val result = AppResult.Success(10)
        val mapped = result.flatMapSuccess { AppResult.Success(it.toString()) }

        assertEquals("10", (mapped as AppResult.Success).data)
    }

    @Test
    fun `onSuccess and onError execute side effects`() {
        var successData: Int? = null
        var caughtError: AppError? = null

        AppResult.Success(1).onSuccess { successData = it }
        AppResult.Error(testError).onError { caughtError = it }

        assertEquals(1, successData)
        assertEquals(testError, caughtError)
    }

    @Test
    fun `combine returns success when all are successful`() {
        val list = listOf(AppResult.Success(1), AppResult.Success(2))
        val combined = list.combine()

        assertTrue(combined is AppResult.Success)
        assertEquals(listOf(1, 2), (combined as AppResult.Success).data)
    }

    @Test
    fun `combine returns first error when encountered`() {
        val list = listOf(AppResult.Success(1), AppResult.Error(testError), AppResult.Success(2))
        val combined = list.combine()

        assertTrue(combined is AppResult.Error)
        assertEquals(testError, (combined as AppResult.Error).error)
    }

    @Test
    fun `dataOrNull returns data on success and null on error`() {
        assertEquals(5, AppResult.Success(5).dataOrNull())
        assertNull(AppResult.Error(testError).dataOrNull())
    }
}