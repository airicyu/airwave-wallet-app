package com.airwave.wallet.mwa.tx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

/** Covers version dispatch, fee math, and unsigned budget writes. No keys are embedded. */
class SignTxPlanTest {
    @Test
    fun unknownVersionDeclines() {
        val bytes = byteArrayOf(0x01, 0x82.toByte())
        assertNull(SignTxWire.versionOf(bytes))
        assertNull(SignTxWire.inspect(bytes))
    }

    @Test
    fun legacyPriorityFeeUsesIntegerCeiling() {
        assertEquals(BigInteger.valueOf(2), SignTxMath.priorityFromPrice(3, 500_000))
        assertEquals(BigInteger.ONE, SignTxMath.priorityFromPrice(1, 1))
    }

    @Test
    fun legacyMissingPriceWritesDefaultAndLeavesSignedBytesAlone() {
        val unsigned = legacy(signed = false)
        val prepared = SignTxPlan.prepare(unsigned, 25_000, null, null, null, null)!!
        assertEquals(TxVersion.Legacy, prepared.version)
        assertTrue(prepared.editable)
        assertEquals(25_000L, prepared.cuPrice)
        assertFalse(prepared.bytes.contentEquals(unsigned))
        val signed = legacy(signed = true)
        val kept = SignTxPlan.prepare(signed, 25_000, null, null, null, null)!!
        assertFalse(kept.editable)
        assertTrue(kept.bytes.contentEquals(signed))
    }

    @Test
    fun v1MissingFeeWrites5000AndLeavesLimitUnset() {
        val bytes = v1(fee = null, limit = null)
        val prepared = SignTxPlan.prepare(bytes, 25_000, null, null, null, null)!!
        assertEquals(TxVersion.V1, prepared.version)
        assertNull(prepared.cuLimit)
        assertEquals(BigInteger.valueOf(5_000), prepared.priorityLamports)
        val again = SignTxWire.inspect(prepared.bytes)!!
        assertNull(again.cuLimit)
        assertEquals(BigInteger.valueOf(5_000), again.priorityFeeLamports)
    }

    @Test
    fun v0MarkerIsNotLegacy() {
        val bytes = legacy(signed = false).toMutableList()
        val messageAt = 1 + 64
        bytes.add(messageAt, 0x80.toByte())
        assertEquals(TxVersion.V0, SignTxWire.versionOf(bytes.toByteArray()))
    }

    private fun legacy(signed: Boolean): ByteArray {
        val signer = ByteArray(32) { 7 }
        val system = ByteArray(32)
        val blockhash = ByteArray(32) { 9 }
        val message = ArrayList<Byte>()
        message += 1
        message += 0
        message += 1
        message += 2
        message += signer.toList()
        message += system.toList()
        message += blockhash.toList()
        message += 1
        message += 1
        message += 0
        message += 0
        val sig = ByteArray(64)
        if (signed) sig[0] = 1
        return byteArrayOf(1) + sig + message.toByteArray()
    }

    private fun v1(fee: BigInteger?, limit: Long?): ByteArray {
        var mask = 0
        val config = ArrayList<Byte>()
        if (fee != null) {
            mask = mask or 3
            var rest = fee
            repeat(8) {
                config += rest!!.and(BigInteger.valueOf(0xff)).toByte()
                rest = rest!!.shiftRight(8)
            }
        }
        if (limit != null) {
            mask = mask or 4
            config += (limit and 0xff).toByte()
            config += ((limit shr 8) and 0xff).toByte()
            config += ((limit shr 16) and 0xff).toByte()
            config += ((limit shr 24) and 0xff).toByte()
        }
        val head = ByteArray(42)
        head[0] = 0x81.toByte()
        head[1] = 1
        head[4] = mask.toByte()
        head[40] = 0
        head[41] = 1
        return head + ByteArray(32) { 4 } + config.toByteArray() + ByteArray(64)
    }
}
