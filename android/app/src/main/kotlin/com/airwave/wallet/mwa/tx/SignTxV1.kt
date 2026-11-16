package com.airwave.wallet.mwa.tx

import com.airwave.wallet.vault.BurnerKeys
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Decodes and edits a v1 transaction message.
 * Legacy and v0 stay on SignTxWire. This file does not submit.
 */
internal object SignTxV1 {
    fun inspect(bytes: ByteArray): TxBudget {
        val parsed = parse(bytes)
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

    fun write(bytes: ByteArray, limit: Long?, priorityFee: BigInteger?): ByteArray {
        val parsed = parse(bytes)
        val fee = priorityFee ?: parsed.priorityFee
        val cu = limit ?: parsed.cuLimit
        val message = buildMessage(parsed, cu, fee)
        return message + parsed.signatures.flatMap { it.toList() }.toByteArray()
    }

    fun sign(bytes: ByteArray, seed: ByteArray, pub: ByteArray): ByteArray {
        val parsed = parse(bytes)
        val index = parsed.keys.indexOfFirst { it.contentEquals(pub) }
        if (index < 0 || index >= parsed.signatures.size) throw IllegalArgumentException("signer")
        if (parsed.signatures[index].any { it != 0.toByte() }) return bytes
        val message = bytes.copyOf(bytes.size - parsed.signatures.size * 64)
        val signed = parsed.signatures.toMutableList()
        signed[index] = BurnerKeys.sign(seed, message)
        return message + signed.flatMap { it.toList() }.toByteArray()
    }

    fun signerSlot(bytes: ByteArray, pub: ByteArray): ByteArray? {
        val parsed = parse(bytes)
        val index = parsed.keys.indexOfFirst { it.contentEquals(pub) }
        if (index < 0) return null
        return parsed.signatures.getOrNull(index)
    }

    fun compiled(bytes: ByteArray): SignTxWire.CompiledTx {
        val required = bytes[1].toInt() and 0xff
        val messageLen = bytes.size - required * 64
        val mask = SignTxCodec.readU32(bytes, 4).toInt()
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
            SignTxWire.CompiledIx(keys[program], accounts, data)
        }
        return SignTxWire.CompiledTx(keys, ixs)
    }

    private fun parse(bytes: ByteArray): V1 {
        require(bytes[0] == 0x81.toByte())
        val required = bytes[1].toInt() and 0xff
        require(required in 1..64)
        val sigBytes = required * 64
        require(bytes.size > sigBytes + 42)
        val messageLen = bytes.size - sigBytes
        val mask = SignTxCodec.readU32(bytes, 4).toInt()
        if (mask and 3 == 1 || mask and 3 == 2) throw IllegalArgumentException("priority bits")
        var offset = 42
        val addressCount = bytes[41].toInt() and 0xff
        val keys = List(addressCount) {
            bytes.copyOfRange(offset, offset + 32).also { offset += 32 }
        }
        val hasFee = mask and 3 == 3
        val fee = if (hasFee) {
            val value = SignTxCodec.readU64Big(bytes, offset)
            offset += 8
            value
        } else {
            null
        }
        val limit = if (mask and 4 != 0) {
            val value = SignTxCodec.readU32(bytes, offset)
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

    private fun buildMessage(parsed: V1, limit: Long?, fee: BigInteger?): ByteArray {
        var mask = parsed.mask and 3.inv() and 4.inv()
        val values = ArrayList<Byte>()
        if (fee != null) {
            mask = mask or 3
            values += SignTxCodec.u64Big(fee).toList()
        }
        if (limit != null) {
            mask = mask or 4
            values += SignTxCodec.u32(limit).toList()
        }
        if (parsed.mask and 8 != 0) mask = mask or 8
        if (parsed.mask and 16 != 0) mask = mask or 16
        val head = parsed.beforeConfig.copyOf()
        ByteBuffer.wrap(head).order(ByteOrder.LITTLE_ENDIAN).putInt(4, mask)
        return head + values.toByteArray() + parsed.preservedConfig + parsed.afterConfig
    }

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
}
