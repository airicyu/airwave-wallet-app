package com.airwave.wallet.mwa.tx

import com.airwave.wallet.vault.BurnerKeys
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** One instruction row for the sign-transaction details panel. */
data class ShownIx(
    val program: String,
    val kind: String?,
    val fields: List<Pair<String, String>>,
    val accounts: List<String>,
    val dataHex: String?,
)

/**
 * Statically names the same compiled instructions the web wallet shows.
 * An unmatched instruction keeps account shorts and data hex.
 */
object SignTxIx {
    private const val SYSTEM = "11111111111111111111111111111111"
    private const val COMPUTE = "ComputeBudget111111111111111111111111111111"
    private const val TOKEN = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
    private const val TOKEN_2022 = "TokenzQdBNbLqP5VEhdkAS6EPFLC1PHnBqCXEpPxuEb"
    private const val ATA = "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL"
    private const val MEMO = "MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr"

    fun show(tx: SignTxWire.CompiledTx): List<ShownIx> = tx.instructions.map { ix -> decode(ix) }

    fun programLabel(id: String): String = ProgramNames[id] ?: BurnerKeys.short(id)

    private fun decode(ix: SignTxWire.CompiledIx): ShownIx {
        val named = programLabel(ix.program)
        val data = ix.data
        if (ix.program == SYSTEM && data.size == 12 && u32(data, 0) == 2L && ix.accounts.size >= 2) {
            return shown(named, "system.transfer", listOf(
                "from" to short(ix.accounts[0]),
                "to" to short(ix.accounts[1]),
                "lamports" to u64(data, 4),
            ) + extra(ix.accounts, 2))
        }
        if (ix.program == COMPUTE && data.size == 5 && data[0] == 2.toByte()) {
            return shown(named, "computeBudget.setLimit", listOf("units" to u32(data, 1).toString()))
        }
        if (ix.program == COMPUTE && data.size == 9 && data[0] == 3.toByte()) {
            return shown(named, "computeBudget.setPrice", listOf("unitPrice" to u64(data, 1)))
        }
        if ((ix.program == TOKEN || ix.program == TOKEN_2022) && data.size == 9 && data[0] == 3.toByte() && ix.accounts.size >= 3) {
            return shown(named, "token.transfer", listOf(
                "from" to short(ix.accounts[0]),
                "to" to short(ix.accounts[1]),
                "authority" to short(ix.accounts[2]),
                "amount" to u64(data, 1),
            ) + extra(ix.accounts, 3))
        }
        if ((ix.program == TOKEN || ix.program == TOKEN_2022) && data.size == 10 && data[0] == 12.toByte() && ix.accounts.size >= 4) {
            return shown(named, "token.transfer", listOf(
                "from" to short(ix.accounts[0]),
                "mint" to short(ix.accounts[1]),
                "to" to short(ix.accounts[2]),
                "authority" to short(ix.accounts[3]),
                "amount" to u64(data, 1),
                "decimals" to (data[9].toInt() and 0xff).toString(),
            ) + extra(ix.accounts, 4))
        }
        if (ix.program == ATA && data.size == 1 && (data[0] == 0.toByte() || data[0] == 1.toByte()) && ix.accounts.size >= 4) {
            return shown(named, "ata.create", listOf(
                "payer" to short(ix.accounts[0]),
                "account" to short(ix.accounts[1]),
                "wallet" to short(ix.accounts[2]),
                "mint" to short(ix.accounts[3]),
            ) + extra(ix.accounts, 4))
        }
        if (ix.program == MEMO) {
            val text = runCatching { data.toString(Charsets.UTF_8) }.getOrNull()
            if (text != null && text.all { it.code in 32..126 || it == '\n' }) {
                return shown(named, "memo", listOf("content" to text))
            }
        }
        return ShownIx(named, null, emptyList(), ix.accounts.map { short(it) }, data.joinToString("") { "%02x".format(it) })
    }

    private fun shown(program: String, kind: String, fields: List<Pair<String, String>>) =
        ShownIx(program, kind, fields, emptyList(), null)

    private fun extra(accounts: List<String>, from: Int): List<Pair<String, String>> =
        accounts.drop(from).map { "account" to short(it) }

    private fun short(value: String): String = BurnerKeys.short(value)

    private fun u32(data: ByteArray, at: Int): Long =
        ByteBuffer.wrap(data, at, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xffffffffL

    private fun u64(data: ByteArray, at: Int): String {
        val buf = ByteBuffer.wrap(data, at, 8).order(ByteOrder.LITTLE_ENDIAN)
        val lo = buf.int.toLong() and 0xffffffffL
        val hi = buf.int.toLong() and 0xffffffffL
        return (hi shl 32 or lo).toString()
    }
}

private val ProgramNames = mapOf(
    "11111111111111111111111111111111" to "System Program",
    "Vote111111111111111111111111111111111111111" to "Vote",
    "Stake11111111111111111111111111111111111111" to "Stake",
    "ComputeBudget111111111111111111111111111111" to "Compute Budget",
    "Config1111111111111111111111111111111111111" to "Config",
    "AddressLookupTab1e1111111111111111111111111" to "Address Lookup Table",
    "BPFLoaderUpgradeab1e11111111111111111111111" to "BPF Upgradeable Loader",
    "BPFLoader2111111111111111111111111111111111" to "BPF Loader",
    "BPFLoader1111111111111111111111111111111111" to "BPF Loader",
    "Ed25519SigVerify111111111111111111111111111" to "Ed25519",
    "KeccakSecp256k11111111111111111111111111111" to "Secp256k1",
    "NativeLoader1111111111111111111111111111111" to "Native Loader",
    "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA" to "Token Program",
    "TokenzQdBNbLqP5VEhdkAS6EPFLC1PHnBqCXEpPxuEb" to "Token-2022",
    "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL" to "Associated Token Account",
    "MemoSq4gqABAXKb96qnH8TysNcWxMyWCqXgDLGmfcHr" to "Memo",
)
