/**
 * Unlocked session, vault password box, and the in-memory MWA approval.
 * Settings edits live in WalletSettings.kt. Holdings live on HoldingsModel.
 */
package com.airwave.wallet

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.airwave.wallet.settings.AirwaveSettings
import com.airwave.wallet.settings.RpcResolver
import com.airwave.wallet.settings.SettingsStore
import com.airwave.wallet.holdings.HoldingsModel
import com.airwave.wallet.solana.LegacyTransaction
import com.airwave.wallet.solana.SolanaJsonRpc
import com.airwave.wallet.ui.SecretFieldId
import com.airwave.wallet.ui.SecretFieldState
import com.airwave.wallet.vault.AccountKind
import com.airwave.wallet.vault.AccountRecord
import com.airwave.wallet.vault.BurnerKey
import com.airwave.wallet.vault.BurnerKeys
import com.airwave.wallet.vault.ImportPathKind
import com.airwave.wallet.vault.VaultAccounts
import com.airwave.wallet.vault.VaultCipher
import com.airwave.wallet.vault.VaultStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.crypto.AEADBadTagException

class WalletModel(app: Application) : AndroidViewModel(app) {
    internal val store = VaultStore(app)
    internal val cipher = VaultCipher()
    internal val settingsStore = SettingsStore(app)
    internal var passwordChars: CharArray? = null
    internal var signingSeed: ByteArray? = null
    private var lastUserActivityAt = System.currentTimeMillis()
    val holdings = HoldingsModel(viewModelScope)

    init {
        viewModelScope.launch {
            while (isActive) {
                delay(15_000)
                maybeIdleLock()
            }
        }
    }

    var route by mutableStateOf(if (store.exists()) Route.Unlock else Route.Setup)
        internal set
    var error by mutableStateOf<String?>(null)
        private set
    var address by mutableStateOf<String?>(null)
        internal set
    var preview by mutableStateOf<BurnerKey?>(null)
        internal set
    var tab by mutableStateOf(Tab.Assets)
    var pending by mutableStateOf<Pending?>(null)
        private set
    var settings by mutableStateOf(settingsStore.read())
        internal set
    var menuOpen by mutableStateOf(false)
    var homeStack by mutableStateOf<List<HomeDestination>>(emptyList())
        private set
    var approving by mutableStateOf(false)
        private set
    var accountRows by mutableStateOf<List<AccountRecord>>(emptyList())
        internal set

    var seedGenerateDraft by mutableStateOf<String?>(null)
    var importMnemonicDraft by mutableStateOf<String?>(null)
    var importSecretDraft by mutableStateOf<String?>(null)
    var watchDraft by mutableStateOf<String?>(null)
    var importPathKind by mutableStateOf(ImportPathKind.Phantom)
    var importPathIndex by mutableStateOf(0)

    var secretStates by mutableStateOf<Map<SecretFieldId, SecretFieldState>>(emptyMap())
        internal set

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
        if (!applyPlain(plain)) {
            chars.fill(0.toChar())
            passwordChars = null
            error = t("password.wrong")
            route = Route.Unlock
            return
        }
        bumpUserActivity()
        if (pending != null && address != null) route = Route.Approval
        else if (route == Route.Home) syncHoldings(refresh = false)
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
        if (accountRows.isEmpty()) {
            commitBurnerFromPreview()
        } else {
            preview?.let { key ->
                val id = java.util.UUID.randomUUID().toString()
                val row = AccountRecord(
                    id = id,
                    kind = AccountKind.Burner,
                    label = "Burner",
                    address = key.address,
                    seedBase64 = android.util.Base64.encodeToString(key.seed, android.util.Base64.NO_WRAP),
                )
                appendAccount(row, id)
                key.zero()
                preview = null
                popToHome()
            }
        }
    }

    fun lock() {
        menuOpen = false
        homeStack = emptyList()
        failPending()
        clearSigningMaterial()
        preview?.zero()
        preview = null
        passwordChars?.fill(0.toChar())
        passwordChars = null
        error = null
        secretStates = emptyMap()
        holdings.clear()
        route = if (store.exists()) Route.Unlock else Route.Setup
    }

    var mwaError by mutableStateOf<String?>(null)
        private set

    fun offer(request: Pending) {
        if (activeAccount()?.kind == AccountKind.Watch) {
            mwaError = t("mwa.watchAccount")
            request.finish(ApprovalResult.Failed)
            return
        }
        mwaError = null
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
        val secret = signingSeed ?: run {
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
        syncHoldings(refresh = false)
    }

    fun reject() {
        val request = pending
        pending = null
        request?.finish(ApprovalResult.Failed)
        if (address != null) {
            route = Route.Home
            syncHoldings(refresh = false)
        }
    }

    fun push(destination: HomeDestination) {
        menuOpen = false
        homeStack = homeStack + destination
    }

    fun pop() {
        homeStack = if (homeStack.isEmpty()) homeStack else homeStack.dropLast(1)
        if (homeStack.isEmpty()) syncHoldings(refresh = false)
    }

    fun popToHome() {
        homeStack = emptyList()
        syncHoldings(refresh = false)
    }

    fun refreshHoldings() {
        syncHoldings(refresh = true)
    }

    fun skipHoldingsGuide() {
        holdings.skipGuide()
    }

    internal fun syncHoldings(refresh: Boolean) {
        if (address == null) return
        holdings.sync(address, settings, refresh)
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
        val body = if (accountRows.isNotEmpty()) {
            VaultAccounts.toJsonBytes(accountRows)
        } else {
            plain
        }
        store.write(cipher.seal(newChars, body))
        passwordChars?.fill(0.toChar())
        passwordChars = newChars
        return null
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

    private fun applyPlain(plain: ByteArray): Boolean {
        val parsed = VaultAccounts.parse(plain) ?: return false
        if (parsed.accounts.isEmpty()) {
            clearSigningMaterial()
            route = Route.Lead
            return true
        }
        accountRows = parsed.accounts
        var activeId = settings.activeAccountId
        if (parsed.migrated) {
            val migratedId = parsed.accounts.first().id
            if (activeId.isBlank()) {
                activeId = migratedId
                persistSettings(settings.copy(activeAccountId = migratedId))
            }
            persistVault()
        }
        if (activeId.isBlank() || accountRows.none { it.id == activeId }) {
            activeId = parsed.accounts.first().id
            persistSettings(settings.copy(activeAccountId = activeId))
        }
        activateAccount(activeId)
        route = Route.Home
        return true
    }
}

enum class Route { Setup, Unlock, Lead, Generate, Home, Approval }
enum class Tab { Assets, Activity }

enum class HomeDestination {
    Accounts,
    AddAccount,
    ImportChooser,
    AddBurner,
    GenerateSeed,
    ImportMnemonic,
    ImportSecret,
    WatchAddress,
    CombinedLater,
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
