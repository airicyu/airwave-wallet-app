package com.airwave.wallet.holdings

import com.airwave.wallet.settings.Cluster
import com.airwave.wallet.settings.RpcResolver
import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI

/**
 * Decides the Helius wallet-API path and maps SPL rows from one page.
 * It does not draw the assets list or read the vault.
 */
object HeliusHoldings {
    /** Holds one Helius wallet-API page and whether the round is complete. */
    data class Page(val rows: List<HoldingRow>, val hasMore: Boolean, val full: Boolean)

    fun apiKey(url: String): String? {
        if (!RpcResolver.isAllowedCustomUrl(url)) return null
        val uri = try {
            URI(url.trim())
        } catch (_: Exception) {
            return null
        }
        if (!uri.host.equals("api.helius.xyz", ignoreCase = true)) return null
        val query = uri.rawQuery ?: return null
        var apiKey = ""
        var apiKeyAlt = ""
        for (part in query.split("&")) {
            val bits = part.split("=", limit = 2)
            if (bits.size != 2) continue
            val value = bits[1].trim()
            if (value.isEmpty()) continue
            if (bits[0] == "api-key") apiKey = value
            if (bits[0] == "apiKey" && apiKeyAlt.isEmpty()) apiKeyAlt = value
        }
        val chosen = apiKey.ifEmpty { apiKeyAlt }
        return chosen.ifEmpty { null }
    }

    fun useWalletApi(cluster: Cluster, heliusApiUrl: String): Boolean =
        cluster == Cluster.Mainnet && apiKey(heliusApiUrl) != null

    fun page(body: String): Page? {
        if (!body.trimStart().startsWith("{")) return null
        val objects = topLevelObjects(body, "balances")
        val rows = objects.mapNotNull { row(it) }.filter { it.id != HoldingIds.WRAPPED }
        val hasMore = Regex(""""hasMore"\s*:\s*true""").containsMatchIn(body)
        return Page(rows, hasMore, objects.size >= 100)
    }

    private fun row(rawObject: String): HoldingRow? {
        val mint = Regex(""""mint"\s*:\s*"([^"]+)"""").find(rawObject)?.groupValues?.get(1) ?: return null
        val balanceText = JsonNumbers.numberToken(rawObject, "balance") ?: return null
        val decimalsText = JsonNumbers.numberToken(rawObject, "decimals") ?: return null
        if (!decimalsText.matches(Regex("\\d+"))) return null
        val decimals = decimalsText.toIntOrNull() ?: return null
        if (decimals !in 0..18) return null
        val scaled = try {
            BigDecimal(balanceText).movePointRight(decimals)
        } catch (_: Exception) {
            return null
        }
        if (scaled.stripTrailingZeros().scale() > 0) return null
        val raw = scaled.toBigInteger()
        if (raw.signum() == 0 || decimals == 0) return null
        val programText = Regex(""""tokenProgram"\s*:\s*"([^"]+)"""").find(rawObject)?.groupValues?.get(1)
        val program = if (programText == "token-2022") TokenProgramKind.Token2022 else TokenProgramKind.SplToken
        return HoldingRow(
            id = mint,
            name = "",
            symbol = "",
            raw = raw,
            decimals = decimals,
            tokenProgram = program,
        )
    }

    private fun topLevelObjects(body: String, field: String): List<String> {
        val key = "\"$field\""
        val start = body.indexOf(key)
        if (start < 0) return emptyList()
        val bracket = body.indexOf('[', start)
        if (bracket < 0) return emptyList()
        val objects = mutableListOf<String>()
        var index = bracket + 1
        while (index < body.length) {
            while (index < body.length && body[index].isWhitespace() || index < body.length && body[index] == ',') {
                index++
            }
            if (index >= body.length || body[index] == ']') break
            if (body[index] != '{') break
            val end = matchObject(body, index) ?: break
            objects.add(body.substring(index, end + 1))
            index = end + 1
        }
        return objects
    }

    private fun matchObject(body: String, open: Int): Int? {
        var depth = 0
        var inString = false
        var escaped = false
        for (index in open until body.length) {
            val char = body[index]
            if (inString) {
                if (escaped) escaped = false
                else if (char == '\\') escaped = true
                else if (char == '"') inString = false
                continue
            }
            when (char) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return index
                }
            }
        }
        return null
    }
}
