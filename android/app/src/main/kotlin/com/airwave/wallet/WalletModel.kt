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

/**
 * In-memory wallet session for this process: the unlocked vault, the password box, the shell route, and at most one MWA approval.
 * Compose screens read this object. VaultStore is the durable file; settings edits live in WalletSettings.kt; holdings live on HoldingsModel. The pending approval is never written to disk. If the process dies, the user unlocks again.
 */
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
    var approvalSettled by mutableStateOf(false)
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
    private var afterUnlock = mutableListOf<(Boolean) -> Unit>()

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
        flushAfterUnlock()
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

    fun canSignNow(): Boolean = address != null && activeAccount()?.canSign() == true

    fun whenSigningReady(onReady: (Boolean) -> Unit) {
        if (canSignNow()) {
            onReady(true)
            return
        }
        if (!store.exists() || (address == null && route != Route.Unlock)) {
            onReady(false)
            return
        }
        afterUnlock.add(onReady)
    }

    fun lock() {
        menuOpen = false
        homeStack = emptyList()
        failPending()
        val waiting = afterUnlock.toList()
        afterUnlock.clear()
        waiting.forEach { it(false) }
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
        if (approvalSettled) dropSessionChrome()
        val watch = activeAccount()?.kind == AccountKind.Watch
        if (com.airwave.wallet.mwa.MwaPayloads.failBeforeOffer(pending != null, watch)) {
            if (watch) mwaError = t("mwa.watchAccount")
            request.finish(ApprovalResult.Rejected)
            return
        }
        mwaError = null
        if (!store.exists() || (address == null && route != Route.Unlock)) {
            request.finish(ApprovalResult.Rejected)
            return
        }
        pending = request
        approvalSettled = false
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
        approvalSettled = true
        request.finish(result)
        if (address != null) route = Route.Home
        syncHoldings(refresh = false)
    }

    fun reject() {
        if (approvalSettled) {
            dropSessionChrome()
            if (address != null) route = Route.Home
            return
        }
        val request = pending
        pending = null
        request?.finish(ApprovalResult.Rejected)
        if (address != null) {
            route = Route.Home
            syncHoldings(refresh = false)
        }
    }

    fun endAssociation() {
        if (approvalSettled) {
            dropSessionChrome()
            return
        }
        val request = pending
        pending = null
        approving = false
        request?.finish(ApprovalResult.Rejected)
    }

    fun dropSessionChrome() {
        pending = null
        approvalSettled = false
        approving = false
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

    private fun flushAfterUnlock() {
        val waiting = afterUnlock.toList()
        afterUnlock.clear()
        val ok = canSignNow()
        waiting.forEach { it(ok) }
    }

    private fun failPending() {
        if (approvalSettled) {
            dropSessionChrome()
            return
        }
        val request = pending
        pending = null
        request?.finish(ApprovalResult.Rejected)
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

/** Names the shell screen WalletApp should show. */
enum class Route { Setup, Unlock, Lead, Generate, Home, Approval }

/** Names the selected Home bottom tab. */
enum class Tab { Assets, Activity }

/** Names a screen pushed on the home stack from the hamburger menu. */
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

/** Carries a result that goes back only to the MWA request that created the pending approval. */
sealed class ApprovalResult {
    /** Returns the public key or message signature bytes for that originating request. */
    data class Bytes(val value: ByteArray) : ApprovalResult()
    /** Returns the transaction signatures after a successful sign-and-send. */
    data class Signatures(val value: Array<ByteArray>) : ApprovalResult()
    /** The user declined, locked, or the sign/send path failed. */
    /** The user declined or locked, or the request was refused before approval chrome. */
    data object Rejected : ApprovalResult()
    /** Signing or submitting failed after the user approved. */
    data object Failed : ApprovalResult()
}

/** Names the approval chrome MwaScreens should draw for the current pending request. */
enum class PendingKind { Authorize, SignMessage, SignAndSend }

/**
 * One in-memory MWA approval waiting for the user.
 * It is not written to disk. A second offer fails this value in place instead of replacing it, so the originating request still gets a result.
 */
data class Pending(
    val kind: PendingKind,
    val identityName: String = "",
    val identityUri: String = "",
    val chain: String = "",
    val accountLabel: String = "",
    val accountShort: String = "",
    val messageUtf8: String? = null,
    val messageHex: String? = null,
    val amountLamports: java.math.BigInteger? = null,
    val feeLamports: java.math.BigInteger? = null,
    val instructionNames: List<String> = emptyList(),
    val approve: (ByteArray) -> ApprovalResult,
    val finish: (ApprovalResult) -> Unit,
)
