/**
 * HTTP GET and Solana JSON-RPC POST for holdings fetches.
 * Does not parse token rows or keep a snapshot.
 */
package com.airwave.wallet.holdings

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

object HoldingsHttp {
    fun get(url: String, headers: Map<String, String> = emptyMap()): String? = try {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
            val code = connection.responseCode
            if (code !in 200..299) null else connection.inputStream.bufferedReader().use(BufferedReader::readText)
        } finally {
            connection.disconnect()
        }
    } catch (_: Exception) {
        null
    }

    fun rpc(url: String, method: String, params: JSONArray): String? = try {
        val body = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", 1)
            .put("method", method)
            .put("params", params)
            .toString()
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { it.write(body.encodeToByteArray()) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            stream?.bufferedReader()?.use(BufferedReader::readText)
        } finally {
            connection.disconnect()
        }
    } catch (_: Exception) {
        null
    }
}
