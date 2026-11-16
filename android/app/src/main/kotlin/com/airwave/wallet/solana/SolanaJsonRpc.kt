package com.airwave.wallet.solana

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.URL

/**
 * Sends a signed transaction and estimates fees over JSON-RPC for sign-and-send.
 * It does not parse instruction names or hold keys.
 */
object SolanaJsonRpc {
    /** The `sendTransaction` outcome. [errorMessage] is the JSON-RPC `error.message` when one exists. */
    data class SendResult(val signature: String?, val errorMessage: String?)

    fun sendTransaction(rpcUrl: String, signedTx: ByteArray, minContextSlot: Int? = null): SendResult {
        val encoded = LegacyTransaction.base64Wire(signedTx)
        val config = JSONObject()
            .put("encoding", "base64")
            .put("skipPreflight", false)
        if (minContextSlot != null) config.put("minContextSlot", minContextSlot)
        val params = JSONArray().put(encoded).put(config)
        val json = post(rpcUrl, "sendTransaction", params) ?: return SendResult(null, null)
        val err = json.optJSONObject("error")
        if (err != null) {
            val message = err.optString("message", "")
            return SendResult(null, message.takeIf { it.isNotEmpty() })
        }
        if (json.isNull("result")) return SendResult(null, null)
        val sig = json.optString("result", "")
        return SendResult(sig.takeIf { it.isNotEmpty() && it != "null" }, null)
    }

    fun estimateFeeLamports(rpcUrl: String, payload: ByteArray): BigInteger? {
        val message = LegacyTransaction.messageBase64(payload) ?: return null
        val params = JSONArray().put(message).put(
            JSONObject().put("encoding", "base64"),
        )
        val result = post(rpcUrl, "getFeeForMessage", params)?.optJSONObject("result")
            ?: return null
        if (result.isNull("value")) return null
        val fee = result.optLong("value", -1L)
        return if (fee >= 0) BigInteger.valueOf(fee) else null
    }

    private fun post(rpcUrl: String, method: String, params: JSONArray): JSONObject? {
        val body = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", 1)
            .put("method", method)
            .put("params", params)
        val connection = URL(rpcUrl).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream.bufferedReader().use(BufferedReader::readText)
            JSONObject(text)
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }
}
