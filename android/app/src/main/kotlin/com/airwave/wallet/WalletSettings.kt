/**
 * Settings and secret-field edits on WalletModel.
 * Does not change the vault password box or hold signing key material.
 */
package com.airwave.wallet

import com.airwave.wallet.settings.AirwaveSettings
import com.airwave.wallet.settings.Cluster
import com.airwave.wallet.settings.LocaleTag
import com.airwave.wallet.settings.RpcClusterSettings
import com.airwave.wallet.settings.RpcResolver
import com.airwave.wallet.ui.SecretFieldId
import com.airwave.wallet.ui.SecretFieldState

internal fun WalletModel.persistSettings(next: AirwaveSettings) {
    settings = next
    settingsStore.write(next)
}

fun WalletModel.updateSettings(transform: (AirwaveSettings) -> AirwaveSettings) {
    persistSettings(transform(settings))
}

fun WalletModel.setCluster(cluster: Cluster) {
    updateSettings { it.copy(cluster = cluster) }
    syncHoldings(refresh = false)
}

fun WalletModel.selectRpcEndpoint(cluster: Cluster, url: String) {
    updateSettings {
        val block = it.rpcByCluster[cluster] ?: RpcClusterSettings()
        val active = if (cluster == Cluster.Devnet && url == RpcResolver.PUBLIC_DEVNET) "" else url
        it.copy(rpcByCluster = it.rpcByCluster + (cluster to block.copy(active = active)))
    }
    syncHoldings(refresh = false)
}

fun WalletModel.addRpcEndpoint(cluster: Cluster, url: String): Boolean {
    val trimmed = url.trim()
    if (!RpcResolver.isAllowedCustomUrl(trimmed)) return false
    if (cluster == Cluster.Devnet && trimmed == RpcResolver.PUBLIC_DEVNET) return false
    updateSettings {
        val block = it.rpcByCluster[cluster] ?: RpcClusterSettings()
        if (trimmed in block.urls) return@updateSettings it
        it.copy(
            rpcByCluster = it.rpcByCluster + (
                cluster to block.copy(urls = block.urls + trimmed, active = trimmed)
            ),
        )
    }
    syncHoldings(refresh = false)
    return true
}

fun WalletModel.removeRpcEndpoint(cluster: Cluster, url: String) {
    updateSettings {
        val block = it.rpcByCluster[cluster] ?: return@updateSettings it
        val urls = block.urls.filterNot { it == url }
        val active = if (block.active == url) "" else block.active
        it.copy(rpcByCluster = it.rpcByCluster + (cluster to block.copy(urls = urls, active = active)))
    }
    syncHoldings(refresh = false)
}

fun WalletModel.replaceRpcEndpoint(cluster: Cluster, oldUrl: String, newUrl: String): Boolean {
    val trimmed = newUrl.trim()
    if (!RpcResolver.isAllowedCustomUrl(trimmed)) return false
    if (cluster == Cluster.Devnet && trimmed == RpcResolver.PUBLIC_DEVNET) return false
    updateSettings {
        val block = it.rpcByCluster[cluster] ?: return@updateSettings it
        if (oldUrl !in block.urls) return@updateSettings it
        val urls = block.urls.map { if (it == oldUrl) trimmed else it }.distinct()
        val active = when (block.active) {
            oldUrl -> trimmed
            "" -> block.active
            else -> block.active
        }
        it.copy(rpcByCluster = it.rpcByCluster + (cluster to block.copy(urls = urls, active = active)))
    }
    syncHoldings(refresh = false)
    return true
}

fun WalletModel.isRpcEndpointActive(cluster: Cluster, url: String, block: RpcClusterSettings): Boolean {
    if (cluster == Cluster.Devnet && url == RpcResolver.PUBLIC_DEVNET) {
        return block.active.isEmpty() || block.active == RpcResolver.PUBLIC_DEVNET
    }
    return block.active == url
}

fun WalletModel.setLocale(locale: LocaleTag) {
    updateSettings { it.copy(locale = locale) }
}

fun WalletModel.beginSecretEdit(id: SecretFieldId, currentValue: String) {
    secretStates = mapOf(id to SecretFieldState(editing = true, draft = currentValue))
}

fun WalletModel.discardSecret(id: SecretFieldId) {
    secretStates = secretStates - id
}

fun WalletModel.toggleReveal(id: SecretFieldId) {
    val state = secretStates[id] ?: SecretFieldState()
    secretStates = secretStates + (id to state.copy(revealed = !state.revealed))
}

fun WalletModel.updateSecretDraft(id: SecretFieldId, draft: String) {
    val state = secretStates[id] ?: SecretFieldState(editing = true)
    secretStates = secretStates + (id to state.copy(draft = draft, editing = true))
}

fun WalletModel.confirmSecret(id: SecretFieldId, onValue: (String) -> Unit) {
    val state = secretStates[id] ?: return
    onValue(state.draft.trim())
    secretStates = secretStates - id
}
