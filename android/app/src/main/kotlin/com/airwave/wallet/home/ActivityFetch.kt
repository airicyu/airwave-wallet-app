/**
 * Loads one activity page from Helius or from getSignaturesForAddress, then attaches https icons.
 * It does not keep the list or draw it.
 */
package com.airwave.wallet.home

import com.airwave.wallet.holdings.HoldingsHttp
import com.airwave.wallet.holdings.JupiterMintMeta
import com.airwave.wallet.settings.AirwaveSettings
import com.airwave.wallet.settings.Cluster
import com.airwave.wallet.settings.RpcResolver
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** One page, or a failure that must not fall back to the other source. */
data class ActivityPage(
    val mapped: ActivityMapped?,
    val failed: Boolean,
)

/** Fetches activity pages. */
object ActivityFetch {
    fun page(owner: String, settings: AirwaveSettings, before: String?): ActivityPage {
        val devnet = settings.cluster == Cluster.Devnet
        val apiKey = activityHeliusKey(settings.heliusApiUrl)
        val enhanced = settings.cluster == Cluster.Mainnet && apiKey != null
        val body = if (enhanced) {
            enhancedBody(owner, apiKey, before)
        } else {
            val endpoint = RpcResolver.endpointForCluster(settings.cluster, settings) ?: return ActivityPage(null, true)
            signatureBody(endpoint, owner, before)
        }
        if (body == null) return ActivityPage(null, true)
        val mapped = if (enhanced) rowsFromEnhanced(body, owner, false) else rowsFromSignatures(body, devnet)
        if (mapped == null) return ActivityPage(null, true)
        if (!enhanced || settings.cluster != Cluster.Mainnet) return ActivityPage(mapped, false)
        return ActivityPage(withIcons(mapped, settings), false)
    }

    private fun enhancedBody(owner: String, key: String, before: String?): String? {
        val query = StringBuilder()
        query.append("limit=20")
        query.append("&token-accounts=balanceChanged")
        query.append("&sort-order=desc")
        query.append("&commitment=confirmed")
        query.append("&api-key=").append(URLEncoder.encode(key, "UTF-8"))
        if (!before.isNullOrEmpty()) query.append("&before=").append(URLEncoder.encode(before, "UTF-8"))
        val path = "/v0/addresses/${URLEncoder.encode(owner, "UTF-8")}/transactions"
        val full = URL("https", "api.helius.xyz", path + "?" + query)
        return request(full, "GET", null)
    }

    private fun signatureBody(endpoint: String, owner: String, before: String?): String? {
        val config = JSONObject().put("limit", 20).put("commitment", "confirmed")
        if (!before.isNullOrEmpty()) config.put("before", before)
        val params = JSONArray().put(owner).put(config)
        val payload = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", 1)
            .put("method", "getSignaturesForAddress")
            .put("params", params)
            .toString()
        val body = request(URL(endpoint), "POST", payload) ?: return null
        if (Regex(""""error"\s*:""").containsMatchIn(body)) return null
        val result = objectArray(body, "result") ?: return null
        return result
    }

    private fun objectArray(body: String, field: String): String? {
        val key = "\"$field\""
        val start = body.indexOf(key)
        if (start < 0) return null
        val bracket = body.indexOf('[', start + key.length)
        if (bracket < 0) return null
        var depth = 0
        var quote = false
        var escape = false
        for (i in bracket until body.length) {
            val ch = body[i]
            if (escape) {
                escape = false
                continue
            }
            if (ch == '\\' && quote) {
                escape = true
                continue
            }
            if (ch == '"') quote = !quote
            if (quote) continue
            if (ch == '[') depth++
            if (ch == ']') {
                depth--
                if (depth == 0) return body.substring(bracket, i + 1)
            }
        }
        return null
    }

    private fun withIcons(mapped: ActivityMapped, settings: AirwaveSettings): ActivityMapped {
        val needed = mapped.rows.flatMap { it.mints }.distinct()
        if (needed.isEmpty()) return mapped
        val hits = try {
            val query = needed.joinToString(",") { URLEncoder.encode(it, "UTF-8") }
            val key = settings.jupiterApiKey.trim()
            val headers = if (key.isEmpty()) emptyMap() else mapOf("x-api-key" to key)
            val body = HoldingsHttp.get("https://api.jup.ag/tokens/v2/search?query=$query", headers) ?: return mapped
            JupiterMintMeta.hits(body)
        } catch (_: Exception) {
            return mapped
        }
        val symbols = hits.mapValues { it.value.symbol }
        val rows = mapped.rows.map { row ->
            val icons = row.mints.mapNotNull { mint ->
                val url = hits[mint]?.iconUrl
                if (url != null && url.startsWith("https:")) ActivityIcon(mint, url) else null
            }
            row.copy(detail = activityDetailWithSymbols(row.detail, row.mints, symbols), icons = icons)
        }
        return mapped.copy(rows = rows)
    }

    private fun request(url: URL, method: String, payload: String?): String? = try {
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 20_000
            connection.readTimeout = 20_000
            if (payload != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(payload.encodeToByteArray()) }
            }
            val code = connection.responseCode
            if (code !in 200..299) null else connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    } catch (_: Exception) {
        null
    }
}
