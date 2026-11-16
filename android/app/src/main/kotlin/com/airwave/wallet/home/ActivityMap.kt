/**
 * Maps one Helius enhanced payload, or one signature-list entry, into activity rows.
 * It does not fetch or draw the list.
 */
package com.airwave.wallet.home

import com.airwave.wallet.vault.BurnerKeys
import java.math.BigInteger
import java.net.URLEncoder

/** One activity row the home list can draw. */
data class ActivityRow(
    val signature: String,
    val kind: ActivityKind,
    val lead: ActivityLead,
    val detail: String,
    val timestampSec: Long?,
    val orbUrl: String,
    val mints: List<String>,
    val icons: List<ActivityIcon>,
)

/** Token image attached after a Jupiter lookup. */
data class ActivityIcon(val mint: String, val iconUrl: String)

/** Send, receive, swap, or a transaction the list cannot classify. */
enum class ActivityKind { Send, Receive, Swap, Tx }

/** Failure or success lead. None means the detail stands alone. */
enum class ActivityLead { Fail, Ok, None }

/** Mapped rows plus the raw array length and the next-page cursor. */
data class ActivityMapped(
    val rows: List<ActivityRow>,
    val rawCount: Int,
    val cursor: String?,
)

private const val SOL_MINT = "So11111111111111111111111111111111111111112"
private const val USDC = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"
private const val USDT = "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB"
private const val PAGE = 20

private data class Leg(val outgoing: Boolean, val label: String, val counterparty: String, val mint: String)
private data class Side(val text: String, val mint: String)

/** Local `YYYY-MM-DD HH:MM:SS` for a unix second. Empty when the second is missing. */
fun activityWhen(timestampSec: Long?): String {
    if (timestampSec == null) return ""
    val cal = java.util.Calendar.getInstance()
    cal.timeInMillis = timestampSec * 1000
    return "%04d-%02d-%02d %02d:%02d:%02d".format(
        cal.get(java.util.Calendar.YEAR),
        cal.get(java.util.Calendar.MONTH) + 1,
        cal.get(java.util.Calendar.DAY_OF_MONTH),
        cal.get(java.util.Calendar.HOUR_OF_DAY),
        cal.get(java.util.Calendar.MINUTE),
        cal.get(java.util.Calendar.SECOND),
    )
}

/** Orb transaction URL for this cluster. */
fun orbTxUrl(signature: String, devnet: Boolean): String {
    val sig = URLEncoder.encode(signature, "UTF-8")
    val cluster = if (devnet) "devnet" else "mainnet-beta"
    return "https://orb.helius.dev/tx/$sig?cluster=$cluster"
}

/** True when the URL is an Orb transaction link. */
fun isOrbTxUrl(url: String): Boolean = url.startsWith("https://orb.helius.dev/tx/")

/** Replaces a mint's short address in the detail when a usable symbol exists. */
fun activityDetailWithSymbols(detail: String, mints: List<String>, symbols: Map<String, String>): String {
    var out = detail
    for (mint in mints) {
        val symbol = symbols[mint]?.trim().orEmpty()
        if (symbol.isEmpty() || symbol.contains("…") || symbol == mint) continue
        val short = BurnerKeys.short(mint)
        if (short.isEmpty() || short == symbol || !out.contains(short)) continue
        out = out.split(short).joinToString(symbol)
    }
    return out
}

/** Maps a top-level JSON array of enhanced transactions. A non-array yields a null page. */
fun rowsFromEnhanced(body: String, owner: String, devnet: Boolean): ActivityMapped? {
    val array = topLevelArray(body.trim()) ?: return null
    return mapArray(array, owner, devnet, enhanced = true)
}

/** Maps a `getSignaturesForAddress` result array. */
fun rowsFromSignatures(body: String, devnet: Boolean): ActivityMapped? {
    val array = topLevelArray(body.trim()) ?: return null
    return mapArray(array, owner = "", devnet, enhanced = false)
}

private fun mapArray(array: List<String>, owner: String, devnet: Boolean, enhanced: Boolean): ActivityMapped {
    val rows = mutableListOf<ActivityRow>()
    var cursor: String? = null
    for (raw in array) {
        val signature = jsonString(raw, "signature")
        if (signature.isNotEmpty()) cursor = signature
        val row = if (enhanced) rowFromEnhanced(raw, owner, devnet) else rowFromSignature(raw, devnet)
        if (row != null && rows.size < PAGE) rows.add(row)
    }
    return ActivityMapped(rows, array.size, cursor)
}

private fun rowFromSignature(raw: String, devnet: Boolean): ActivityRow? {
    val signature = jsonString(raw, "signature")
    if (signature.isEmpty()) return null
    val err = jsonField(raw, "err")
    val failed = err != null && err != "null"
    val block = jsonInt(raw, "blockTime")
    return ActivityRow(
        signature = signature,
        kind = ActivityKind.Tx,
        lead = if (failed) ActivityLead.Fail else ActivityLead.Ok,
        detail = BurnerKeys.short(signature),
        timestampSec = block,
        orbUrl = orbTxUrl(signature, devnet),
        mints = emptyList(),
        icons = emptyList(),
    )
}

private fun rowFromEnhanced(raw: String, owner: String, devnet: Boolean): ActivityRow? {
    val signature = jsonString(raw, "signature")
    if (signature.isEmpty()) return null
    val type = jsonString(raw, "type").uppercase()
    val failed = jsonField(raw, "transactionError").let { it != null && it != "null" }
    var kind = ActivityKind.Tx
    var detail = BurnerKeys.short(signature)
    var mints = emptyList<String>()
    if (type == "SWAP") {
        kind = ActivityKind.Swap
        val sides = swapSides(raw, owner)
        detail = if (sides.first != null && sides.second != null) {
            "${sides.first!!.text} → ${sides.second!!.text}"
        } else {
            BurnerKeys.short(signature)
        }
        mints = uniqueMints(listOfNotNull(sides.first?.mint, sides.second?.mint))
    } else if (type == "TRANSFER") {
        val legs = legsForOwner(raw, owner)
        kind = classify(legs)
        detail = detailFromLeg(kind, legs, signature)
        mints = mintsFor(kind, legs)
    } else {
        mints = uniqueMints(legsForOwner(raw, owner).map { it.mint })
    }
    return ActivityRow(
        signature = signature,
        kind = kind,
        lead = if (failed) ActivityLead.Fail else ActivityLead.None,
        detail = detail,
        timestampSec = jsonInt(raw, "timestamp"),
        orbUrl = orbTxUrl(signature, devnet),
        mints = mints,
        icons = emptyList(),
    )
}

private fun classify(legs: List<Leg>): ActivityKind {
    val out = legs.any { it.outgoing }
    val inn = legs.any { !it.outgoing }
    if (out && !inn) return ActivityKind.Send
    if (inn && !out) return ActivityKind.Receive
    return ActivityKind.Tx
}

private fun detailFromLeg(kind: ActivityKind, legs: List<Leg>, signature: String): String {
    val leg = when (kind) {
        ActivityKind.Send -> legs.firstOrNull { it.outgoing }
        ActivityKind.Receive -> legs.firstOrNull { !it.outgoing }
        else -> null
    } ?: return BurnerKeys.short(signature)
    if (kind == ActivityKind.Send) {
        val to = if (leg.counterparty.isEmpty()) "" else BurnerKeys.short(leg.counterparty)
        return if (to.isEmpty()) "−${leg.label}" else "−${leg.label} → $to"
    }
    if (kind == ActivityKind.Receive) return "+${leg.label}"
    return BurnerKeys.short(signature)
}

private fun mintsFor(kind: ActivityKind, legs: List<Leg>): List<String> {
    if (kind == ActivityKind.Send) return uniqueMints(listOfNotNull(legs.firstOrNull { it.outgoing }?.mint))
    if (kind == ActivityKind.Receive) return uniqueMints(listOfNotNull(legs.firstOrNull { !it.outgoing }?.mint))
    return uniqueMints(legs.map { it.mint })
}

private fun legsForOwner(raw: String, owner: String): List<Leg> {
    val tokens = arrayField(raw, "tokenTransfers")
    if (tokens.isNotEmpty()) return transferLegs(tokens, owner, native = false)
    return transferLegs(arrayField(raw, "nativeTransfers"), owner, native = true)
}

private fun transferLegs(items: List<String>, owner: String, native: Boolean): List<Leg> {
    val legs = mutableListOf<Leg>()
    for (item in items) {
        val from = jsonString(item, "fromUserAccount")
        val to = jsonString(item, "toUserAccount")
        val label = if (native) nativeLabel(item) else tokenLabel(item) ?: continue
        if (label == null) continue
        val mint = if (native) SOL_MINT else jsonString(item, "mint").ifEmpty { SOL_MINT }
        if (from == owner) legs.add(Leg(true, label, to, mint))
        else if (to == owner) legs.add(Leg(false, label, from, mint))
    }
    return legs
}

private fun swapSides(raw: String, owner: String): Pair<Side?, Side?> {
    val events = objectField(raw, "events")
    val swap = if (events == null) null else objectField(events, "swap")
    var inn: Side? = null
    var out: Side? = null
    if (swap != null) {
        inn = firstSide(swap, "tokenInputs", owner, native = false) ?: firstSide(swap, "nativeInput", owner, native = true)
        out = firstSide(swap, "tokenOutputs", owner, native = false) ?: firstSide(swap, "nativeOutput", owner, native = true)
    }
    val legs = legsForOwner(raw, owner)
    if (inn == null) {
        val spent = legs.firstOrNull { it.outgoing }
        if (spent != null) inn = Side(spent.label, spent.mint)
    }
    if (out == null) {
        val got = legs.firstOrNull { !it.outgoing }
        if (got != null) out = Side(got.label, got.mint)
    }
    return inn to out
}

private fun firstSide(swap: String, field: String, owner: String, native: Boolean): Side? {
    val items = arrayField(swap, field).ifEmpty {
        val one = objectField(swap, field)
        if (one == null) emptyList() else listOf(one)
    }
    var any: Side? = null
    var matched: Side? = null
    for (item in items) {
        val text = if (native) nativeLabel(item) else tokenLabel(item)
        if (text == null) continue
        val mint = if (native) SOL_MINT else jsonString(item, "mint").ifEmpty { SOL_MINT }
        val side = Side(text, mint)
        if (any == null) any = side
        val who = jsonString(item, "account").ifEmpty { jsonString(item, "userAccount") }
        if (who == owner && matched == null) matched = side
    }
    return matched ?: any
}

private fun tokenLabel(raw: String): String? {
    val boxed = objectField(raw, "rawTokenAmount")
    if (boxed != null) {
        val units = decimalInteger(jsonAmountText(boxed, "tokenAmount") ?: "")
        val decimals = jsonInt(boxed, "decimals")?.toInt()
        if (units != null && decimals != null && decimals in 0..18) {
            val mint = jsonString(raw, "mint").ifEmpty { SOL_MINT }
            return formatUnits(units, decimals) + " " + symbolFor(mint)
        }
    }
    val withoutRaw = if (boxed == null) raw else raw.replace(boxed, "")
    val ui = jsonAmountText(withoutRaw, "tokenAmount") ?: return null
    if (!ui.matches(Regex("-?\\d+(?:\\.\\d+)?"))) return null
    val mint = jsonString(raw, "mint").ifEmpty { SOL_MINT }
    return trimDecimalText(ui) + " " + symbolFor(mint)
}

private fun nativeLabel(raw: String): String? {
    val units = decimalInteger(jsonAmountText(raw, "amount") ?: "") ?: return null
    return formatUnits(units, 9) + " SOL"
}

/** Quoted decimal text or the JSON number's own digits. Never parsed as a binary float. */
private fun jsonAmountText(raw: String, field: String): String? {
    val quoted = jsonString(raw, field)
    if (quoted.isNotEmpty()) return quoted
    return Regex(""""$field"\s*:\s*(-?\d+(?:\.\d+)?)""").find(raw)?.groupValues?.get(1)
}

private fun trimDecimalText(text: String): String {
    if (!text.contains('.')) return text
    return text.trimEnd('0').trimEnd('.')
}

private fun symbolFor(mint: String): String = when (mint) {
    SOL_MINT -> "SOL"
    USDC -> "USDC"
    USDT -> "USDT"
    else -> BurnerKeys.short(mint)
}

private fun uniqueMints(mints: List<String>): List<String> {
    val out = mutableListOf<String>()
    for (mint in mints) {
        if (mint.isEmpty() || out.contains(mint)) continue
        out.add(mint)
        if (out.size >= 2) break
    }
    return out
}

/** Formats a base-unit integer. The amount string must already be a decimal integer. */
fun formatUnits(amount: BigInteger, decimals: Int): String {
    val neg = amount.signum() < 0
    val value = amount.abs()
    val body = if (decimals <= 0) {
        value.toString()
    } else {
        val base = BigInteger.TEN.pow(decimals)
        val whole = value.divide(base)
        val frac = value.remainder(base).toString().padStart(decimals, '0').trimEnd('0')
        if (frac.isEmpty()) whole.toString() else "$whole.$frac"
    }
    return if (neg) "-$body" else body
}

private fun decimalInteger(text: String): BigInteger? {
    if (!text.matches(Regex("-?\\d+"))) return null
    return try {
        BigInteger(text)
    } catch (_: Exception) {
        null
    }
}

private fun jsonString(raw: String, field: String): String {
    val match = Regex(""""$field"\s*:\s*"([^"\\]*)"""").find(raw) ?: return ""
    return match.groupValues[1]
}

private fun jsonInt(raw: String, field: String): Long? {
    val match = Regex(""""$field"\s*:\s*(-?\d+)""").find(raw) ?: return null
    return match.groupValues[1].toLongOrNull()
}

private fun jsonField(raw: String, field: String): String? {
    val match = Regex(""""$field"\s*:\s*([^,}\]]+)""").find(raw) ?: return null
    return match.groupValues[1].trim()
}

private fun objectField(raw: String, field: String): String? {
    val key = "\"$field\""
    val start = raw.indexOf(key)
    if (start < 0) return null
    val brace = raw.indexOf('{', start + key.length)
    if (brace < 0) return null
    return sliceBlock(raw, brace, '{', '}')
}

private fun arrayField(raw: String, field: String): List<String> {
    val key = "\"$field\""
    val start = raw.indexOf(key)
    if (start < 0) return emptyList()
    val bracket = raw.indexOf('[', start + key.length)
    if (bracket < 0) return emptyList()
    val block = sliceBlock(raw, bracket, '[', ']') ?: return emptyList()
    return topLevelObjects(block)
}

private fun topLevelArray(body: String): List<String>? {
    if (!body.startsWith("[")) return null
    return topLevelObjects(body)
}

private fun topLevelObjects(body: String): List<String> {
    val out = mutableListOf<String>()
    var i = 0
    while (i < body.length) {
        if (body[i] == '{') {
            val block = sliceBlock(body, i, '{', '}') ?: break
            out.add(block)
            i += block.length
        } else {
            i++
        }
    }
    return out
}

private fun sliceBlock(body: String, open: Int, left: Char, right: Char): String? {
    var depth = 0
    var quote = false
    var escape = false
    for (i in open until body.length) {
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
        if (ch == left) depth++
        if (ch == right) {
            depth--
            if (depth == 0) return body.substring(open, i + 1)
        }
    }
    return null
}
