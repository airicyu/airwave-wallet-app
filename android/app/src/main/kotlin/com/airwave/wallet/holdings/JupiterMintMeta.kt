/**
 * Patches Mainnet rows with Jupiter token search metadata.
 * Does not change base units or call Solana RPC.
 */
package com.airwave.wallet.holdings

import com.airwave.wallet.vault.BurnerKeys
import org.json.JSONArray
import org.json.JSONObject

object JupiterMintMeta {
    data class Hit(
        val name: String,
        val symbol: String,
        val iconUrl: String?,
        val verified: Boolean,
        val organicScore: String?,
        val usdPrice: String?,
    )

    fun hits(body: String): Map<String, Hit> {
        val array = try {
            JSONArray(body)
        } catch (_: Exception) {
            return emptyMap()
        }
        val objects = objectsInArray(body)
        val map = linkedMapOf<String, Hit>()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val id = item.optString("id", "")
            if (id.isEmpty()) continue
            val raw = objects.getOrNull(index) ?: item.toString()
            map[id] = Hit(
                name = item.optString("name", "").trim(),
                symbol = item.optString("symbol", "").trim(),
                iconUrl = httpsIcon(item.optString("icon", "")),
                verified = item.optBoolean("isVerified", false),
                organicScore = JsonNumbers.numberToken(raw, "organicScore"),
                usdPrice = JsonNumbers.numberToken(raw, "usdPrice"),
            )
        }
        return map
    }

    fun apply(rows: List<HoldingRow>, hits: Map<String, Hit>): List<HoldingRow> {
        val wrapped = hits[HoldingIds.WRAPPED]
        return rows.map { row ->
            val hit = if (row.id == HoldingIds.NATIVE || row.id == HoldingIds.WRAPPED) wrapped else hits[row.id]
            if (hit == null) fallback(row) else patch(row, hit)
        }
    }

    private fun patch(row: HoldingRow, hit: Hit): HoldingRow {
        val fixed = row.id == HoldingIds.NATIVE || row.id == HoldingIds.WRAPPED
        val name = if (fixed || hit.name.isEmpty() || hit.symbol.isEmpty()) row.name else hit.name
        val symbol = if (fixed || hit.name.isEmpty() || hit.symbol.isEmpty()) row.symbol else hit.symbol
        val price = hit.usdPrice
        val label = if (price == null) "—" else BaseUnits.usdLabel(price, row.raw, row.decimals) ?: "—"
        val sort = if (price == null) null else BaseUnits.usdSort(price, row.raw, row.decimals)
        val badge = if (fixed) null else hit.organicScore?.takeIf { BaseUnits.scoreAboveZero(it) }
        return fallback(
            row.copy(
                name = name,
                symbol = symbol,
                iconUrl = hit.iconUrl,
                verified = hit.verified,
                organicScore = badge,
                usdLabel = label,
                usdSort = sort,
            ),
        )
    }

    fun fallback(row: HoldingRow): HoldingRow {
        if (row.id == HoldingIds.NATIVE || row.id == HoldingIds.WRAPPED) return row
        val short = BurnerKeys.short(row.id)
        return row.copy(
            name = row.name.ifEmpty { short },
            symbol = row.symbol.ifEmpty { short },
        )
    }

    private fun httpsIcon(raw: String): String? =
        raw.trim().takeIf { it.startsWith("https:") }

    private fun objectsInArray(body: String): List<String> {
        val objects = mutableListOf<String>()
        var index = body.indexOf('[')
        if (index < 0) return objects
        index++
        while (index < body.length) {
            while (index < body.length && (body[index].isWhitespace() || body[index] == ',')) index++
            if (index >= body.length || body[index] == ']') break
            if (body[index] != '{') break
            var depth = 0
            var inString = false
            var escaped = false
            var end = index
            for (cursor in index until body.length) {
                val char = body[cursor]
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
                        if (depth == 0) {
                            end = cursor
                            break
                        }
                    }
                }
            }
            objects.add(body.substring(index, end + 1))
            index = end + 1
        }
        return objects
    }
}
