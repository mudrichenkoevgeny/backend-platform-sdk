package io.github.mudrichenkoevgeny.backend.core.settings.integration

import io.github.mudrichenkoevgeny.backend.core.common.testing.docker.RequiresDocker
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

@RequiresDocker
internal class RedisSettingsIntegrationTest {
    @Test
    fun `redis container should start successfully for settings cache sync`() {
        GenericContainer(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379).use { redis ->
            redis.start()
            assertTrue(redis.isRunning)
        }
    }
}
