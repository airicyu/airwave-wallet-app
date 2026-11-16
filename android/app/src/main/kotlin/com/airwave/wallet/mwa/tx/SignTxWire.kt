package com.airwave.wallet.mwa.tx

import com.airwave.wallet.vault.BurnerKeys
import java.math.BigInteger

/** Which message layout a sign-transaction payload decoded as. */
enum class TxVersion { Legacy, V0, V1 }

/**
 * Compute budget read from one decoded transaction.
 * A null limit or price means that field was absent. Duplicate means two instructions share a discriminator.
 */
data class TxBudget(
    val version: TxVersion,
    val signed: Boolean,
    val requiredSignatures: Int,
    val cuLimit: Long?,
    val cuPrice: Long?,
    val priorityFeeLamports: BigInteger?,
    val duplicateComputeBudget: Boolean,
    val blockhash: String,
    val instructionPrograms: List<String>,
    val accountKeys: List<String> = emptyList(),
)

/**
 * Decodes legacy and v0 wire bytes and patches an unsigned compute budget.
 * v1 lives on SignTxV1. It does not submit a transaction or call RPC.
 */
object SignTxWire {
    private val COMPUTE_BUDGET = BurnerKeys.base58Decode("ComputeBudget111111111111111111111111111111")
    private val BUILTINS = setOf(
        "11111111111111111111111111111111",
        "Vote111111111111111111111111111111111111111",
        "Stake11111111111111111111111111111111111111",
        "ComputeBudget111111111111111111111111111111",
        "Config1111111111111111111111111111111111111",
        "AddressLookupTab1e1111111111111111111111111",
        "BPFLoaderUpgradeab1e11111111111111111111111",
        "BPFLoader2111111111111111111111111111111111",
        "BPFLoader1111111111111111111111111111111111",
        "Ed25519SigVerify111111111111111111111111111",
        "KeccakSecp256k11111111111111111111111111111",
        "NativeLoader1111111111111111111111111111111",
    )

    /** One compiled instruction plus the account keys it indexes. */
    data class CompiledIx(val program: String, val accounts: List<String>, val data: ByteArray)

    /** Account keys and compiled instructions for the details panel. Empty when the bytes do not parse. */
    data class CompiledTx(val keys: List<String>, val instructions: List<CompiledIx>)

    fun compiled(bytes: ByteArray): CompiledTx = try {
        if (bytes.isNotEmpty() && bytes[0] == 0x81.toByte()) SignTxV1.compiled(bytes) else compiledLegacy(bytes)
    } catch (_: RuntimeException) {
        CompiledTx(emptyList(), emptyList())
    }

    /** Message bytes for the Solana Explorer inspector, without the signature tail. */
    fun messageBytes(bytes: ByteArray): ByteArray? = try {
        if (bytes.isNotEmpty() && bytes[0] == 0x81.toByte()) {
            val required = bytes[1].toInt() and 0xff
            bytes.copyOf(bytes.size - required * 64)
        } else {
            val (count, after) = SignTxCodec.readShortVec(bytes, 0)
            bytes.copyOfRange(after + count * 64, bytes.size)
        }
    } catch (_: RuntimeException) {
        null
    }

    fun inspect(bytes: ByteArray): TxBudget? = try {
        when (versionOf(bytes)) {
            TxVersion.V1 -> SignTxV1.inspect(bytes)
            TxVersion.Legacy, TxVersion.V0 -> inspectLegacy(bytes)
            null -> null
        }
    } catch (_: Exception) {
        null
    }

    fun versionOf(bytes: ByteArray): TxVersion? {
        if (bytes.isEmpty()) return null
        if (bytes[0] == 0x81.toByte()) return TxVersion.V1
        val (count, after) = SignTxCodec.readShortVec(bytes, 0)
        val messageAt = after + count * 64
        if (count !in 1..64 || messageAt >= bytes.size) return null
        val marker = bytes[messageAt].toInt() and 0xff
        return when {
            marker == 0x80 -> TxVersion.V0
            marker < 0x80 -> TxVersion.Legacy
            else -> null
        }
    }

    fun writeBudget(bytes: ByteArray, limit: Long?, price: Long?, priorityFee: BigInteger?): ByteArray? =
        try {
            when (versionOf(bytes)) {
                TxVersion.V1 -> SignTxV1.write(bytes, limit, priorityFee)
                TxVersion.Legacy, TxVersion.V0 -> writeLegacy(bytes, limit, price)
                null -> null
            }
        } catch (_: Exception) {
            null
        }

    fun partialSign(bytes: ByteArray, seed: ByteArray): ByteArray? = try {
        val pub = org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters(seed, 0).generatePublicKey().encoded
        if (versionOf(bytes) == TxVersion.V1) SignTxV1.sign(bytes, seed, pub) else signLegacy(bytes, seed, pub)
    } catch (_: Exception) {
        null
    }

    /** The 64-byte signature in this public key's slot, or null when that slot is absent. */
    fun signerSlot(bytes: ByteArray, pub: ByteArray): ByteArray? = try {
        if (versionOf(bytes) == TxVersion.V1) return SignTxV1.signerSlot(bytes, pub)
        val parsed = run {
            val legacy = parseLegacy(bytes)
            legacy.keys.indexOfFirst { it.contentEquals(pub) } to legacy.signatures
        }
        val index = parsed.first
        if (index < 0) null else parsed.second.getOrNull(index)
    } catch (_: Exception) {
        null
    }

    fun builtinCount(programs: List<String>): Pair<Int, Int> {
        val builtin = programs.count { it in BUILTINS }
        return builtin to (programs.size - builtin)
    }

    private fun inspectLegacy(bytes: ByteArray): TxBudget {
        val parsed = parseLegacy(bytes)
        var limit: Long? = null
        var price: Long? = null
        var limits = 0
        var prices = 0
        parsed.instructions.forEach { ix ->
            val program = parsed.keys[ix.programIdIndex]
            if (!program.contentEquals(COMPUTE_BUDGET) || ix.data.isEmpty()) return@forEach
            if (ix.data[0] == 2.toByte() && ix.data.size >= 5) {
                limits++
                limit = SignTxCodec.readU32(ix.data, 1)
            }
            if (ix.data[0] == 3.toByte() && ix.data.size >= 9) {
                prices++
                price = SignTxCodec.readU64(ix.data, 1)
            }
        }
        return TxBudget(
            version = if (parsed.versioned) TxVersion.V0 else TxVersion.Legacy,
            signed = parsed.signatures.any { slot -> slot.any { it != 0.toByte() } },
            requiredSignatures = parsed.required,
            cuLimit = limit,
            cuPrice = price,
            priorityFeeLamports = null,
            duplicateComputeBudget = limits > 1 || prices > 1,
            blockhash = BurnerKeys.base58(parsed.blockhash),
            instructionPrograms = parsed.instructions.map { BurnerKeys.base58(parsed.keys[it.programIdIndex]) },
            accountKeys = parsed.keys.map { BurnerKeys.base58(it) },
        )
    }

    private fun writeLegacy(bytes: ByteArray, limit: Long?, price: Long?): ByteArray {
        val parsed = parseLegacy(bytes)
        val keys = parsed.keys.toMutableList()
        var readonlyUnsigned = parsed.readonlyUnsigned
        var programIndex = keys.indexOfFirst { it.contentEquals(COMPUTE_BUDGET) }
        if (programIndex < 0) {
            keys += COMPUTE_BUDGET
            programIndex = keys.lastIndex
            readonlyUnsigned++
        }
        val ixs = parsed.instructions.toMutableList()
        if (limit != null) upsert(ixs, programIndex, 2, SignTxCodec.u32(limit))
        if (price != null) upsert(ixs, programIndex, 3, SignTxCodec.u64(price))
        val message = buildLegacyMessage(parsed.versioned, parsed.required, parsed.readonlySigned, readonlyUnsigned, keys, parsed.blockhash, ixs, parsed.suffix)
        return packSignatures(parsed.signatures, message)
    }

    private fun signLegacy(bytes: ByteArray, seed: ByteArray, pub: ByteArray): ByteArray {
        val parsed = parseLegacy(bytes)
        val index = parsed.keys.indexOfFirst { it.contentEquals(pub) }
        if (index < 0 || index >= parsed.signatures.size) throw IllegalArgumentException("signer")
        if (parsed.signatures[index].any { it != 0.toByte() }) return bytes
        val signed = parsed.signatures.toMutableList()
        signed[index] = BurnerKeys.sign(seed, parsed.message)
        return packSignatures(signed, parsed.message)
    }

    private data class Ix(val programIdIndex: Int, val accounts: IntArray, val data: ByteArray)
    private data class Legacy(
        val versioned: Boolean,
        val required: Int,
        val readonlySigned: Int,
        val readonlyUnsigned: Int,
        val keys: List<ByteArray>,
        val blockhash: ByteArray,
        val instructions: List<Ix>,
        val suffix: ByteArray,
        val signatures: List<ByteArray>,
        val message: ByteArray,
    )
    private fun parseLegacy(bytes: ByteArray): Legacy {
        val (count, afterCount) = SignTxCodec.readShortVec(bytes, 0)
        val messageAt = afterCount + count * 64
        val message = bytes.copyOfRange(messageAt, bytes.size)
        val signatures = List(count) { i -> bytes.copyOfRange(afterCount + i * 64, afterCount + (i + 1) * 64) }
        val versioned = message[0] == 0x80.toByte()
        var offset = if (versioned) 1 else 0
        val required = message[offset].toInt() and 0xff
        val readonlySigned = message[offset + 1].toInt() and 0xff
        val readonlyUnsigned = message[offset + 2].toInt() and 0xff
        offset += 3
        val (keyCount, afterKeysCount) = SignTxCodec.readShortVec(message, offset)
        offset = afterKeysCount
        val keys = List(keyCount) {
            message.copyOfRange(offset, offset + 32).also { offset += 32 }
        }
        val blockhash = message.copyOfRange(offset, offset + 32)
        offset += 32
        val (ixCount, afterIx) = SignTxCodec.readShortVec(message, offset)
        offset = afterIx
        val ixs = List(ixCount) {
            val program = message[offset].toInt() and 0xff
            val (accountsLen, afterAccounts) = SignTxCodec.readShortVec(message, offset + 1)
            val accounts = IntArray(accountsLen) { n -> message[afterAccounts + n].toInt() and 0xff }
            val dataAt = afterAccounts + accountsLen
            val (dataLen, afterDataLen) = SignTxCodec.readShortVec(message, dataAt)
            val data = message.copyOfRange(afterDataLen, afterDataLen + dataLen)
            offset = afterDataLen + dataLen
            Ix(program, accounts, data)
        }
        return Legacy(versioned, required, readonlySigned, readonlyUnsigned, keys, blockhash, ixs, message.copyOfRange(offset, message.size), signatures, message)
    }

    private fun buildLegacyMessage(
        versioned: Boolean,
        required: Int,
        readonlySigned: Int,
        readonlyUnsigned: Int,
        keys: List<ByteArray>,
        blockhash: ByteArray,
        ixs: List<Ix>,
        suffix: ByteArray,
    ): ByteArray {
        val body = ArrayList<Byte>()
        if (versioned) body += 0x80.toByte()
        body += required.toByte()
        body += readonlySigned.toByte()
        body += readonlyUnsigned.toByte()
        body += SignTxCodec.writeShortVec(keys.size).toList()
        keys.forEach { body += it.toList() }
        body += blockhash.toList()
        body += SignTxCodec.writeShortVec(ixs.size).toList()
        ixs.forEach { ix ->
            body += ix.programIdIndex.toByte()
            body += SignTxCodec.writeShortVec(ix.accounts.size).toList()
            ix.accounts.forEach { body += it.toByte() }
            body += SignTxCodec.writeShortVec(ix.data.size).toList()
            body += ix.data.toList()
        }
        body += suffix.toList()
        return body.toByteArray()
    }

    private fun packSignatures(signatures: List<ByteArray>, message: ByteArray): ByteArray =
        SignTxCodec.writeShortVec(signatures.size) + signatures.flatMap { it.toList() }.toByteArray() + message

    private fun upsert(ixs: MutableList<Ix>, programIndex: Int, disc: Int, payload: ByteArray) {
        val data = byteArrayOf(disc.toByte()) + payload
        val found = ixs.indexOfFirst { it.programIdIndex == programIndex && it.data.isNotEmpty() && it.data[0] == disc.toByte() }
        val ix = Ix(programIndex, IntArray(0), data)
        if (found >= 0) ixs[found] = ix else ixs.add(0, ix)
    }

    private fun compiledLegacy(bytes: ByteArray): CompiledTx {
        val parsed = parseLegacy(bytes)
        val keys = parsed.keys.map { BurnerKeys.base58(it) }
        val ixs = parsed.instructions.map { ix ->
            CompiledIx(keys[ix.programIdIndex], ix.accounts.map { keys[it] }, ix.data)
        }
        return CompiledTx(keys, ixs)
    }
}

