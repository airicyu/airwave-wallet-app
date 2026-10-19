package com.airwave.wallet.solana

import com.airwave.wallet.vault.BurnerKeys
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64

data class TxSummary(
    val instructionNames: List<String>,
    val destination: String?,
    val amountLamports: BigInteger?,
)

object LegacyTransaction {
    private val SYSTEM_PROGRAM = byteArrayOf(
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    )

    fun summarize(payload: ByteArray): TxSummary? = try {
        val parsed = parse(payload)
        val names = mutableListOf<String>()
        var destination: String? = null
        var amount: BigInteger? = null
        parsed.instructions.forEach { ix ->
            val program = parsed.accountKeys[ix.programIdIndex]
            if (program.contentEquals(SYSTEM_PROGRAM) && ix.data.size >= 12) {
                val opcode = readLeInt(ix.data, 0)
                if (opcode == 2) {
                    names += "Transfer"
                    if (ix.accounts.size >= 2) {
                        destination = BurnerKeys.base58(parsed.accountKeys[ix.accounts[1]])
                        amount = readLeLong(ix.data, 4).toBigInteger()
                    }
                } else {
                    names += "System program"
                }
            } else {
                names += "Program invoke"
            }
        }
        TxSummary(names, destination, amount)
    } catch (_: Exception) {
        null
    }

    fun sign(payload: ByteArray, seed: ByteArray): ByteArray? {
        return try {
            val parsed = parse(payload)
            val pub = Ed25519PrivateKeyParameters(seed, 0).generatePublicKey().encoded
            val signerIndex = parsed.accountKeys.indexOfFirst { it.contentEquals(pub) }
            if (signerIndex < 0) return null
            val signature = BurnerKeys.sign(seed, parsed.messageBytes)
            val signatures = parsed.signatures.toMutableList()
            while (signatures.size <= signerIndex) {
                signatures += ByteArray(64)
            }
            signatures[signerIndex] = signature
            serialize(signatures, parsed.messageBytes)
        } catch (_: Exception) {
            null
        }
    }

    private data class ParsedInstruction(
        val programIdIndex: Int,
        val accounts: IntArray,
        val data: ByteArray,
    )

    private data class ParsedTx(
        val signatures: List<ByteArray>,
        val messageBytes: ByteArray,
        val accountKeys: List<ByteArray>,
        val instructions: List<ParsedInstruction>,
    )

    private fun parse(payload: ByteArray): ParsedTx {
        var offset = 0
        val (sigCount, next) = readCompactU16(payload, offset)
        offset = next
        val signatures = mutableListOf<ByteArray>()
        repeat(sigCount) {
            signatures += payload.copyOfRange(offset, offset + 64)
            offset += 64
        }
        val messageStart = offset
        val header = payload.copyOfRange(offset, offset + 3)
        val numRequired = header[0].toInt()
        offset += 3
        val (keyCount, keyOffset) = readCompactU16(payload, offset)
        offset = keyOffset
        val accountKeys = mutableListOf<ByteArray>()
        repeat(keyCount) {
            accountKeys += payload.copyOfRange(offset, offset + 32)
            offset += 32
        }
        offset += 32 // blockhash
        val (ixCount, ixOffset) = readCompactU16(payload, offset)
        offset = ixOffset
        val instructions = mutableListOf<ParsedInstruction>()
        repeat(ixCount) {
            val programIdIndex = payload[offset].toInt() and 0xff
            offset += 1
            val (accLen, accOffset) = readCompactU16(payload, offset)
            offset = accOffset
            val accounts = IntArray(accLen) { i ->
                payload[offset + i].toInt() and 0xff
            }
            offset += accLen
            val (dataLen, dataOffset) = readCompactU16(payload, offset)
            offset = dataOffset
            val data = payload.copyOfRange(offset, offset + dataLen)
            offset += dataLen
            instructions += ParsedInstruction(programIdIndex, accounts, data)
        }
        val messageBytes = payload.copyOfRange(messageStart, offset)
        if (numRequired < 1) throw IllegalArgumentException("no signers")
        return ParsedTx(signatures, messageBytes, accountKeys, instructions)
    }

    private fun serialize(signatures: List<ByteArray>, message: ByteArray): ByteArray {
        val sigCountBytes = writeCompactU16(signatures.size)
        val out = ByteArray(sigCountBytes.size + signatures.size * 64 + message.size)
        var offset = 0
        sigCountBytes.copyInto(out, offset)
        offset += sigCountBytes.size
        signatures.forEach { sig ->
            sig.copyInto(out, offset)
            offset += 64
        }
        message.copyInto(out, offset)
        return out
    }

    fun base64Wire(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    private fun readCompactU16(data: ByteArray, start: Int): Pair<Int, Int> {
        var result = 0
        var shift = 0
        var offset = start
        while (true) {
            val b = data[offset].toInt() and 0xff
            offset++
            result = result or ((b and 0x7f) shl shift)
            if (b and 0x80 == 0) return result to offset
            shift += 7
        }
    }

    private fun writeCompactU16(value: Int): ByteArray {
        var remaining = value
        val bytes = mutableListOf<Byte>()
        while (true) {
            var elem = remaining and 0x7f
            remaining = remaining shr 7
            if (remaining != 0) elem = elem or 0x80
            bytes += elem.toByte()
            if (remaining == 0) break
        }
        return bytes.toByteArray()
    }

    private fun readLeInt(data: ByteArray, offset: Int): Int =
        ByteBuffer.wrap(data, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int

    private fun readLeLong(data: ByteArray, offset: Int): Long =
        ByteBuffer.wrap(data, offset, 8).order(ByteOrder.LITTLE_ENDIAN).long
}
