package io.github.mudrichenkoevgeny.backend.core.common.util

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.Instant as JavaInstant
import kotlin.time.Instant as KotlinInstant

private const val DATE_TIME_FORMAT_DEFAULT = "yyyy-MM-dd HH:mm:ss 'UTC'"
private const val TIME_ZONE_ID_DEFAULT = "UTC"

/**
 * Converts this [KotlinInstant] to a [JavaInstant].
 */
public fun KotlinInstant.toJavaInstant(): JavaInstant = JavaInstant.ofEpochMilli(toEpochMilliseconds())

/**
 * Converts this nullable [KotlinInstant] to a [JavaInstant] or `null`.
 */
public fun KotlinInstant?.toJavaInstantOrNull(): JavaInstant? = this?.toJavaInstant()

/**
 * Converts this [JavaInstant] to a [KotlinInstant].
 */
public fun JavaInstant.toKotlinInstant(): KotlinInstant = KotlinInstant.fromEpochMilliseconds(toEpochMilli())

/**
 * Converts this nullable [JavaInstant] to a [KotlinInstant] or `null`.
 */
public fun JavaInstant?.toKotlinInstantOrNull(): KotlinInstant? = this?.toKotlinInstant()

/**
 * Formats this epoch milliseconds timestamp into a UTC date-time string.
 */
public fun Long.formatEpochMillisToUtcString(): String {
    val formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_DEFAULT).withZone(ZoneId.of(TIME_ZONE_ID_DEFAULT))
    return formatter.format(JavaInstant.ofEpochMilli(this))
}
