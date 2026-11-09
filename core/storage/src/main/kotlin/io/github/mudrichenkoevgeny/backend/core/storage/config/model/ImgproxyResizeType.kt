package io.github.mudrichenkoevgeny.backend.core.storage.config.model

/**
 * Image resize fitting mode supported by imgproxy.
 *
 * @property value String representation passed to imgproxy options (such as "fit" or "fill").
 */
public enum class ImgproxyResizeType(
    public val value: String
) {
    FIT("fit"),
    FILL("fill");

    public companion object {
        /**
         * Resolves [ImgproxyResizeType] by string value ignoring case, or returns `null` if unrecognized.
         */
        public fun fromValueOrNull(value: String): ImgproxyResizeType? = entries.firstOrNull {
            it.value.equals(
                value,
                ignoreCase = true
            )
        }
    }
}
