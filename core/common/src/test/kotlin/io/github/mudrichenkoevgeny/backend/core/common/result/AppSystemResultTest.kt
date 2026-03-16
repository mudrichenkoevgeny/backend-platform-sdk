package io.github.mudrichenkoevgeny.backend.core.common.result

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class AppSystemResultTest {

    @Test
    fun `success wraps data`() {
        val result: AppSystemResult<String> = AppSystemResult.Success("ok")

        require(result is AppSystemResult.Success)
        assertEquals("ok", result.data)
    }

    @Test
    fun `error wraps internal error`() {
        val internal = CommonError.Internal(RuntimeException("boom"))

        val result: AppSystemResult<String> = AppSystemResult.Error(internal)

        require(result is AppSystemResult.Error)
        assertSame(internal, result.internalError)
    }
}

