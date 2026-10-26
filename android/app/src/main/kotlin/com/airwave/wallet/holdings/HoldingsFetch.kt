package com.airwave.wallet.holdings

import com.airwave.wallet.settings.AirwaveSettings
import com.airwave.wallet.settings.Cluster
import com.airwave.wallet.settings.RpcResolver
import java.math.BigInteger
import java.net.URLEncoder

/**
 * Runs one holdings round against RPC, and against Helius or Jupiter when the contract says so.
 * It does not keep the snapshot or the round id.
 */
object HoldingsFetch {
    fun round(address: String, settings: AirwaveSettings, endpoint: String): HoldingsSnapshot {
        val helius = HeliusHoldings.useWalletApi(settings.cluster, settings.heliusApiUrl)
        val loaded = if (helius) heliusRound(address, settings, endpoint) else rpcRound(address, endpoint)
        val rows = loaded ?: return HoldingsSnapshot(phase = HoldingPhase.Failed, error = "failed")
        val named = rows.map { JupiterMintMeta.fallback(it) }
        if (settings.cluster != Cluster.Mainnet) {
            return HoldingsSnapshot(rows = HoldingSort.ready(named, byUsd = false), phase = HoldingPhase.Ready)
        }
        val patched = jupiter(named, settings)
        val byUsd = patched != null
        val finalRows = (patched ?: named).map { JupiterMintMeta.fallback(it) }
        return HoldingsSnapshot(rows = HoldingSort.ready(finalRows, byUsd), phase = HoldingPhase.Ready)
    }

    private fun rpcRound(address: String, endpoint: String): List<HoldingRow>? {
        val lamports = lamports(endpoint, address) ?: return null
        val legacy = program(endpoint, address, HoldingIds.TOKEN_PROGRAM, TokenProgramKind.SplToken) ?: return null
        val token2022 = program(endpoint, address, HoldingIds.TOKEN_2022, TokenProgramKind.Token2022) ?: return null
        return RpcHoldings.rowsFromRpc(lamports, legacy + token2022)
    }

    private fun heliusRound(address: String, settings: AirwaveSettings, endpoint: String): List<HoldingRow>? {
        val lamports = lamports(endpoint, address) ?: return null
        val wrappedBody = HoldingsHttp.rpc(endpoint, "getTokenAccountsByOwner", RpcHoldings.wrappedParams(address))
            ?: return null
        val wrapped = RpcHoldings.tokenAccounts(wrappedBody, TokenProgramKind.SplToken) ?: return null
        val spl = walletRows(address, settings) ?: return null
        return RpcHoldings.rowsFromRpc(lamports, wrapped + spl.map {
            RpcHoldings.ParsedAccount(it.id, it.raw, it.decimals, it.tokenProgram ?: TokenProgramKind.SplToken)
        })
    }

    private fun walletRows(address: String, settings: AirwaveSettings): List<HoldingRow>? {
        val key = HeliusHoldings.apiKey(settings.heliusApiUrl) ?: return null
        val rows = mutableListOf<HoldingRow>()
        for (page in 1..20) {
            if (page > 1) Thread.sleep(500)
            val url = "https://api.helius.xyz/v1/wallet/$address/balances?api-key=${URLEncoder.encode(key, "UTF-8")}" +
                "&page=$page&limit=100&showNfts=false&showZeroBalance=false&showNative=false"
            val body = HoldingsHttp.get(url, mapOf("X-Api-Key" to key)) ?: return null
            val parsed = HeliusHoldings.page(body) ?: return null
            rows.addAll(parsed.rows)
            if (!parsed.hasMore || !parsed.full) return rows
            if (page == 20) return null
        }
        return rows
    }

    private fun lamports(endpoint: String, address: String): BigInteger? {
        val body = HoldingsHttp.rpc(endpoint, "getBalance", RpcHoldings.balanceParams(address)) ?: return null
        return RpcHoldings.lamports(body)
    }

    private fun program(
        endpoint: String,
        address: String,
        programId: String,
        kind: TokenProgramKind,
    ): List<RpcHoldings.ParsedAccount>? {
        val body = HoldingsHttp.rpc(endpoint, "getTokenAccountsByOwner", RpcHoldings.programParams(address, programId))
            ?: return null
        return RpcHoldings.tokenAccounts(body, kind)
    }

    private fun jupiter(rows: List<HoldingRow>, settings: AirwaveSettings): List<HoldingRow>? {
        val mints = rows.map { if (it.id == HoldingIds.NATIVE) HoldingIds.WRAPPED else it.id }.distinct()
        if (mints.isEmpty()) return rows
        val hits = linkedMapOf<String, JupiterMintMeta.Hit>()
        val key = settings.jupiterApiKey.trim()
        val headers = if (key.isEmpty()) emptyMap() else mapOf("x-api-key" to key)
        mints.chunked(100).forEachIndexed { index, batch ->
            if (index > 0) Thread.sleep(if (key.isEmpty()) 2000 else 1000)
            val query = batch.joinToString(",") { URLEncoder.encode(it, "UTF-8") }
            val body = HoldingsHttp.get("https://api.jup.ag/tokens/v2/search?query=$query", headers) ?: return null
            hits.putAll(JupiterMintMeta.hits(body))
        }
        return JupiterMintMeta.apply(rows, hits)
    }
}
