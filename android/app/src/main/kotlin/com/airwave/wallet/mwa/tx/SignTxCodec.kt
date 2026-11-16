package com.airwave.wallet.mwa.tx

import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Reads and writes the little-endian integers and short vectors inside a transaction.
 * It does not decide which message version those bytes belong to.
 */
internal object SignTxCodec {
    fun readShortVec(bytes: ByteArray, at: Int): Pair<Int, Int> {
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

    fun writeShortVec(value: Int): ByteArray {
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

    fun readU32(bytes: ByteArray, at: Int): Long =
        (bytes[at].toLong() and 0xff) or
            ((bytes[at + 1].toLong() and 0xff) shl 8) or
            ((bytes[at + 2].toLong() and 0xff) shl 16) or
            ((bytes[at + 3].toLong() and 0xff) shl 24)

    fun readU64(bytes: ByteArray, at: Int): Long = readU64Big(bytes, at).toLong()

    fun readU64Big(bytes: ByteArray, at: Int): BigInteger {
        var value = BigInteger.ZERO
        for (i in 7 downTo 0) {
            value = value.shiftLeft(8).or(BigInteger.valueOf(bytes[at + i].toLong() and 0xff))
        }
        return value
    }

    fun u32(value: Long): ByteArray =
        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value.toInt()).array()

    fun u64(value: Long): ByteArray = u64Big(BigInteger.valueOf(value))

    fun u64Big(value: BigInteger): ByteArray {
        val out = ByteArray(8)
        var rest = value
        for (i in 0 until 8) {
            out[i] = rest.and(BigInteger.valueOf(0xff)).toByte()
            rest = rest.shiftRight(8)
        }
        return out
    }
}
