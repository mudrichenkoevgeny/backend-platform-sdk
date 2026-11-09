package io.github.mudrichenkoevgeny.backend.core.storage.imgproxy

import io.github.mudrichenkoevgeny.backend.core.storage.config.model.ImgproxyPreset
import io.github.mudrichenkoevgeny.backend.core.storage.config.model.ImgproxyResizeType
import io.github.mudrichenkoevgeny.backend.core.storage.config.model.createTestImgproxyConfig
import io.github.mudrichenkoevgeny.backend.core.storage.config.model.createTestStorageConfig
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.category.StorageCategory
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.FileUrlPresets
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ImgproxyUrlBuilderImplTest {
    private val testPresets = mapOf(
        FileUrlPresets.THUMBNAIL to ImgproxyPreset(
            sizePixels = 150,
            resizeType = ImgproxyResizeType.FILL
        ),
        FileUrlPresets.PREVIEW to ImgproxyPreset(
            sizePixels = 600,
            resizeType = ImgproxyResizeType.FIT
        )
    )

    private val enabledConfig = createTestImgproxyConfig(
        isEnabled = true,
        baseUrl = BASE_URL,
        key = KEY,
        salt = SALT,
        presets = testPresets,
        videoThumbnailSecond = 2
    )

    @Test
    fun `returns empty map when imgproxy is disabled`() {
        val config = createTestStorageConfig(
            imgproxyConfig = enabledConfig.copy(isEnabled = false)
        )
        val builder = ImgproxyUrlBuilderImpl(config)

        assertTrue(
            builder.buildPresetUrls(
                BUCKET,
                IMAGE_JPG_FILE,
                IMAGE_JPEG_MIME,
                StorageCategory.IMAGE
            ).isEmpty()
        )
    }

    @Test
    fun `returns empty map when imgproxyConfig is null`() {
        val config = createTestStorageConfig(imgproxyConfig = null)
        val builder = ImgproxyUrlBuilderImpl(config)

        assertTrue(
            builder.buildPresetUrls(
                BUCKET,
                IMAGE_JPG_FILE,
                IMAGE_JPEG_MIME,
                StorageCategory.IMAGE
            ).isEmpty()
        )
    }

    @Test
    fun `returns empty map for unsupported mime types such as PDF, ZIP, or MP4 video`() {
        val config = createTestStorageConfig(imgproxyConfig = enabledConfig)
        val builder = ImgproxyUrlBuilderImpl(config)

        assertTrue(
            builder.buildPresetUrls(
                BUCKET,
                DOC_PDF_FILE,
                PDF_MIME,
                StorageCategory.DOCUMENT
            ).isEmpty()
        )
        assertTrue(
            builder.buildPresetUrls(
                BUCKET,
                ARCHIVE_ZIP_FILE,
                ZIP_MIME,
                StorageCategory.OTHER
            ).isEmpty()
        )
        assertTrue(
            builder.buildPresetUrls(
                BUCKET,
                CLIP_MP4_FILE,
                VIDEO_MP4_MIME,
                StorageCategory.VIDEO
            ).isEmpty()
        )
    }

    @Test
    fun `builds signed preset URLs for raster images with fit and fill types`() {
        val config = createTestStorageConfig(imgproxyConfig = enabledConfig)
        val builder = ImgproxyUrlBuilderImpl(config)

        val presetUrls = builder.buildPresetUrls(
            BUCKET,
            AVATAR_PNG_FILE,
            IMAGE_PNG_MIME,
            StorageCategory.IMAGE
        )

        assertEquals(
            2,
            presetUrls.size
        )

        val thumbnailUrl = requireNotNull(presetUrls[FileUrlPresets.THUMBNAIL])
        val previewUrl = requireNotNull(presetUrls[FileUrlPresets.PREVIEW])

        assertTrue(thumbnailUrl.startsWith("$BASE_URL/"))
        assertTrue(thumbnailUrl.contains("/rs:fill:150:150/"))

        assertTrue(previewUrl.startsWith("$BASE_URL/"))
        assertTrue(previewUrl.contains("/rs:fit:600:600/"))
    }

    @Test
    fun `builds signed PNG rasterized URLs for vector SVG files across all presets`() {
        val config = createTestStorageConfig(imgproxyConfig = enabledConfig)
        val builder = ImgproxyUrlBuilderImpl(config)

        val presetUrls = builder.buildPresetUrls(
            BUCKET,
            LOGO_SVG_FILE,
            SVG_MIME,
            StorageCategory.IMAGE
        )

        val thumbnailUrl = requireNotNull(presetUrls[FileUrlPresets.THUMBNAIL])
        val previewUrl = requireNotNull(presetUrls[FileUrlPresets.PREVIEW])

        assertTrue(thumbnailUrl.contains("/rs:fill:150:150/fmt:png/"))
        assertTrue(previewUrl.contains("/rs:fit:600:600/fmt:png/"))
    }

    @Test
    fun `uses insecure signature when key or salt is blank`() {
        val config = createTestStorageConfig(
            imgproxyConfig = enabledConfig.copy(
                key = "",
                salt = ""
            )
        )
        val builder = ImgproxyUrlBuilderImpl(config)

        val presetUrls = builder.buildPresetUrls(
            BUCKET,
            PHOTO_JPG_FILE,
            IMAGE_JPEG_MIME,
            StorageCategory.IMAGE
        )
        val thumbnailUrl = requireNotNull(presetUrls[FileUrlPresets.THUMBNAIL])

        assertTrue(thumbnailUrl.startsWith("$BASE_URL/insecure/"))
    }

    private companion object {
        private const val BASE_URL = "http://localhost:8081"
        private const val KEY = "943f4c9b9fd6938a00ef01a6cbd990552a8f220d51832e0c5294f8cb66d6f380"
        private const val SALT = "4c0e123a968603f48c8b6727d9284242"
        private const val BUCKET = "uploads"
        private const val IMAGE_JPG_FILE = "image.jpg"
        private const val PHOTO_JPG_FILE = "photo.jpg"
        private const val AVATAR_PNG_FILE = "avatar.png"
        private const val LOGO_SVG_FILE = "logo.svg"
        private const val CLIP_MP4_FILE = "clip.mp4"
        private const val DOC_PDF_FILE = "doc.pdf"
        private const val ARCHIVE_ZIP_FILE = "archive.zip"

        private const val IMAGE_JPEG_MIME = "image/jpeg"
        private const val IMAGE_PNG_MIME = "image/png"
        private const val SVG_MIME = "image/svg+xml"
        private const val VIDEO_MP4_MIME = "video/mp4"
        private const val PDF_MIME = "application/pdf"
        private const val ZIP_MIME = "application/zip"
    }
}
