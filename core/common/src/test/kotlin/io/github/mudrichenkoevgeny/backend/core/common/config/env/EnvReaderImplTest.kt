package io.github.mudrichenkoevgeny.backend.core.common.config.env

import io.github.mudrichenkoevgeny.backend.core.common.config.pathresolver.ResolvedPaths
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class EnvReaderImplTest {

    @Test
    fun `getByKey returns value from env or file`() {
        val tempDir = createTempDirectory().toFile()
        val envFile = File(tempDir, ".env").apply {
            writeText("FOO=bar\n")
        }
        val paths = ResolvedPaths(envFile = envFile, secretsDir = tempDir)
        val reader = EnvReaderImpl(paths)

        assertEquals("bar", reader.getByKey("FOO"))
    }

    @Test
    fun `getByKeyOrNull returns null when variable missing`() {
        val tempDir = createTempDirectory().toFile()
        val envFile = File(tempDir, ".env").apply { writeText("") }
        val paths = ResolvedPaths(envFile = envFile, secretsDir = tempDir)
        val reader = EnvReaderImpl(paths)

        assertNull(reader.getByKeyOrNull("MISSING"))
    }

    @Test
    fun `readSecret reads from relative file under secretsDir`() {
        val tempDir = createTempDirectory().toFile()
        val secretsDir = File(tempDir, "secrets").apply { mkdirs() }
        val secretFile = File(secretsDir, "token.txt").apply {
            writeText("  super-secret  ")
        }
        val envFile = File(tempDir, ".env").apply { writeText("") }
        val paths = ResolvedPaths(envFile = envFile, secretsDir = secretsDir)
        val reader = EnvReaderImpl(paths)

        assertTrue(secretFile.isFile)
        assertEquals("super-secret", reader.readSecret(secretFile.name))
    }

    @Test
    fun `readSecret throws when file does not exist`() {
        val tempDir = createTempDirectory().toFile()
        val secretsDir = File(tempDir, "secrets").apply { mkdirs() }
        val envFile = File(tempDir, ".env").apply { writeText("") }
        val paths = ResolvedPaths(envFile = envFile, secretsDir = secretsDir)
        val reader = EnvReaderImpl(paths)

        assertThrows(IllegalStateException::class.java) {
            reader.readSecret("missing.txt")
        }
    }
}

