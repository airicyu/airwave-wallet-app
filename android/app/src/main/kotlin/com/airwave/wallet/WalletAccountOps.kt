/**
 * Applies multi-account vault edits and refreshes active-account session material on WalletModel.
 * It does not render Compose screens or parse holdings RPC.
 */
package com.airwave.wallet

import android.util.Base64
import com.airwave.wallet.vault.AccountKind
import com.airwave.wallet.vault.AccountRecord
import com.airwave.wallet.vault.Bip39
import com.airwave.wallet.vault.BurnerKeys
import com.airwave.wallet.vault.ImportPathKind
import com.airwave.wallet.vault.VaultAccounts
import java.util.UUID

internal fun WalletModel.activeAccount(): AccountRecord? =
    accountRows.firstOrNull { it.id == settings.activeAccountId }
        ?: accountRows.firstOrNull()

fun WalletModel.setActiveAccountId(id: String) {
    if (accountRows.none { it.id == id }) return
    holdings.clear()
    updateSettings { it.copy(activeAccountId = id) }
    activateAccount(id)
    syncHoldings(refresh = false)
}

fun WalletModel.renameAccount(id: String, label: String) {
    val trimmed = label.trim()
    if (trimmed.isEmpty()) return
    accountRows = accountRows.map { if (it.id == id) it.copy(label = trimmed) else it }
    persistVault()
}

fun WalletModel.removeAccount(id: String): Boolean {
    val row = accountRows.firstOrNull { it.id == id } ?: return false
    val signingLeft = accountRows.filter { it.id != id && it.canSign() }
    if (row.canSign() && signingLeft.isEmpty()) return false
    val next = accountRows.filterNot { it.id == id }
    accountRows = next
    if (settings.activeAccountId == id) {
        val pick = signingLeft.firstOrNull() ?: next.first()
        updateSettings { it.copy(activeAccountId = pick.id) }
        activateAccount(pick.id)
    }
    persistVault()
    syncHoldings(refresh = false)
    return true
}

fun WalletModel.commitBurnerFromPreview() {
    val key = preview ?: return
    val id = UUID.randomUUID().toString()
    val row = AccountRecord(
        id = id,
        kind = AccountKind.Burner,
        label = defaultLabel(AccountKind.Burner),
        address = key.address,
        seedBase64 = Base64.encodeToString(key.seed, Base64.NO_WRAP),
    )
    appendAccount(row, id)
    key.zero()
    preview = null
    route = Route.Home
    syncHoldings(refresh = false)
}

fun WalletModel.commitSeedAccount(mnemonic: String, label: String = "") {
    val seed32 = Bip39.deriveDefaultSeedAccount(mnemonic)
    val address = BurnerKeys.addressOf(seed32)
    seed32.fill(0)
    val id = UUID.randomUUID().toString()
    val row = AccountRecord(
        id = id,
        kind = AccountKind.Seed,
        label = resolvedLabel(label, AccountKind.Seed),
        address = address,
        mnemonic = mnemonic,
        pathKind = ImportPathKind.Phantom,
        pathIndex = 0,
    )
    appendAccount(row, id)
    seedGenerateDraft = null
    syncHoldings(refresh = false)
}

fun WalletModel.commitImportedMnemonic(mnemonic: String, pathKind: ImportPathKind, index: Int, label: String = "") {
    val resources = getApplication<android.app.Application>().resources
    val seed32 = Bip39.deriveSolanaSeed(resources, mnemonic, pathKind, index)
    val address = BurnerKeys.addressOf(seed32)
    seed32.fill(0)
    if (VaultAccounts.hasAddress(accountRows, address)) return
    val id = UUID.randomUUID().toString()
    val row = AccountRecord(
        id = id,
        kind = AccountKind.Seed,
        label = resolvedLabel(label, AccountKind.Seed),
        address = address,
        mnemonic = mnemonic,
        pathKind = pathKind,
        pathIndex = index,
    )
    appendAccount(row, id)
    importMnemonicDraft = null
    syncHoldings(refresh = false)
}

fun WalletModel.commitImportedSecret(seed: ByteArray, label: String = "") {
    val address = BurnerKeys.addressOf(seed)
    if (VaultAccounts.hasAddress(accountRows, address)) {
        seed.fill(0)
        return
    }
    val id = UUID.randomUUID().toString()
    val row = AccountRecord(
        id = id,
        kind = AccountKind.Secret,
        label = resolvedLabel(label, AccountKind.Secret),
        address = address,
        seedBase64 = Base64.encodeToString(seed, Base64.NO_WRAP),
    )
    seed.fill(0)
    appendAccount(row, id)
    importSecretDraft = null
    syncHoldings(refresh = false)
}

fun WalletModel.commitWatchAddress(address: String, label: String = "") {
    if (VaultAccounts.hasAddress(accountRows, address)) return
    val id = UUID.randomUUID().toString()
    val row = AccountRecord(
        id = id,
        kind = AccountKind.Watch,
        label = resolvedLabel(label, AccountKind.Watch),
        address = address,
    )
    appendAccount(row, id)
    watchDraft = null
    syncHoldings(refresh = false)
}

internal fun WalletModel.activateAccount(id: String) {
    val row = accountRows.firstOrNull { it.id == id } ?: return
    signingSeed?.fill(0)
    signingSeed = null
    val resources = getApplication<android.app.Application>().resources
    signingSeed = VaultAccounts.signingSeed(row, resources)?.copyOf()
    address = row.address
}

fun WalletModel.commitPreviewBurner(label: String = "") {
    val key = preview ?: return
    val id = UUID.randomUUID().toString()
    val row = AccountRecord(
        id = id,
        kind = AccountKind.Burner,
        label = resolvedLabel(label, AccountKind.Burner),
        address = key.address,
        seedBase64 = Base64.encodeToString(key.seed, Base64.NO_WRAP),
    )
    appendAccount(row, id)
    key.zero()
    preview = null
    popToHome()
}

internal fun WalletModel.appendAccount(row: AccountRecord, makeActiveId: String) {
    accountRows = accountRows + row
    updateSettings { it.copy(activeAccountId = makeActiveId) }
    activateAccount(makeActiveId)
    persistVault()
}

internal fun WalletModel.persistVault() {
    val chars = passwordChars ?: return
    val bytes = VaultAccounts.toJsonBytes(accountRows)
    store.write(cipher.seal(chars, bytes))
}

internal fun WalletModel.clearSigningMaterial() {
    signingSeed?.fill(0)
    signingSeed = null
    accountRows = emptyList()
    address = null
    seedGenerateDraft = null
    importMnemonicDraft = null
    importSecretDraft = null
    watchDraft = null
}

private fun WalletModel.resolvedLabel(label: String, kind: AccountKind): String {
    val trimmed = label.trim()
    return trimmed.ifEmpty { defaultLabel(kind) }
}

private fun WalletModel.defaultLabel(kind: AccountKind): String {
    val n = accountRows.count { it.kind == kind } + 1
    return when (kind) {
        AccountKind.Seed -> "${t("accounts.kindSeed")} $n"
        AccountKind.Burner -> "${t("accounts.kindBurner")} $n"
        AccountKind.Secret -> "${t("accounts.kindSecret")} $n"
        AccountKind.Watch -> "${t("accounts.kindWatch")} $n"
    }
}
