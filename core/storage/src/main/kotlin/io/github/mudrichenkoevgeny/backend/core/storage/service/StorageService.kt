package io.github.mudrichenkoevgeny.backend.core.storage.service

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult

interface StorageService {
    suspend fun save(
        fileName: String,
        content: ByteArray,
        contentType: String,
        bucket: String? = null
    ): AppResult<String>

    suspend fun delete(key: String, bucket: String? = null): AppResult<Boolean>

    fun getUrl(key: String): AppResult<String>
}