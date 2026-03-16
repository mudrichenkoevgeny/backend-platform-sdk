package io.github.mudrichenkoevgeny.backend.core.common.config.pathresolver

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class PathResolverImplTest {

    private val logger = mockk<AppLogger>(relaxed = true)

    @Test
    fun `getResolvedPaths returns existing env and secrets paths`() {
        val tempRoot = createTempDirectory().toFile()
        val secretsDir = File(tempRoot, "secrets").apply { mkdirs() }
        val envFile = File(tempRoot, ".env").apply { writeText("FOO=bar") }

        val config = PathResolverConfig(
            projectRoot = tempRoot,
            envFilePath = envFile.name,
            secretsDirPath = secretsDir.name,
        )

        val resolver = PathResolverImpl(config, logger)

        val resolved = resolver.getResolvedPaths()

        assertEquals(envFile.absolutePath, resolved.envFile.absolutePath)
        assertEquals(secretsDir.absolutePath, resolved.secretsDir.absolutePath)
    }

    @Test
    fun `missing secrets path logs and throws`() {
        val tempRoot = createTempDirectory().toFile()
        val config = PathResolverConfig(
            projectRoot = tempRoot,
            envFilePath = ".env",
            secretsDirPath = null,
        )

        assertThrows(IllegalStateException::class.java) {
            PathResolverImpl(config, logger)
        }
        verify { logger.logError(any<CommonError.Internal>()) }
    }

    @Test
    fun `missing env file logs and throws`() {
        val tempRoot = createTempDirectory().toFile()
        val secretsDir = File(tempRoot, "secrets").apply { mkdirs() }

        val config = PathResolverConfig(
            projectRoot = tempRoot,
            envFilePath = null,
            secretsDirPath = secretsDir.name,
        )

        assertThrows(IllegalStateException::class.java) {
            PathResolverImpl(config, logger)
        }
        verify { logger.logError(any<CommonError.Internal>()) }
    }
}

