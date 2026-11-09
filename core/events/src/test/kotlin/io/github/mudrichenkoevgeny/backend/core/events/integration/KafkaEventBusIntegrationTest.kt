package io.github.mudrichenkoevgeny.backend.core.events.integration

import io.github.mudrichenkoevgeny.backend.core.common.testing.docker.RequiresDocker
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName

@RequiresDocker
internal class KafkaEventBusIntegrationTest {
    @Test
    fun `kafka container should start successfully for event bus integration tests`() {
        val imageName = DockerImageName.parse("confluentinc/cp-kafka:7.5.0").asCompatibleSubstituteFor("apache/kafka")
        KafkaContainer(imageName).use { kafka ->
            kafka.start()
            assertTrue(kafka.isRunning)
        }
    }
}
