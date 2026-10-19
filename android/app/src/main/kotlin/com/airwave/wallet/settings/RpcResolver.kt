package com.airwave.wallet.settings

import java.net.URI

object RpcResolver {
    const val PUBLIC_DEVNET = "https://api.devnet.solana.com"
    const val PUBLIC_MAINNET = "https://api.mainnet-beta.solana.com"
    const val PUBLIC_TESTNET = "https://api.testnet.solana.com"

    enum class SubmitCluster { Devnet, Mainnet, Testnet, Unknown }

    fun mapRequestChain(chain: String?): SubmitCluster = when (chain) {
        "solana:mainnet", "mainnet-beta" -> SubmitCluster.Mainnet
        "solana:devnet", "devnet" -> SubmitCluster.Devnet
        "solana:testnet", "testnet" -> SubmitCluster.Testnet
        else -> SubmitCluster.Unknown
    }

    fun rpcUrlForSubmit(chain: String?, settings: AirwaveSettings): String? {
        return when (mapRequestChain(chain)) {
            SubmitCluster.Unknown -> null
            SubmitCluster.Testnet -> PUBLIC_TESTNET
            SubmitCluster.Devnet -> endpointForCluster(Cluster.Devnet, settings)
            SubmitCluster.Mainnet -> endpointForCluster(Cluster.Mainnet, settings)
        }
    }

    fun summaryForCluster(cluster: Cluster, settings: AirwaveSettings): String {
        val active = settings.rpcByCluster[cluster]?.active?.trim().orEmpty()
        return when (cluster) {
            Cluster.Devnet -> {
                val url = if (active.isNotEmpty() && isAllowedCustomUrl(active)) active else PUBLIC_DEVNET
                shorten(url)
            }
            Cluster.Mainnet -> {
                if (active.isEmpty() || !isAllowedCustomUrl(active)) return ""
                shorten(active)
            }
        }
    }

    fun endpointForCluster(cluster: Cluster, settings: AirwaveSettings): String? {
        val active = settings.rpcByCluster[cluster]?.active?.trim().orEmpty()
        return when (cluster) {
            Cluster.Devnet -> {
                if (active.isNotEmpty() && isAllowedCustomUrl(active)) active else PUBLIC_DEVNET
            }
            Cluster.Mainnet -> {
                if (active.isEmpty() || !isAllowedCustomUrl(active)) null else active
            }
        }
    }

    fun isAllowedCustomUrl(raw: String): Boolean {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return false
        return try {
            val uri = URI(trimmed)
            if (!uri.userInfo.isNullOrEmpty()) return false
            when (uri.scheme?.lowercase()) {
                "https" -> true
                "http" -> {
                    val host = uri.host?.lowercase() ?: return false
                    host == "localhost" || host == "127.0.0.1"
                }
                else -> false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun shorten(url: String): String {
        if (url.length <= 40) return url
        return url.take(24) + "…" + url.takeLast(10)
    }
}
