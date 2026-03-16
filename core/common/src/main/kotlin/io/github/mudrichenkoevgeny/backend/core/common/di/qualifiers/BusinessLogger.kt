package io.github.mudrichenkoevgeny.backend.core.common.di.qualifiers

import javax.inject.Qualifier
import org.slf4j.Logger

/**
 * Qualifier for the business-purpose [Logger].
 *
 * This logger is intended for domain/business events (e.g. user actions),
 * as opposed to low-level system/technical logging.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class BusinessLogger