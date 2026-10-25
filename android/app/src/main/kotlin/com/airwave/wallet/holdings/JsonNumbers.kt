/**
 * Reads a JSON number token as the characters on the wire.
 * Does not interpret balances with floating point.
 */
package com.airwave.wallet.holdings

object JsonNumbers {
    fun numberToken(raw: String, field: String): String? {
        val quoted = Regex(""""$field"\s*:\s*"(\d+(?:\.\d+)?)"""").find(raw)
        if (quoted != null) return quoted.groupValues[1]
        val bare = Regex(""""$field"\s*:\s*(\d+(?:\.\d+)?)(?![\d.eE])""").find(raw) ?: return null
        return bare.groupValues[1]
    }
}
