package io.github.mudrichenkoevgeny.backend.core.common.di.qualifiers

import org.slf4j.Logger
import javax.inject.Qualifier

/**
 * Qualifier for the system/technical [Logger].
 *
 * This logger is intended for infrastructure- and platform-level events, not for domain/business logs.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
public annotation class SystemLogger
