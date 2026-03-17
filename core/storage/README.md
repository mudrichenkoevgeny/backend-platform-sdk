# core/storage

Object storage module for SDK-based applications. Provides a single abstraction ([StorageService]) with two default backends:

- **S3-compatible storage** via AWS SDK v2 ([S3StorageService])
- **Local filesystem** storage ([LocalStorageService])

The backend is selected from configuration ([StorageConfig]) during DI wiring.

## What it provides

- **Config**: [StorageConfig] built by [StorageConfigFactory] from env via [StorageEnvKeys].
- **Storage API**: [StorageService] with `save`, `delete`, and `getUrl`.
- **Implementations**:
  - [S3StorageService] — uploads/deletes objects in S3-compatible storage.
  - [LocalStorageService] — saves/deletes files on local filesystem.
- **DI wiring**: [StorageModules] aggregates config and service selection.

## Environment variables

The default config factory ([StorageConfigFactoryImpl]) reads:

- `STORAGE_TYPE` — `"S3"` or `"LOCAL"` (fallback: `"LOCAL"`).
- `S3_ENDPOINT` — S3 endpoint URL (AWS or S3-compatible, e.g. MinIO).
- `S3_REGION` — region name for the AWS client.
- `S3_BUCKET_NAME` — default bucket name.
- `S3_PUBLIC_URL` — base public URL used to build object URLs.
- `S3_FORCE_PATH_STYLE` — `"true"`/`"false"` to force path-style addressing.
- `LOCAL_STORAGE_PATH` — root directory for local storage.
- `S3_ACCESS_KEY_FILE` — path to a file containing S3 access key.
- `S3_SECRET_KEY_FILE` — path to a file containing S3 secret key.

See: [StorageEnvKeys].

## Usage

- Add dependency on `core:storage`. Depends on `core:common`.
- Install [StorageModules] in your Dagger component.
- Inject [StorageService] where your feature/application needs to store blobs (avatars, attachments, exports, etc.).

### Save a file

```kotlin
@Serializable
data class AvatarUploadedEvent(val userId: String, val avatarKey: String)

suspend fun saveExample(storage: StorageService) {
    val result = storage.save(
        fileName = "avatar.png",
        content = byteArrayOf(/* ... */),
        contentType = "image/png",
        bucket = null
    )

    // result is AppResult.Success(key) or AppResult.Error(appError)
}
```

### Delete a file

```kotlin
suspend fun deleteExample(storage: StorageService) {
    val result = storage.delete(key = "avatar.png")
}
```

### Build a public URL

```kotlin
fun urlExample(storage: StorageService): AppResult<String> {
    return storage.getUrl(key = "avatar.png")
}
```

## Notes

- **Local backend + public URLs**: [LocalStorageService.getUrl] uses [StorageConfig.s3PublicUrl] as a base. This enables a uniform URL strategy
  across backends (e.g. local files served through a reverse proxy under the same public base URL).
- **Bucket parameter**:
  - For S3, `bucket` overrides the configured default bucket.
  - For local filesystem, `bucket` is treated as a subdirectory under `LOCAL_STORAGE_PATH`.

[StorageConfig]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/storage/config/model/StorageConfig.kt
[StorageEnvKeys]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/storage/config/envkeys/StorageEnvKeys.kt
[StorageConfigFactory]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/storage/config/factory/StorageConfigFactory.kt
[StorageConfigFactoryImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/storage/config/factory/StorageConfigFactoryImpl.kt
[StorageService]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/storage/service/StorageService.kt
[LocalStorageService]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/storage/service/LocalStorageService.kt
[LocalStorageService.getUrl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/storage/service/LocalStorageService.kt
[S3StorageService]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/storage/service/S3StorageService.kt
[StorageModules]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/storage/di/StorageModules.kt

