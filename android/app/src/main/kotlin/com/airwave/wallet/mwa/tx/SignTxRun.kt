package com.airwave.wallet.mwa.tx

import com.airwave.wallet.holdings.BaseUnits
import com.airwave.wallet.holdings.HoldingsHttp
import com.airwave.wallet.holdings.JupiterMintMeta
import com.airwave.wallet.settings.AirwaveSettings
import com.airwave.wallet.settings.Cluster
import com.airwave.wallet.settings.RpcResolver
import com.airwave.wallet.vault.BurnerKeys
import java.math.BigInteger
import java.net.URLEncoder

/**
 * Runs one sign-transaction simulation against the wallet cluster and the other of mainnet and devnet.
 * It keeps the working bytes in memory and does not submit.
 */
object SignTxRun {
    fun load(
        original: ByteArray,
        settings: AirwaveSettings,
        signer: String?,
        userLimit: Long?,
        userPrice: Long?,
        userPriority: BigInteger?,
    ): Pair<ByteArray, SignTxUi> {
        val cluster = settings.cluster
        val devnetStrip = cluster == Cluster.Devnet
        val budget = SignTxWire.inspect(original)
        val walletRpc = RpcResolver.endpointForCluster(cluster, settings)
        val other = if (cluster == Cluster.Devnet) Cluster.Mainnet else Cluster.Devnet
        val otherRpc = RpcResolver.endpointForCluster(other, settings)
        val blockhash = budget?.blockhash
        if (budget != null && blockhash != null && walletRpc != null && otherRpc != null) {
            val here = SignTxRpc.blockhashValid(walletRpc, blockhash)
            val there = SignTxRpc.blockhashValid(otherRpc, blockhash)
            if (here == false && there == true) {
                val prepared = SignTxPlan.prepare(original, settings.defaultCuPrice, null, userLimit, userPrice, userPriority)
                val bytes = prepared?.bytes ?: original
                return bytes to mismatch(devnetStrip, cluster, other)
            }
        }
        var measured: Long? = null
        if (budget != null && !budget.signed && budget.version != TxVersion.V1 && budget.cuLimit == null && userLimit == null && walletRpc != null) {
            val probe = SignTxWire.writeBudget(original, SignTxMath.CU_LIMIT_MAX, 0, null)
            if (probe != null) measured = SignTxRpc.simulate(walletRpc, probe)?.unitsConsumed
        }
        val prepared = SignTxPlan.prepare(original, settings.defaultCuPrice, measured, userLimit, userPrice, userPriority)
            ?: return original to failed(devnetStrip, "TX_UNPARSEABLE", emptyList(), null)
        val sim = if (walletRpc == null) null else SignTxRpc.simulate(walletRpc, prepared.bytes)
        val ui = if (sim == null) {
            failed(devnetStrip, "SIM_RPC", prepared)
        } else if (sim.err != null) {
            failed(devnetStrip, sim.err, prepared)
        } else {
            success(devnetStrip, prepared, sim, signer, settings)
        }
        return prepared.bytes to ui
    }

    private fun mismatch(devnetStrip: Boolean, wallet: Cluster, other: Cluster): SignTxUi =
        SignTxUi(
            loading = false,
            mismatch = wallet.name + ":" + other.name,
            devnetStrip = devnetStrip,
            failureTitle = false,
            failureReason = null,
            deltas = emptyList(),
            feeTotal = null,
            signatureFee = null,
            priorityFee = null,
            cuLimit = "",
            secondField = "",
            v1 = false,
            editable = false,
            cuWriteError = false,
            instructions = emptyList(),
        )

    private fun failed(devnetStrip: Boolean, reason: String, prepared: SignTxPrepared): SignTxUi =
        base(devnetStrip, prepared).copy(failureTitle = true, failureReason = reason, deltas = emptyList())

    private fun failed(devnetStrip: Boolean, reason: String, instructions: List<String>, prepared: SignTxPrepared?): SignTxUi =
        SignTxUi(false, null, devnetStrip, true, reason, emptyList(), null, null, null, "", "", false, false, false, instructions)

    private fun success(
        devnetStrip: Boolean,
        prepared: SignTxPrepared,
        sim: TxSimulation,
        signer: String?,
        settings: AirwaveSettings,
    ): SignTxUi {
        val rows = labelTokens(deltas(prepared.bytes, sim, signer), settings)
        return base(devnetStrip, prepared).copy(deltas = rows)
    }

    private fun base(devnetStrip: Boolean, prepared: SignTxPrepared): SignTxUi {
        val priority = prepared.priorityLamports
        val total = if (priority == null) null else prepared.signatureLamports.add(priority)
        val second = if (prepared.version == TxVersion.V1) priority?.toString().orEmpty() else prepared.cuPrice?.toString().orEmpty()
        return SignTxUi(
            loading = false,
            mismatch = null,
            devnetStrip = devnetStrip,
            failureTitle = false,
            failureReason = null,
            deltas = emptyList(),
            feeTotal = total,
            signatureFee = prepared.signatureLamports,
            priorityFee = priority,
            cuLimit = prepared.cuLimit?.toString().orEmpty(),
            secondField = second,
            v1 = prepared.version == TxVersion.V1,
            editable = prepared.editable,
            cuWriteError = prepared.cuWriteError,
            instructions = SignTxWire.inspect(prepared.bytes)?.instructionPrograms ?: emptyList(),
        )
    }

    private fun deltas(bytes: ByteArray, sim: TxSimulation, signer: String?): List<SignTxDelta> {
        val rows = mutableListOf<SignTxDelta>()
        val keys = SignTxWire.inspect(bytes)?.accountKeys.orEmpty()
        val index = if (signer.isNullOrBlank()) 0 else keys.indexOf(signer).let { if (it < 0) 0 else it }
        val pre = sim.preBalances
        val post = sim.postBalances
        if (pre != null && post != null && index < pre.size && index < post.size) {
            val change = post[index].subtract(pre[index])
            if (change.signum() != 0) {
                rows += SignTxDelta("SOL", "", change.abs().toString(), 9, "SOL", change.signum() > 0)
            }
        }
        val ownedPre = sums(sim.preTokens, signer)
        val ownedPost = sums(sim.postTokens, signer)
        (ownedPre.keys + ownedPost.keys).distinct().forEach { mint ->
            val before = ownedPre[mint]
            val after = ownedPost[mint]
            val change = (after?.first ?: BigInteger.ZERO).subtract(before?.first ?: BigInteger.ZERO)
            if (change.signum() == 0) return@forEach
            val decimals = after?.second ?: before?.second ?: 0
            val short = BurnerKeys.short(mint)
            rows += SignTxDelta(short, mint, change.abs().toString(), decimals, short, change.signum() > 0)
        }
        return rows
    }

    private fun sums(rows: List<SimTokenBalance>, signer: String?): Map<String, Pair<BigInteger, Int>> {
        if (signer.isNullOrBlank()) return emptyMap()
        val map = linkedMapOf<String, Pair<BigInteger, Int>>()
        rows.filter { it.owner == signer }.forEach { row ->
            val current = map[row.mint]
            val next = (current?.first ?: BigInteger.ZERO).add(row.amount)
            map[row.mint] = next to row.decimals
        }
        return map
    }

    private fun labelTokens(rows: List<SignTxDelta>, settings: AirwaveSettings): List<SignTxDelta> {
        if (settings.cluster != Cluster.Mainnet) return rows
        val mints = rows.map { it.mint }.filter { it.isNotEmpty() }.distinct()
        if (mints.isEmpty()) return rows
        val hits = jupiter(mints, settings) ?: return rows
        return rows.map { row ->
            if (row.mint.isEmpty()) return@map row
            val hit = hits[row.mint] ?: return@map row
            val short = BurnerKeys.short(row.mint)
            val symbol = hit.symbol.ifEmpty { short }
            val name = if (hit.symbol.isEmpty()) short else "${hit.symbol} · $short"
            val score = hit.organicScore?.takeIf { BaseUnits.scoreAboveZero(it) }
            row.copy(name = name, symbol = symbol, verified = hit.verified, organicScore = score)
        }
    }

    private fun jupiter(mints: List<String>, settings: AirwaveSettings): Map<String, JupiterMintMeta.Hit>? {
        val key = settings.jupiterApiKey.trim()
        val headers = if (key.isEmpty()) emptyMap() else mapOf("x-api-key" to key)
        val query = mints.joinToString(",") { URLEncoder.encode(it, "UTF-8") }
        val body = HoldingsHttp.get("https://api.jup.ag/tokens/v2/search?query=$query", headers) ?: return null
        return JupiterMintMeta.hits(body)
    }
}
