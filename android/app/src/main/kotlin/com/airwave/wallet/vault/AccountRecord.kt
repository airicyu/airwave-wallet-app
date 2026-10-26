package com.airwave.wallet.vault

/** Records whether this vault row is a seed, burner, imported secret, or watch-only account. */
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

/** Names the BIP44 path flavor to use when importing a mnemonic. */
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

/**
 * One wallet account as stored in vault plaintext and shown in the account list.
 * It does not encrypt or parse the vault file.
 */
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
