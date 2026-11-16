/**
 * Reads a stored Helius credential the way the web wallet does.
 * It does not choose an RPC host or send a request.
 */
package com.airwave.wallet.settings

import java.net.URI
import java.net.URLDecoder

/** A bare key, or the `api-key` / `apiKey` query on an http(s) URL. Empty when the text is not a key. */
fun heliusApiKeyFromStored(raw: String): String? {
    val text = raw.trim()
    if (text.isEmpty()) return null
    if (text.startsWith("https://") || text.startsWith("http://")) {
        val uri = try {
            URI(text)
        } catch (_: Exception) {
            return null
        }
        val query = uri.rawQuery ?: return null
        var apiKey = ""
        var apiKeyAlt = ""
        for (part in query.split("&")) {
            val bits = part.split("=", limit = 2)
            if (bits.size != 2) continue
            val value = decode(bits[1]).trim()
            if (value.isEmpty()) continue
            if (bits[0] == "api-key") apiKey = value
            if (bits[0] == "apiKey" && apiKeyAlt.isEmpty()) apiKeyAlt = value
        }
        val chosen = apiKey.ifEmpty { apiKeyAlt }
        return chosen.ifEmpty { null }
    }
    if (text.any { it.isWhitespace() }) return null
    return text
}

private fun decode(value: String): String = try {
    URLDecoder.decode(value, "UTF-8")
} catch (_: Exception) {
    value
}
