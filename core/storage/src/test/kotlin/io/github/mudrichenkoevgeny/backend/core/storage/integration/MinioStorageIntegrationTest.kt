package io.github.mudrichenkoevgeny.backend.core.storage.integration

import io.github.mudrichenkoevgeny.backend.core.common.testing.docker.RequiresDocker
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.testcontainers.containers.MinIOContainer

@RequiresDocker
internal class MinioStorageIntegrationTest {
    @Test
    fun `minio container should start successfully for S3 storage tests`() {
        MinIOContainer("minio/minio:RELEASE.2024-01-16T16-07-38Z").use { minio ->
            minio.withUserName("minioadmin")
            minio.withPassword("minioadmin")
            minio.start()
            assertTrue(minio.isRunning)
        }
    }
}
