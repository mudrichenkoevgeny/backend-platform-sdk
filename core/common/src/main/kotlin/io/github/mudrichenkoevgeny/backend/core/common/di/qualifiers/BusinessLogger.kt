package io.github.mudrichenkoevgeny.backend.core.common.di.qualifiers

import org.slf4j.Logger
import javax.inject.Qualifier

/**
 * Qualifier for the business-purpose [Logger].
 *
 * This logger is intended for domain/business events (e.g. user actions),
 * as opposed to low-level system/technical logging.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
public annotation class BusinessLogger
