/**
 * One wallet account row as stored in vault plaintext and shown in the list.
 * Does not encrypt or parse the vault file.
 */
package com.airwave.wallet.vault

enum class AccountKind(val wire: String) {
    Seed("seed"),
    Burner("burner"),
    Secret("secret"),
    Watch("watch"),
    ;

    companion object {
        fun fromWire(value: String?): AccountKind? =
            entries.firstOrNull { it.wire == value }
    }
}

enum class ImportPathKind(val wire: String) {
    Phantom("phantom"),
    Cli("cli"),
    Change("change"),
    ;

    companion object {
        fun fromWire(value: String?): ImportPathKind =
            entries.firstOrNull { it.wire == value } ?: Phantom
    }
}

data class AccountRecord(
    val id: String,
    val kind: AccountKind,
    val label: String,
    val address: String,
    val seedBase64: String? = null,
    val mnemonic: String? = null,
    val pathKind: ImportPathKind = ImportPathKind.Phantom,
    val pathIndex: Int = 0,
) {
    fun canSign(): Boolean = kind != AccountKind.Watch
}
