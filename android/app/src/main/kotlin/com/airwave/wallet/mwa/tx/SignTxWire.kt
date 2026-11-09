package com.airwave.wallet.mwa.tx

import com.airwave.wallet.vault.BurnerKeys
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder

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
 * Decodes legacy, v0, and v1 wire bytes and patches an unsigned compute budget.
 * It does not submit a transaction or call RPC.
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
        if (bytes.isNotEmpty() && bytes[0] == 0x81.toByte()) compiledV1(bytes) else compiledLegacy(bytes)
    } catch (_: RuntimeException) {
        CompiledTx(emptyList(), emptyList())
    }

    /** Message bytes for the Solana Explorer inspector, without the signature tail. */
    fun messageBytes(bytes: ByteArray): ByteArray? = try {
        if (bytes.isNotEmpty() && bytes[0] == 0x81.toByte()) {
            val required = bytes[1].toInt() and 0xff
            bytes.copyOf(bytes.size - required * 64)
        } else {
            val (count, after) = readShortVec(bytes, 0)
            bytes.copyOfRange(after + count * 64, bytes.size)
        }
    } catch (_: RuntimeException) {
        null
    }

    fun inspect(bytes: ByteArray): TxBudget? = try {
        when (versionOf(bytes)) {
            TxVersion.V1 -> inspectV1(bytes)
            TxVersion.Legacy, TxVersion.V0 -> inspectLegacy(bytes)
            null -> null
        }
    } catch (_: Exception) {
        null
    }

    fun versionOf(bytes: ByteArray): TxVersion? {
        if (bytes.isEmpty()) return null
        if (bytes[0] == 0x81.toByte()) return TxVersion.V1
        val (count, after) = readShortVec(bytes, 0)
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
                TxVersion.V1 -> writeV1(bytes, limit, priorityFee)
                TxVersion.Legacy, TxVersion.V0 -> writeLegacy(bytes, limit, price)
                null -> null
            }
        } catch (_: Exception) {
            null
        }

    fun partialSign(bytes: ByteArray, seed: ByteArray): ByteArray? = try {
        val pub = org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters(seed, 0).generatePublicKey().encoded
        if (versionOf(bytes) == TxVersion.V1) signV1(bytes, seed, pub) else signLegacy(bytes, seed, pub)
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
                limit = readU32(ix.data, 1)
            }
            if (ix.data[0] == 3.toByte() && ix.data.size >= 9) {
                prices++
                price = readU64(ix.data, 1)
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
        if (limit != null) upsert(ixs, programIndex, 2, u32(limit))
        if (price != null) upsert(ixs, programIndex, 3, u64(price))
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

    private fun inspectV1(bytes: ByteArray): TxBudget {
        val parsed = parseV1(bytes)
        return TxBudget(
            version = TxVersion.V1,
            signed = parsed.signatures.any { slot -> slot.any { it != 0.toByte() } },
            requiredSignatures = parsed.required,
            cuLimit = parsed.cuLimit,
            cuPrice = null,
            priorityFeeLamports = parsed.priorityFee,
            duplicateComputeBudget = false,
            blockhash = BurnerKeys.base58(parsed.blockhash),
            instructionPrograms = parsed.programs,
            accountKeys = parsed.keys.map { BurnerKeys.base58(it) },
        )
    }

    private fun writeV1(bytes: ByteArray, limit: Long?, priorityFee: BigInteger?): ByteArray {
        val parsed = parseV1(bytes)
        val fee = priorityFee ?: parsed.priorityFee
        val cu = limit ?: parsed.cuLimit
        val message = buildV1Message(parsed, cu, fee)
        return message + parsed.signatures.flatMap { it.toList() }.toByteArray()
    }

    private fun signV1(bytes: ByteArray, seed: ByteArray, pub: ByteArray): ByteArray {
        val parsed = parseV1(bytes)
        val index = parsed.keys.indexOfFirst { it.contentEquals(pub) }
        if (index < 0 || index >= parsed.signatures.size) throw IllegalArgumentException("signer")
        if (parsed.signatures[index].any { it != 0.toByte() }) return bytes
        val message = bytes.copyOf(bytes.size - parsed.signatures.size * 64)
        val signed = parsed.signatures.toMutableList()
        signed[index] = BurnerKeys.sign(seed, message)
        return message + signed.flatMap { it.toList() }.toByteArray()
    }

    private fun parseV1(bytes: ByteArray): V1 {
        require(bytes[0] == 0x81.toByte())
        val required = bytes[1].toInt() and 0xff
        require(required in 1..64)
        val sigBytes = required * 64
        require(bytes.size > sigBytes + 42)
        val messageLen = bytes.size - sigBytes
        val mask = readU32(bytes, 4).toInt()
        if (mask and 3 == 1 || mask and 3 == 2) throw IllegalArgumentException("priority bits")
        var offset = 42
        val addressCount = bytes[41].toInt() and 0xff
        val keys = List(addressCount) {
            bytes.copyOfRange(offset, offset + 32).also { offset += 32 }
        }
        val hasFee = mask and 3 == 3
        val fee = if (hasFee) {
            val value = readU64Big(bytes, offset)
            offset += 8
            value
        } else {
            null
        }
        val limit = if (mask and 4 != 0) {
            val value = readU32(bytes, offset)
            offset += 4
            value
        } else {
            null
        }
        if (mask and 8 != 0) offset += 4
        if (mask and 16 != 0) offset += 4
        val ixCount = bytes[40].toInt() and 0xff
        val programs = mutableListOf<String>()
        var cursor = offset + ixCount * 4
        for (i in 0 until ixCount) {
            val header = offset + i * 4
            val program = bytes[header].toInt() and 0xff
            val accounts = bytes[header + 1].toInt() and 0xff
            val dataLen = (bytes[header + 2].toInt() and 0xff) or ((bytes[header + 3].toInt() and 0xff) shl 8)
            programs += BurnerKeys.base58(keys[program])
            cursor += accounts + dataLen
        }
        require(cursor == messageLen)
        val configEnd = offset
        val signatures = List(required) { i ->
            bytes.copyOfRange(messageLen + i * 64, messageLen + (i + 1) * 64)
        }
        val feeAndLimit = (if (hasFee) 8 else 0) + (if (limit != null) 4 else 0)
        return V1(
            required,
            bytes.copyOfRange(8, 40),
            keys,
            fee,
            limit,
            mask,
            programs,
            signatures,
            bytes.copyOfRange(0, configEnd - configLength(mask)),
            bytes.copyOfRange(configEnd - configLength(mask) + feeAndLimit, configEnd),
            bytes.copyOfRange(configEnd, messageLen),
        )
    }

    private fun configLength(mask: Int): Int {
        var n = 0
        if (mask and 3 == 3) n += 8
        if (mask and 4 != 0) n += 4
        if (mask and 8 != 0) n += 4
        if (mask and 16 != 0) n += 4
        return n
    }

    private fun buildV1Message(parsed: V1, limit: Long?, fee: BigInteger?): ByteArray {
        var mask = parsed.mask and 3.inv() and 4.inv()
        val values = ArrayList<Byte>()
        if (fee != null) {
            mask = mask or 3
            values += u64Big(fee).toList()
        }
        if (limit != null) {
            mask = mask or 4
            values += u32(limit).toList()
        }
        if (parsed.mask and 8 != 0) mask = mask or 8
        if (parsed.mask and 16 != 0) mask = mask or 16
        val head = parsed.beforeConfig.copyOf()
        ByteBuffer.wrap(head).order(ByteOrder.LITTLE_ENDIAN).putInt(4, mask)
        return head + values.toByteArray() + parsed.preservedConfig + parsed.afterConfig
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
    private data class V1(
        val required: Int,
        val blockhash: ByteArray,
        val keys: List<ByteArray>,
        val priorityFee: BigInteger?,
        val cuLimit: Long?,
        val mask: Int,
        val programs: List<String>,
        val signatures: List<ByteArray>,
        val beforeConfig: ByteArray,
        val preservedConfig: ByteArray,
        val afterConfig: ByteArray,
    )

    private fun parseLegacy(bytes: ByteArray): Legacy {
        val (count, afterCount) = readShortVec(bytes, 0)
        val messageAt = afterCount + count * 64
        val message = bytes.copyOfRange(messageAt, bytes.size)
        val signatures = List(count) { i -> bytes.copyOfRange(afterCount + i * 64, afterCount + (i + 1) * 64) }
        val versioned = message[0] == 0x80.toByte()
        var offset = if (versioned) 1 else 0
        val required = message[offset].toInt() and 0xff
        val readonlySigned = message[offset + 1].toInt() and 0xff
        val readonlyUnsigned = message[offset + 2].toInt() and 0xff
        offset += 3
        val (keyCount, afterKeysCount) = readShortVec(message, offset)
        offset = afterKeysCount
        val keys = List(keyCount) {
            message.copyOfRange(offset, offset + 32).also { offset += 32 }
        }
        val blockhash = message.copyOfRange(offset, offset + 32)
        offset += 32
        val (ixCount, afterIx) = readShortVec(message, offset)
        offset = afterIx
        val ixs = List(ixCount) {
            val program = message[offset].toInt() and 0xff
            val (accountsLen, afterAccounts) = readShortVec(message, offset + 1)
            val accounts = IntArray(accountsLen) { n -> message[afterAccounts + n].toInt() and 0xff }
            val dataAt = afterAccounts + accountsLen
            val (dataLen, afterDataLen) = readShortVec(message, dataAt)
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
        body += writeShortVec(keys.size).toList()
        keys.forEach { body += it.toList() }
        body += blockhash.toList()
        body += writeShortVec(ixs.size).toList()
        ixs.forEach { ix ->
            body += ix.programIdIndex.toByte()
            body += writeShortVec(ix.accounts.size).toList()
            ix.accounts.forEach { body += it.toByte() }
            body += writeShortVec(ix.data.size).toList()
            body += ix.data.toList()
        }
        body += suffix.toList()
        return body.toByteArray()
    }

    private fun packSignatures(signatures: List<ByteArray>, message: ByteArray): ByteArray =
        writeShortVec(signatures.size) + signatures.flatMap { it.toList() }.toByteArray() + message

    private fun upsert(ixs: MutableList<Ix>, programIndex: Int, disc: Int, payload: ByteArray) {
        val data = byteArrayOf(disc.toByte()) + payload
        val found = ixs.indexOfFirst { it.programIdIndex == programIndex && it.data.isNotEmpty() && it.data[0] == disc.toByte() }
        val ix = Ix(programIndex, IntArray(0), data)
        if (found >= 0) ixs[found] = ix else ixs.add(0, ix)
    }

    private fun readShortVec(bytes: ByteArray, at: Int): Pair<Int, Int> {
        var value = 0
        var shift = 0
        var i = at
        while (i < bytes.size) {
            val b = bytes[i].toInt() and 0xff
            value = value or ((b and 0x7f) shl shift)
            i++
            if (b and 0x80 == 0) return value to i
            shift += 7
        }
        throw IllegalArgumentException("shortvec")
    }

    private fun writeShortVec(value: Int): ByteArray {
        val out = ArrayList<Byte>()
        var rest = value
        while (true) {
            var b = rest and 0x7f
            rest = rest shr 7
            if (rest != 0) b = b or 0x80
            out += b.toByte()
            if (rest == 0) break
        }
        return out.toByteArray()
    }

    private fun readU32(bytes: ByteArray, at: Int): Long =
        (bytes[at].toLong() and 0xff) or
            ((bytes[at + 1].toLong() and 0xff) shl 8) or
            ((bytes[at + 2].toLong() and 0xff) shl 16) or
            ((bytes[at + 3].toLong() and 0xff) shl 24)

    private fun readU64(bytes: ByteArray, at: Int): Long = readU64Big(bytes, at).toLong()

    private fun readU64Big(bytes: ByteArray, at: Int): BigInteger {
        var value = BigInteger.ZERO
        for (i in 7 downTo 0) {
            value = value.shiftLeft(8).or(BigInteger.valueOf(bytes[at + i].toLong() and 0xff))
        }
        return value
    }

    private fun u32(value: Long): ByteArray =
        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value.toInt()).array()

    private fun u64(value: Long): ByteArray = u64Big(BigInteger.valueOf(value))

    private fun compiledLegacy(bytes: ByteArray): CompiledTx {
        val parsed = parseLegacy(bytes)
        val keys = parsed.keys.map { BurnerKeys.base58(it) }
        val ixs = parsed.instructions.map { ix ->
            CompiledIx(keys[ix.programIdIndex], ix.accounts.map { keys[it] }, ix.data)
        }
        return CompiledTx(keys, ixs)
    }

    private fun compiledV1(bytes: ByteArray): CompiledTx {
        val required = bytes[1].toInt() and 0xff
        val messageLen = bytes.size - required * 64
        val mask = readU32(bytes, 4).toInt()
        var offset = 42
        val addressCount = bytes[41].toInt() and 0xff
        val keys = List(addressCount) {
            BurnerKeys.base58(bytes.copyOfRange(offset, offset + 32)).also { offset += 32 }
        }
        if (mask and 3 == 3) offset += 8
        if (mask and 4 != 0) offset += 4
        if (mask and 8 != 0) offset += 4
        if (mask and 16 != 0) offset += 4
        val ixCount = bytes[40].toInt() and 0xff
        var cursor = offset + ixCount * 4
        val ixs = List(ixCount) { i ->
            val header = offset + i * 4
            val program = bytes[header].toInt() and 0xff
            val accountsLen = bytes[header + 1].toInt() and 0xff
            val dataLen = (bytes[header + 2].toInt() and 0xff) or ((bytes[header + 3].toInt() and 0xff) shl 8)
            val accounts = List(accountsLen) { n -> keys[bytes[cursor + n].toInt() and 0xff] }
            cursor += accountsLen
            val data = bytes.copyOfRange(cursor, cursor + dataLen)
            cursor += dataLen
            CompiledIx(keys[program], accounts, data)
        }
        return CompiledTx(keys, ixs)
    }

    private fun u64Big(value: BigInteger): ByteArray {
        val out = ByteArray(8)
        var rest = value
        for (i in 0 until 8) {
            out[i] = rest.and(BigInteger.valueOf(0xff)).toByte()
            rest = rest.shiftRight(8)
        }
        return out
    }
}
