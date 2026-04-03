package io.github.mudrichenkoevgeny.backend.core.database.extensions

import org.jetbrains.exposed.v1.core.LikePattern

/**
 * Builds an Exposed [LikePattern] for SQL `LIKE` substring matching (`%fragment%`).
 *
 * This is **not** a generic `contains` check for in-memory strings; it builds a pattern for Exposed `like`
 * against a database column. Metacharacters `\`, `%`, and `_` in [needle] are escaped so user
 * input cannot widen the match. For case-insensitive search, lower-case [needle] and use
 * `lowerCase(column) like substringSqlLikePattern(needle)`.
 *
 * @param needle Inner fragment without surrounding `%`.
 * @param escapeChar SQL `LIKE` escape character; must match the escape semantics of [LikePattern] for your dialect.
 */
fun substringSqlLikePattern(needle: String, escapeChar: Char = '\\'): LikePattern {
    val escaped = needle
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")
    return LikePattern("%$escaped%", escapeChar)
}
