package io.github.mudrichenkoevgeny.backend.core.storage.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StorageTypeTest {

    @Test
    fun `fromString returns LOCAL when value is null`() {
        assertEquals(StorageType.LOCAL, StorageType.fromString(null))
    }

    @Test
    fun `fromString returns LOCAL when value is blank`() {
        assertEquals(StorageType.LOCAL, StorageType.fromString("   "))
    }

    @Test
    fun `fromString returns LOCAL when value is unrecognized`() {
        assertEquals(StorageType.LOCAL, StorageType.fromString("gcs"))
    }

    @Test
    fun `fromString is case-insensitive`() {
        assertEquals(StorageType.S3, StorageType.fromString("s3"))
        assertEquals(StorageType.LOCAL, StorageType.fromString("local"))
    }
}
