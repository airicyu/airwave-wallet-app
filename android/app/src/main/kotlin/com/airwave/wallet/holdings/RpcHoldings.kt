package com.airwave.wallet.holdings

import org.json.JSONArray
import org.json.JSONObject
import java.math.BigInteger

/**
 * Builds the native, wrapped, and RPC token rows for one address.
 * It does not call Jupiter or the Helius wallet API.
 */
object RpcHoldings {
    /** Holds decoded token-account fields used to build a HoldingRow. */
    data class ParsedAccount(
        val mint: String,
        val raw: BigInteger,
        val decimals: Int,
        val program: TokenProgramKind,
    )

    fun lamports(body: String): BigInteger? {
        val token = JsonNumbers.numberToken(body, "value") ?: return null
        if (!token.matches(Regex("\\d+"))) return null
        return BigInteger(token)
    }

    fun tokenAccounts(body: String, program: TokenProgramKind): List<ParsedAccount>? {
        if (Regex(""""error"\s*:""").containsMatchIn(body)) return null
        val valueKey = body.indexOf("\"value\"")
        if (valueKey < 0) return null
        val bracket = body.indexOf('[', valueKey)
        if (bracket < 0) return null
        return objectsFrom(body, bracket).mapNotNull { parseAccount(it, program) }
    }

    fun nativeRow(lamports: BigInteger): HoldingRow = HoldingRow(
        id = HoldingIds.NATIVE,
        name = "Solana",
        symbol = "SOL",
        raw = lamports,
        decimals = 9,
        tokenProgram = null,
    )

    fun rowsFromRpc(lamports: BigInteger, accounts: List<ParsedAccount>): List<HoldingRow> {
        val summed = linkedMapOf<String, ParsedAccount>()
        for (account in accounts) {
            if (account.decimals !in 0..18) continue
            if (account.raw.signum() == 0 || account.decimals == 0) continue
            val key = account.mint + ":" + account.program.name
            val existing = summed[key]
            summed[key] = if (existing == null) {
                account
            } else {
                existing.copy(raw = existing.raw.add(account.raw))
            }
        }
        val spl = summed.values.map { account ->
            val wrapped = account.mint == HoldingIds.WRAPPED
            HoldingRow(
                id = account.mint,
                name = if (wrapped) "Wrapped SOL" else "",
                symbol = if (wrapped) "wSOL" else "",
                raw = account.raw,
                decimals = account.decimals,
                tokenProgram = account.program,
            )
        }
        return order(nativeRow(lamports), spl)
    }

    fun order(native: HoldingRow, others: List<HoldingRow>): List<HoldingRow> {
        val wrapped = others.filter { it.id == HoldingIds.WRAPPED }
        val rest = others.filter { it.id != HoldingIds.WRAPPED }
        return listOf(native) + wrapped + rest
    }

    private fun parseAccount(raw: String, program: TokenProgramKind): ParsedAccount? {
        val mint = Regex(""""mint"\s*:\s*"([^"]+)"""").find(raw)?.groupValues?.get(1) ?: return null
        val amount = Regex(""""amount"\s*:\s*"(\d+)"""").find(raw)?.groupValues?.get(1) ?: return null
        val decimalsToken = JsonNumbers.numberToken(raw, "decimals") ?: return null
        if (!decimalsToken.matches(Regex("\\d+"))) return null
        val decimals = decimalsToken.toIntOrNull() ?: return null
        if (decimals !in 0..18) return null
        return ParsedAccount(mint, BigInteger(amount), decimals, program)
    }

    private fun objectsFrom(body: String, openBracket: Int): List<String> {
        val objects = mutableListOf<String>()
        var index = openBracket + 1
        while (index < body.length) {
            while (index < body.length && (body[index].isWhitespace() || body[index] == ',')) index++
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

    fun balanceParams(address: String): JSONArray =
        JSONArray().put(address).put(JSONObject().put("commitment", "confirmed"))

    fun programParams(address: String, programId: String): JSONArray =
        JSONArray().put(address).put(JSONObject().put("programId", programId)).put(
            JSONObject().put("encoding", "jsonParsed").put("commitment", "confirmed"),
        )

    fun wrappedParams(address: String): JSONArray =
        JSONArray().put(address).put(JSONObject().put("mint", HoldingIds.WRAPPED)).put(
            JSONObject().put("encoding", "jsonParsed").put("commitment", "confirmed"),
        )
}
