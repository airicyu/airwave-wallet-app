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
    fun sendTransaction(rpcUrl: String, signedTx: ByteArray): String? {
        val encoded = LegacyTransaction.base64Wire(signedTx)
        val params = JSONArray().put(encoded).put(
            JSONObject()
                .put("encoding", "base64")
                .put("skipPreflight", false),
        )
        val json = post(rpcUrl, "sendTransaction", params) ?: return null
        if (json.isNull("result")) return null
        val sig = json.optString("result", "")
        return sig.takeIf { it.isNotEmpty() && it != "null" }
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
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream.bufferedReader().use(BufferedReader::readText)
            val json = JSONObject(text)
            val err = json.optJSONObject("error")
            if (err != null) null else json
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }
}
