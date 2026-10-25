package com.airwave.wallet.settings

import org.json.JSONArray
import org.json.JSONObject
import java.math.BigInteger

enum class Cluster(val wire: String) {
    Devnet("devnet"),
    Mainnet("mainnet"),
    ;

    companion object {
        fun fromWire(value: String?): Cluster =
            entries.firstOrNull { it.wire == value } ?: Devnet
    }
}

enum class LocaleTag(val wire: String) {
    En("en"),
    ZhHant("zh-Hant"),
    ZhHans("zh-Hans"),
    ;

    companion object {
        fun fromWire(value: String?): LocaleTag =
            entries.firstOrNull { it.wire == value } ?: En
    }
}

enum class ModelMode(val wire: String) {
    Default("default"),
    Custom("custom"),
    ;

    companion object {
        fun fromWire(value: String?): ModelMode =
            entries.firstOrNull { it.wire == value } ?: Default
    }
}

data class RpcClusterSettings(
    val urls: List<String> = emptyList(),
    val active: String = "",
)

data class AirwaveSettings(
    val cluster: Cluster = Cluster.Devnet,
    val rpcByCluster: Map<Cluster, RpcClusterSettings> = defaultRpcByCluster(),
    val heliusApiUrl: String = "",
    val jupiterApiKey: String = "",
    val openRouterApiKey: String = "",
    val txAiAnalysisEnabled: Boolean = true,
    val openRouterReportModelMode: ModelMode = ModelMode.Default,
    val openRouterReportModelChain: List<String> = emptyList(),
    val openRouterDecisionsModelMode: ModelMode = ModelMode.Default,
    val openRouterDecisionsModelChain: List<String> = emptyList(),
    val defaultCuPrice: Long = 25_000L,
    val locale: LocaleTag = LocaleTag.En,
    val idleAutoLockEnabled: Boolean = true,
    val idleAutoLockMinutes: Int = 10,
    val activeAccountId: String = "",
) {
    val heliusConfigured: Boolean
        get() = RpcResolver.isAllowedCustomUrl(heliusApiUrl.trim())

    val jupiterConfigured: Boolean
        get() = jupiterApiKey.trim().isNotEmpty()

    val openRouterConfigured: Boolean
        get() = openRouterApiKey.trim().isNotEmpty()

    fun keysSummary(): KeysSummary {
        val count = listOf(heliusConfigured, jupiterConfigured).count { it }
        return when (count) {
            2 -> KeysSummary.Configured
            1 -> KeysSummary.Partial
            else -> KeysSummary.Unset
        }
    }

    fun toJson(): JSONObject {
        val rpc = JSONObject()
        Cluster.entries.forEach { cluster ->
            val entry = rpcByCluster[cluster] ?: RpcClusterSettings()
            val urlArr = JSONArray()
            entry.urls.forEach { urlArr.put(it) }
            rpc.put(
                cluster.wire,
                JSONObject()
                    .put("urls", urlArr)
                    .put("active", entry.active),
            )
        }
        return JSONObject()
            .put("cluster", cluster.wire)
            .put("rpcByCluster", rpc)
            .put("heliusApiUrl", heliusApiUrl)
            .put("jupiterApiKey", jupiterApiKey)
            .put("openRouterApiKey", openRouterApiKey)
            .put("txAiAnalysisEnabled", txAiAnalysisEnabled)
            .put("openRouterReportModelMode", openRouterReportModelMode.wire)
            .put("openRouterReportModelChain", stringArray(openRouterReportModelChain))
            .put("openRouterDecisionsModelMode", openRouterDecisionsModelMode.wire)
            .put("openRouterDecisionsModelChain", stringArray(openRouterDecisionsModelChain))
            .put("defaultCuPrice", defaultCuPrice)
            .put("locale", locale.wire)
            .put("idleAutoLockEnabled", idleAutoLockEnabled)
            .put("idleAutoLockMinutes", idleAutoLockMinutes)
            .put("activeAccountId", activeAccountId)
    }

    companion object {
        const val MASKED_SECRET_DISPLAY = "••••••"

        val DEFAULT_REPORT_MODELS = listOf(
            "inclusionai/ling-3.0-flash",
            "nvidia/nemotron-3.5-lightning",
            "~deepseek/deepseek-flash-latest",
        )

        val DEFAULT_DECISIONS_MODELS = listOf(
            "liquid/d1",
            "nace-ai/drex-v1.5",
        )

        fun defaultRpcByCluster(): Map<Cluster, RpcClusterSettings> =
            Cluster.entries.associateWith { RpcClusterSettings() }

        fun fromJson(text: String): AirwaveSettings {
            if (text.isBlank()) return AirwaveSettings()
            val json = JSONObject(text)
            val rpcByCluster = mutableMapOf<Cluster, RpcClusterSettings>()
            val rpcRoot = json.optJSONObject("rpcByCluster") ?: JSONObject()
            Cluster.entries.forEach { cluster ->
                val block = rpcRoot.optJSONObject(cluster.wire)
                if (block != null) {
                    val urls = mutableListOf<String>()
                    val arr = block.optJSONArray("urls")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            urls += arr.getString(i)
                        }
                    }
                    rpcByCluster[cluster] = RpcClusterSettings(urls, block.optString("active", ""))
                } else {
                    rpcByCluster[cluster] = RpcClusterSettings()
                }
            }
            val cu = json.optLong("defaultCuPrice", 25_000L).coerceIn(0L, 1_000_000_000L)
            val idleMinutes = json.optInt("idleAutoLockMinutes", 10).coerceIn(1, 1440)
            return AirwaveSettings(
                cluster = Cluster.fromWire(json.optString("cluster", "devnet")),
                rpcByCluster = rpcByCluster,
                heliusApiUrl = json.optString("heliusApiUrl", ""),
                jupiterApiKey = json.optString("jupiterApiKey", ""),
                openRouterApiKey = json.optString("openRouterApiKey", ""),
                txAiAnalysisEnabled = json.optBoolean("txAiAnalysisEnabled", true),
                openRouterReportModelMode = ModelMode.fromWire(json.optString("openRouterReportModelMode")),
                openRouterReportModelChain = json.optStringArray("openRouterReportModelChain"),
                openRouterDecisionsModelMode = ModelMode.fromWire(json.optString("openRouterDecisionsModelMode")),
                openRouterDecisionsModelChain = json.optStringArray("openRouterDecisionsModelChain"),
                defaultCuPrice = cu,
                locale = LocaleTag.fromWire(json.optString("locale", "en")),
                idleAutoLockEnabled = json.optBoolean("idleAutoLockEnabled", true),
                idleAutoLockMinutes = idleMinutes,
                activeAccountId = json.optString("activeAccountId", ""),
            )
        }

        fun parseIdleLockMinutes(text: String, lastSaved: Int): Int {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return lastSaved
            return try {
                trimmed.toInt().coerceIn(1, 1440)
            } catch (_: Exception) {
                lastSaved
            }
        }

        fun parseCuPriceInput(text: String, lastSaved: Long): Long {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) return lastSaved
            return try {
                val value = BigInteger(trimmed)
                if (value.signum() < 0) lastSaved
                else value.min(BigInteger.valueOf(1_000_000_000L)).longValueExact()
            } catch (_: Exception) {
                lastSaved
            }
        }

        fun normalizeModelChain(slots: List<String>): List<String> =
            slots.map { it.trim() }.filter { it.isNotEmpty() }.take(5)

        private fun stringArray(items: List<String>): JSONArray {
            val arr = JSONArray()
            items.forEach { arr.put(it) }
            return arr
        }

        private fun JSONObject.optStringArray(key: String): List<String> {
            val arr = optJSONArray(key) ?: return emptyList()
            return buildList {
                for (i in 0 until arr.length()) add(arr.getString(i))
            }
        }
    }
}

enum class KeysSummary { Configured, Partial, Unset }
