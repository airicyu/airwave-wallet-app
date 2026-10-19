package com.airwave.wallet.solana

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.URL

object SolanaJsonRpc {
    fun sendTransaction(rpcUrl: String, signedTx: ByteArray): String? {
        val encoded = LegacyTransaction.base64Wire(signedTx)
        val params = JSONArray().put(encoded).put(
            JSONObject()
                .put("encoding", "base64")
                .put("skipPreflight", false),
        )
        val result = post(rpcUrl, "sendTransaction", params)
        return result?.optString("result", null)
    }

    fun estimateFeeLamports(rpcUrl: String, messageBase64: String): BigInteger? {
        val params = JSONArray().put(messageBase64).put(
            JSONObject().put("sigVerify", false),
        )
        val result = post(rpcUrl, "simulateTransaction", params)?.optJSONObject("result")
            ?: return null
        val err = result.optJSONObject("err")
        if (err != null && err.length() > 0) return null
        val value = result.optJSONObject("value")
        val fee = value?.optLong("fee", -1L) ?: -1L
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
            if (json.has("error")) null else json
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }
}
