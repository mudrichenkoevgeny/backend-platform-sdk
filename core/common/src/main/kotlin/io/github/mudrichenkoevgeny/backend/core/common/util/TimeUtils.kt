package io.github.mudrichenkoevgeny.backend.core.common.util

import java.time.Instant as JavaInstant
import kotlin.time.Instant as KotlinInstant

fun KotlinInstant.toJavaInstant(): JavaInstant =
    JavaInstant.ofEpochMilli(toEpochMilliseconds())

fun KotlinInstant?.toJavaInstantOrNull(): JavaInstant? =
    this?.toJavaInstant()

fun JavaInstant.toKotlinInstant(): KotlinInstant =
    KotlinInstant.fromEpochMilliseconds(toEpochMilli())

fun JavaInstant?.toKotlinInstantOrNull(): KotlinInstant? =
    this?.toKotlinInstant()