/**
 * Creates and edits a combined wallet account on the in-memory vault list.
 * It does not draw screens or sign.
 */
package com.airwave.wallet.account
import com.airwave.wallet.shell.HomeDestination

import com.airwave.wallet.session.WalletModel
import com.airwave.wallet.vault.AccountKind
import com.airwave.wallet.vault.AccountRecord
import com.airwave.wallet.vault.BurnerKeys
import java.util.UUID

/** In-memory create draft. Process death drops it. The member sheet does not use this object. */
data class CombinedDraft(
    val label: String = "",
    val members: List<String> = emptyList(),
    val current: String = "",
)

fun parseCombinedPaste(text: String, existing: List<String>): List<String> {
    val seen = existing.toMutableSet()
    val out = mutableListOf<String>()
    text.split(Regex("[\\s,]+")).forEach { part ->
        val key = canonicalPubkey(part.trim()) ?: return@forEach
        if (!seen.add(key)) return@forEach
        out += key
    }
    return out
}

fun canonicalPubkey(text: String): String? {
    if (text.isEmpty()) return null
    val bytes = try {
        BurnerKeys.base58Decode(text)
    } catch (_: IllegalArgumentException) {
        return null
    }
    if (bytes.size != 32) return null
    return BurnerKeys.base58(bytes)
}

fun WalletModel.beginCombinedCreate() {
    combinedDraft = CombinedDraft()
    combinedEditId = null
}

fun WalletModel.commitCombinedCreate() {
    val draft = combinedDraft ?: return
    if (draft.members.isEmpty() || draft.current !in draft.members) return
    val id = UUID.randomUUID().toString()
    val label = draft.label.trim().ifEmpty { "Combined" }
    appendAccount(
        AccountRecord(
            id = id,
            kind = AccountKind.Combined,
            label = label,
            address = draft.current,
            members = draft.members,
        ),
        id,
    )
    combinedDraft = null
    popToHome()
    push(HomeDestination.Accounts)
}

fun WalletModel.appendCombinedMembers(incoming: List<String>) {
    if (incoming.isEmpty()) return
    val edit = combinedEditId
    if (edit == null) {
        val draft = combinedDraft ?: return
        val members = draft.members + incoming
        val current = draft.current.ifEmpty { members.first() }
        combinedDraft = draft.copy(members = members, current = current)
        return
    }
    val row = accountRows.firstOrNull { it.id == edit && it.kind == AccountKind.Combined } ?: return
    val members = row.members + incoming.filter { it !in row.members }
    writeCombined(row.copy(members = members, address = row.address.ifEmpty { members.first() }))
}

fun WalletModel.setCombinedCurrent(address: String) {
    val edit = combinedEditId
    if (edit == null) {
        val draft = combinedDraft ?: return
        if (address !in draft.members) return
        combinedDraft = draft.copy(current = address)
        return
    }
    val row = accountRows.firstOrNull { it.id == edit } ?: return
    if (address !in row.members) return
    writeCombined(row.copy(address = address))
}

fun WalletModel.removeCombinedMember(address: String) {
    val edit = combinedEditId
    if (edit == null) {
        val draft = combinedDraft ?: return
        val members = draft.members.filterNot { it == address }
        val current = when {
            members.isEmpty() -> ""
            draft.current == address -> members.first()
            else -> draft.current
        }
        combinedDraft = draft.copy(members = members, current = current)
        return
    }
    val row = accountRows.firstOrNull { it.id == edit } ?: return
    if (row.members.size <= 1) return
    val members = row.members.filterNot { it == address }
    val current = if (row.address == address) members.first() else row.address
    writeCombined(row.copy(members = members, address = current))
}

private fun WalletModel.writeCombined(row: AccountRecord) {
    accountRows = accountRows.map { if (it.id == row.id) row else it }
    if (settings.activeAccountId == row.id) activateAccount(row.id)
    persistVault()
    syncHoldings(refresh = false)
}
