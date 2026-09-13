package io.github.mudrichenkoevgeny.backend.core.common.util

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.Instant as JavaInstant
import kotlin.time.Instant as KotlinInstant

private const val DATE_TIME_FORMAT_DEFAULT = "yyyy-MM-dd HH:mm:ss 'UTC'"
private const val TIME_ZONE_ID_DEFAULT = "UTC"

fun KotlinInstant.toJavaInstant(): JavaInstant =
    JavaInstant.ofEpochMilli(toEpochMilliseconds())

fun KotlinInstant?.toJavaInstantOrNull(): JavaInstant? =
    this?.toJavaInstant()

fun JavaInstant.toKotlinInstant(): KotlinInstant =
    KotlinInstant.fromEpochMilliseconds(toEpochMilli())

fun JavaInstant?.toKotlinInstantOrNull(): KotlinInstant? =
    this?.toKotlinInstant()

fun Long.formatEpochMillisToUtcString(): String {
    val formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_DEFAULT)
        .withZone(ZoneId.of(TIME_ZONE_ID_DEFAULT))
    return formatter.format(JavaInstant.ofEpochMilli(this))
}