package com.airwave.wallet.mwa.tx

import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

/** One simulateTransaction value, or null when the RPC call itself failed. */
data class TxSimulation(
    val err: String?,
    val unitsConsumed: Long?,
    val preBalances: List<BigInteger>?,
    val postBalances: List<BigInteger>?,
    val preTokens: List<SimTokenBalance> = emptyList(),
    val postTokens: List<SimTokenBalance> = emptyList(),
)

/** One token balance row from simulateTransaction. */
data class SimTokenBalance(
    val mint: String,
    val owner: String,
    val amount: BigInteger,
    val decimals: Int,
)

/**
 * Calls simulateTransaction and isBlockhashValid for a sign-transaction approval.
 * It does not send the transaction.
 */
object SignTxRpc {
    fun simulate(rpcUrl: String, tx: ByteArray): TxSimulation? {
        val params = org.json.JSONArray().put(Base64.getEncoder().encodeToString(tx)).put(
            JSONObject()
                .put("encoding", "base64")
                .put("sigVerify", false)
                .put("replaceRecentBlockhash", false)
                .put("commitment", "confirmed"),
        )
        val value = post(rpcUrl, "simulateTransaction", params)?.optJSONObject("result")?.optJSONObject("value")
            ?: return null
        val err = if (value.isNull("err")) null else value.get("err").toString()
        val units = if (value.isNull("unitsConsumed")) null else value.optLong("unitsConsumed")
        return TxSimulation(
            err,
            units,
            balances(value, "preBalances"),
            balances(value, "postBalances"),
            tokens(value, "preTokenBalances"),
            tokens(value, "postTokenBalances"),
        )
    }

    fun blockhashValid(rpcUrl: String, blockhash: String): Boolean? {
        val params = org.json.JSONArray().put(blockhash).put(JSONObject().put("commitment", "confirmed"))
        val value = post(rpcUrl, "isBlockhashValid", params)?.optJSONObject("result") ?: return null
        if (value.isNull("value")) return null
        return value.optBoolean("value")
    }

    private fun tokens(value: JSONObject, key: String): List<SimTokenBalance> {
        val array = value.optJSONArray(key) ?: return emptyList()
        return List(array.length()) { index ->
            val row = array.optJSONObject(index) ?: return@List null
            val ui = row.optJSONObject("uiTokenAmount") ?: return@List null
            val amount = ui.optString("amount", "")
            if (!amount.matches(Regex("\\d+"))) return@List null
            SimTokenBalance(
                mint = row.optString("mint", ""),
                owner = row.optString("owner", ""),
                amount = BigInteger(amount),
                decimals = ui.optInt("decimals", 0),
            )
        }.filterNotNull().filter { it.mint.isNotEmpty() }
    }

    private fun balances(value: JSONObject, key: String): List<BigInteger>? {
        val array = value.optJSONArray(key) ?: return null
        return List(array.length()) { index -> BigInteger.valueOf(array.optLong(index)) }
    }

    private fun post(rpcUrl: String, method: String, params: org.json.JSONArray): JSONObject? {
        if (rpcUrl.isBlank()) return null
        val body = JSONObject().put("jsonrpc", "2.0").put("id", 1).put("method", method).put("params", params)
        val connection = URL(rpcUrl).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val json = JSONObject(stream.bufferedReader().use(BufferedReader::readText))
            if (json.optJSONObject("error") != null) null else json
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }
}
