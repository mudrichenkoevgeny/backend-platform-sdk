package io.github.mudrichenkoevgeny.backend.core.common.di.qualifiers

import javax.inject.Qualifier
import org.slf4j.Logger

/**
 * Qualifier for the system/technical [Logger].
 *
 * This logger is intended for infrastructure- and platform-level events, not for domain/business logs.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class SystemLogger