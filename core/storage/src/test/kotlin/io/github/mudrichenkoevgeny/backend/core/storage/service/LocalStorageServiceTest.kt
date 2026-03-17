package io.github.mudrichenkoevgeny.backend.core.storage.service

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.storage.config.model.StorageConfig
import io.github.mudrichenkoevgeny.backend.core.storage.model.StorageType
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class LocalStorageServiceTest {

    @Test
    fun `save writes file and returns key`(@TempDir root: Path) = runBlocking {
        val config = storageConfig(root = root.toString())
        val service = LocalStorageService(config)

        val result = service.save(
            fileName = "test.txt",
            content = "hello".toByteArray(),
            contentType = "text/plain",
            bucket = null
        )

        assertTrue(result is AppResult.Success)
        assertEquals("test.txt", (result as AppResult.Success).data)
        assertTrue(root.resolve("test.txt").toFile().exists())
        assertEquals("hello", root.resolve("test.txt").toFile().readText())
    }

    @Test
    fun `save with bucket creates subdirectory`(@TempDir root: Path) = runBlocking {
        val config = storageConfig(root = root.toString())
        val service = LocalStorageService(config)

        val result = service.save(
            fileName = "file.bin",
            content = byteArrayOf(1, 2, 3),
            contentType = "application/octet-stream",
            bucket = "my-bucket"
        )

        assertTrue(result is AppResult.Success)
        assertEquals("my-bucket/file.bin", (result as AppResult.Success).data)
        assertTrue(root.resolve("my-bucket").resolve("file.bin").toFile().exists())
    }

    @Test
    fun `delete returns true when file existed`(@TempDir root: Path) = runBlocking {
        val config = storageConfig(root = root.toString())
        val service = LocalStorageService(config)
        val file = root.resolve("to-delete.txt").toFile()
        file.writeText("x")

        val result = service.delete(key = "to-delete.txt", bucket = null)

        assertTrue(result is AppResult.Success)
        assertTrue((result as AppResult.Success).data)
        assertFalse(file.exists())
    }

    @Test
    fun `delete returns false when file did not exist`(@TempDir root: Path) = runBlocking {
        val config = storageConfig(root = root.toString())
        val service = LocalStorageService(config)

        val result = service.delete(key = "missing.txt", bucket = null)

        assertTrue(result is AppResult.Success)
        assertFalse((result as AppResult.Success).data)
    }

    @Test
    fun `getUrl returns base URL plus key`() {
        val config = storageConfig(s3PublicUrl = "https://cdn.example.com")
        val service = LocalStorageService(config)

        val result = service.getUrl(key = "path/to/file.png")

        assertTrue(result is AppResult.Success)
        assertEquals("https://cdn.example.com/path/to/file.png", (result as AppResult.Success).data)
    }

    @Test
    fun `getUrl strips trailing slash from base URL`() {
        val config = storageConfig(s3PublicUrl = "https://cdn.example.com/")
        val service = LocalStorageService(config)

        val result = service.getUrl(key = "file.txt")

        assertTrue(result is AppResult.Success)
        assertEquals("https://cdn.example.com/file.txt", (result as AppResult.Success).data)
    }

    private fun storageConfig(
        root: String = "/tmp/storage",
        s3PublicUrl: String = "https://example.com/files"
    ) = StorageConfig(
        storageType = StorageType.LOCAL,
        s3Endpoint = "",
        s3Region = "",
        s3AccessKey = "",
        s3SecretKey = "",
        s3BucketName = "",
        s3PublicUrl = s3PublicUrl,
        forcePathStyle = false,
        localStoragePath = root
    )
}
