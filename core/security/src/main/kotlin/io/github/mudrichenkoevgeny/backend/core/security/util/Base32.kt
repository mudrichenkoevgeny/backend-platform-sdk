package io.github.mudrichenkoevgeny.backend.core.security.util

/**
 * Utility for Base32 encoding and decoding as defined in RFC 4648.
 *
 * Primarily used for handling TOTP secrets. Supports cleaning input strings (removing
 * spaces and hyphens) during decoding and provides efficient bit-shifting
 * transformations without external dependencies.
 */
object Base32 {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private val DIGITS = IntArray(128) { -1 }.apply {
        ALPHABET.forEachIndexed { i, char -> this[char.code] = i }
    }

    fun encode(data: ByteArray): String {
        var i = 0
        var index = 0
        var digit: Int
        var currByte: Int
        var nextByte: Int
        val base32 = StringBuilder((data.size + 7) * 8 / 5)

        while (i < data.size) {
            currByte = data[i].toInt() and 0xFF
            if (index > 3) {
                nextByte = if (i + 1 < data.size) data[i + 1].toInt() and 0xFF else 0
                digit = currByte and (0xFF shr index)
                index = (index + 5) % 8
                digit = (digit shl index) or (nextByte shr (8 - index))
                i++
            } else {
                digit = (currByte shr (8 - (index + 5))) and 0x1F
                index = (index + 5) % 8
                if (index == 0) i++
            }
            base32.append(ALPHABET[digit])
        }
        return base32.toString()
    }

    fun decode(base32: String): ByteArray {
        val cleaned = base32.uppercase().replace("-", "").replace(" ", "")
        val out = ByteArray(cleaned.length * 5 / 8)
        var buffer = 0
        var bitsLeft = 0
        var count = 0
        for (c in cleaned) {
            val valIdx = if (c.code < 128) DIGITS[c.code] else -1
            if (valIdx == -1) continue
            buffer = (buffer shl 5) or valIdx
            bitsLeft += 5
            if (bitsLeft >= 8) {
                out[count++] = (buffer shr (bitsLeft - 8)).toByte()
                bitsLeft -= 8
            }
        }
        return out
    }
}