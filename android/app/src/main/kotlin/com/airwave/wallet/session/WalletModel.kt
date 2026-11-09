package com.airwave.wallet.session

import android.app.Application
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.airwave.wallet.activateAccount
import com.airwave.wallet.activeAccount
import com.airwave.wallet.appendAccount
import com.airwave.wallet.clearSigningMaterial
import com.airwave.wallet.commitBurnerFromPreview
import com.airwave.wallet.holdings.HoldingsModel
import com.airwave.wallet.persistSettings
import com.airwave.wallet.persistVault
import com.airwave.wallet.settings.SettingsStore
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import javax.crypto.AEADBadTagException

/**
 * In-memory wallet session for this process: the unlocked vault, the password box, and the shell route.
 * Compose screens read this object. The MWA approval transitions live in WalletApproval.kt and are not written to disk. If the process dies, the user unlocks again.
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
    var walletOperationRequest by mutableStateOf<WalletOperationRequest?>(null)
        internal set
    var settings by mutableStateOf(settingsStore.read())
        internal set
    var menuOpen by mutableStateOf(false)
    var homeStack by mutableStateOf<List<HomeDestination>>(emptyList())
        private set
    var approving by mutableStateOf(false)
        internal set
    var approvalSettled by mutableStateOf(false)
        internal set
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

    var mwaError by mutableStateOf<String?>(null)
        internal set

    /** Host hook for a sign request that is waiting until unlock. Not a second walletOperationRequest store. */
    internal var onMwaUnlocked: (() -> Unit)? = null

    /** Host hook so lock declines a sign request that has not been offered yet. */
    internal var onMwaInterrupted: (() -> Unit)? = null

    /** Host hook when decline fails because the association is already gone. */
    internal var onMwaDeclineStale: (() -> Unit)? = null

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
        onMwaUnlocked?.invoke()
        if (route == Route.Home) syncHoldings(refresh = false)
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
                val id = UUID.randomUUID().toString()
                val row = AccountRecord(
                    id = id,
                    kind = AccountKind.Burner,
                    label = "Burner",
                    address = key.address,
                    seedBase64 = Base64.encodeToString(key.seed, Base64.NO_WRAP),
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
        onMwaInterrupted?.invoke()
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

    private fun flushAfterUnlock() {
        val waiting = afterUnlock.toList()
        afterUnlock.clear()
        val ok = canSignNow()
        waiting.forEach { it(ok) }
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
