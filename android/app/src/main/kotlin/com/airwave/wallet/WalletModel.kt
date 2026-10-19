package com.airwave.wallet

import android.app.Application
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.airwave.wallet.settings.AirwaveSettings
import com.airwave.wallet.settings.Cluster
import com.airwave.wallet.settings.LocaleTag
import com.airwave.wallet.settings.ModelMode
import com.airwave.wallet.settings.RpcClusterSettings
import com.airwave.wallet.settings.RpcResolver
import com.airwave.wallet.settings.SettingsStore
import com.airwave.wallet.solana.LegacyTransaction
import com.airwave.wallet.solana.SolanaJsonRpc
import com.airwave.wallet.ui.SecretFieldId
import com.airwave.wallet.ui.SecretFieldState
import com.airwave.wallet.vault.BurnerKey
import com.airwave.wallet.vault.BurnerKeys
import com.airwave.wallet.vault.VaultCipher
import com.airwave.wallet.vault.VaultStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.crypto.AEADBadTagException

class WalletModel(app: Application) : AndroidViewModel(app) {
    private val store = VaultStore(app)
    private val cipher = VaultCipher()
    private val settingsStore = SettingsStore(app)
    private var passwordChars: CharArray? = null
    private var seed: ByteArray? = null
    private var lastUserActivityAt = System.currentTimeMillis()

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(15_000)
                maybeIdleLock()
            }
        }
    }

    var route by mutableStateOf(if (store.exists()) Route.Unlock else Route.Setup)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var address by mutableStateOf<String?>(null)
        private set
    var preview by mutableStateOf<BurnerKey?>(null)
        private set
    var tab by mutableStateOf(Tab.Assets)
    var pending by mutableStateOf<Pending?>(null)
        private set
    var settings by mutableStateOf(settingsStore.read())
        private set
    var menuOpen by mutableStateOf(false)
    var homeStack by mutableStateOf<List<HomeDestination>>(emptyList())
        private set
    var approving by mutableStateOf(false)
        private set

    var secretStates by mutableStateOf<Map<SecretFieldId, SecretFieldState>>(emptyMap())
        private set

    fun secretState(id: SecretFieldId): SecretFieldState =
        secretStates[id] ?: SecretFieldState()

    fun t(key: String): String =
        com.airwave.wallet.i18n.AppStrings.t(settings.locale, key)

    fun bumpUserActivity() {
        lastUserActivityAt = System.currentTimeMillis()
    }

    fun setup(password: String, again: String) {
        error = null
        when {
            password.length < 8 -> error = t("password.tooShort")
            password != again -> error = t("password.mismatch")
            else -> {
                remember(password)
                store.write(cipher.seal(passwordChars!!, "{}".encodeToByteArray()))
                route = Route.Lead
            }
        }
    }

    fun unlock(password: String) {
        error = null
        val file = store.read() ?: return
        val chars = password.toCharArray()
        val plain = try {
            cipher.open(chars, file)
        } catch (_: AEADBadTagException) {
            chars.fill(0.toChar())
            error = t("password.wrong")
            return
        }
        remember(chars)
        applyPlain(plain)
        bumpUserActivity()
        if (pending != null && address != null) route = Route.Approval
    }

    fun startGenerate() {
        preview?.zero()
        preview = BurnerKeys.generate()
        route = Route.Generate
    }

    fun roll() {
        preview?.zero()
        preview = BurnerKeys.generate()
    }

    fun commitGenerate() {
        val key = preview ?: return
        val body = JSONObject()
            .put("seed", Base64.encodeToString(key.seed, Base64.NO_WRAP))
            .toString()
        val chars = passwordChars ?: return
        store.write(cipher.seal(chars, body.encodeToByteArray()))
        seed?.fill(0)
        seed = key.seed.copyOf()
        address = key.address
        key.zero()
        preview = null
        route = Route.Home
    }

    fun lock() {
        menuOpen = false
        homeStack = emptyList()
        failPending()
        seed?.fill(0)
        seed = null
        address = null
        passwordChars?.fill(0.toChar())
        passwordChars = null
        preview?.zero()
        preview = null
        error = null
        secretStates = emptyMap()
        route = if (store.exists()) Route.Unlock else Route.Setup
    }

    fun offer(request: Pending) {
        if (pending != null) {
            request.finish(ApprovalResult.Failed)
            return
        }
        if (!store.exists() || (address == null && route != Route.Unlock)) {
            request.finish(ApprovalResult.Failed)
            return
        }
        pending = request
        route = if (address == null) Route.Unlock else Route.Approval
    }

    fun approve() {
        val request = pending ?: return
        val secret = seed ?: run {
            finishApproval(request, ApprovalResult.Failed)
            return
        }
        approving = true
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { request.approve(secret) }
            approving = false
            finishApproval(request, result)
        }
    }

    private fun finishApproval(request: Pending, result: ApprovalResult) {
        pending = null
        route = Route.Home
        request.finish(result)
    }

    fun reject() {
        val request = pending
        pending = null
        request?.finish(ApprovalResult.Failed)
        if (address != null) route = Route.Home
    }

    fun push(destination: HomeDestination) {
        menuOpen = false
        homeStack = homeStack + destination
    }

    fun pop() {
        homeStack = if (homeStack.isEmpty()) homeStack else homeStack.dropLast(1)
    }

    fun updateSettings(transform: (AirwaveSettings) -> AirwaveSettings) {
        settings = transform(settings)
        settingsStore.write(settings)
    }

    fun setCluster(cluster: Cluster) {
        updateSettings { it.copy(cluster = cluster) }
    }

    fun selectRpcEndpoint(cluster: Cluster, url: String) {
        updateSettings {
            val block = it.rpcByCluster[cluster] ?: RpcClusterSettings()
            val active = if (cluster == Cluster.Devnet && url == RpcResolver.PUBLIC_DEVNET) "" else url
            it.copy(rpcByCluster = it.rpcByCluster + (cluster to block.copy(active = active)))
        }
    }

    fun addRpcEndpoint(cluster: Cluster, url: String): Boolean {
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
        return true
    }

    fun removeRpcEndpoint(cluster: Cluster, url: String) {
        updateSettings {
            val block = it.rpcByCluster[cluster] ?: return@updateSettings it
            val urls = block.urls.filterNot { it == url }
            val active = if (block.active == url) "" else block.active
            it.copy(rpcByCluster = it.rpcByCluster + (cluster to block.copy(urls = urls, active = active)))
        }
    }

    fun replaceRpcEndpoint(cluster: Cluster, oldUrl: String, newUrl: String): Boolean {
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
        return true
    }

    fun isRpcEndpointActive(cluster: Cluster, url: String, block: RpcClusterSettings): Boolean {
        if (cluster == Cluster.Devnet && url == RpcResolver.PUBLIC_DEVNET) {
            return block.active.isEmpty() || block.active == RpcResolver.PUBLIC_DEVNET
        }
        return block.active == url
    }

    fun setLocale(locale: LocaleTag) {
        updateSettings { it.copy(locale = locale) }
    }

    fun changePassword(current: String, newPassword: String, confirm: String): String? {
        when {
            newPassword.length < 8 -> return t("password.tooShort")
            newPassword != confirm -> return t("password.mismatch")
        }
        val file = store.read() ?: return t("password.wrong")
        val chars = current.toCharArray()
        val plain = try {
            cipher.open(chars, file)
        } catch (_: AEADBadTagException) {
            chars.fill(0.toChar())
            return t("password.wrong")
        }
        chars.fill(0.toChar())
        val newChars = newPassword.toCharArray()
        store.write(cipher.seal(newChars, plain))
        passwordChars?.fill(0.toChar())
        passwordChars = newChars
        return null
    }

    fun beginSecretEdit(id: SecretFieldId, currentValue: String) {
        secretStates = mapOf(id to SecretFieldState(editing = true, draft = currentValue))
    }

    fun discardSecret(id: SecretFieldId) {
        secretStates = secretStates - id
    }

    fun toggleReveal(id: SecretFieldId) {
        val state = secretStates[id] ?: SecretFieldState()
        secretStates = secretStates + (id to state.copy(revealed = !state.revealed))
    }

    fun updateSecretDraft(id: SecretFieldId, draft: String) {
        val state = secretStates[id] ?: SecretFieldState(editing = true)
        secretStates = secretStates + (id to state.copy(draft = draft, editing = true))
    }

    fun confirmSecret(id: SecretFieldId, onValue: (String) -> Unit) {
        val state = secretStates[id] ?: return
        onValue(state.draft.trim())
        secretStates = secretStates - id
    }

    fun rpcForSubmit(chain: String?): String? =
        RpcResolver.rpcUrlForSubmit(chain, settings)

    fun signAndSend(payload: ByteArray, rpcUrl: String, secret: ByteArray): ApprovalResult {
        val signed = LegacyTransaction.sign(payload, secret) ?: return ApprovalResult.Failed
        val signatureB64 = SolanaJsonRpc.sendTransaction(rpcUrl, signed) ?: return ApprovalResult.Failed
        return try {
            val sigBytes = BurnerKeys.base58Decode(signatureB64)
            ApprovalResult.Signatures(arrayOf(sigBytes))
        } catch (_: Exception) {
            ApprovalResult.Failed
        }
    }

    private fun failPending() {
        val request = pending
        pending = null
        request?.finish(ApprovalResult.Failed)
    }

    private fun remember(password: String) {
        passwordChars?.fill(0.toChar())
        passwordChars = password.toCharArray()
    }

    private fun remember(chars: CharArray) {
        passwordChars?.fill(0.toChar())
        passwordChars = chars
    }

    private fun maybeIdleLock() {
        if (!settings.idleAutoLockEnabled) return
        if (passwordChars == null) return
        if (route != Route.Home && route != Route.Approval) return
        val timeoutMs = settings.idleAutoLockMinutes.toLong() * 60_000L
        if (System.currentTimeMillis() - lastUserActivityAt >= timeoutMs) {
            lock()
        }
    }

    private fun applyPlain(plain: ByteArray) {
        val json = JSONObject(plain.decodeToString())
        if (json.has("seed")) {
            seed?.fill(0)
            seed = Base64.decode(json.getString("seed"), Base64.NO_WRAP)
            address = BurnerKeys.addressOf(seed!!)
            route = Route.Home
        } else {
            seed?.fill(0)
            seed = null
            address = null
            route = Route.Lead
        }
    }
}

enum class Route { Setup, Unlock, Lead, Generate, Home, Approval }
enum class Tab { Assets, Activity }

enum class HomeDestination {
    EmptyAccounts,
    EmptyAddressBook,
    EmptyConnected,
    EmptyAbout,
    Network,
    SettingsHub,
    Language,
    Rpc,
    ApiKeys,
    AiSettings,
    CuPrice,
    WalletLockSettings,
}

sealed class ApprovalResult {
    data class Bytes(val value: ByteArray) : ApprovalResult()
    data class Signatures(val value: Array<ByteArray>) : ApprovalResult()
    data object Failed : ApprovalResult()
}

data class Pending(
    val title: String,
    val lines: List<Pair<String, String>>,
    val approve: (ByteArray) -> ApprovalResult,
    val finish: (ApprovalResult) -> Unit,
)
