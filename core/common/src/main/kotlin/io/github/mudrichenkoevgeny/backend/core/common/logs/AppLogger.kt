package io.github.mudrichenkoevgeny.backend.core.common.logs

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError

/**
 * Abstraction for application-level logging of [AppError]s.
 *
 * Implementations typically route internal failures to a system logger
 * and domain or validation problems to a business logger, so that
 * operational and product-facing signals can be handled separately.
 */
public interface AppLogger {
    /**
     * Logs the given [appError] to the appropriate logger based on its type and severity.
     */
    public fun logError(appError: AppError)

    public companion object {
        /**
         * Name for the logger that records internal and infrastructure failures.
         */
        public const val SYSTEM_LOGGER: String = "system"

        /**
         * Name for the logger that records business errors.
         */
        public const val BUSINESS_LOGGER: String = "business"
    }
}
