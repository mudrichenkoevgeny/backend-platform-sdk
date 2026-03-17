package io.github.mudrichenkoevgeny.backend.sample

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SampleAppInfoTest {

    @Test
    fun `exposes app name and version`() {
        val info = SampleAppInfo()

        assertEquals("1.0.0", info.version)
        assertEquals("Backend-platform-sdk Sample", info.appName)
    }
}

