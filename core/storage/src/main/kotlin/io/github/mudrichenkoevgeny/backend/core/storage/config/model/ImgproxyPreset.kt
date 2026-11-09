package io.github.mudrichenkoevgeny.backend.core.storage.config.model

/**
 * Configuration specification for a single imgproxy resolution preset.
 *
 * @property sizePixels Target square dimension in pixels (such as 150 or 600).
 * @property resizeType Image resize fitting mode ([ImgproxyResizeType]).
 */
public data class ImgproxyPreset(
    val sizePixels: Int,
    val resizeType: ImgproxyResizeType
)
