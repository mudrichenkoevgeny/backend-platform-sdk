package io.github.mudrichenkoevgeny.backend.core.common.server

import io.github.mudrichenkoevgeny.backend.core.common.config.common.model.createTestCommonConfig
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class KtorServerFactoryTest {

    @Test
    fun `create builds server with two connectors`() {
        val config = createTestCommonConfig()

        val server = KtorServer.create(config) { }

        assertNotNull(server)
    }
}
