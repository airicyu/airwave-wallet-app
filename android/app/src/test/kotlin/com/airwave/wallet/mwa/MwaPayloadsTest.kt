package com.airwave.wallet.mwa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks MWA payload display and the fail-before-offer rules.
 */
class MwaPayloadsTest {
    @Test
    fun emptyPayloadsDecline() {
        assertTrue(MwaPayloads.emptyPayloads(null))
        assertTrue(MwaPayloads.emptyPayloads(emptyArray()))
        assertTrue(MwaPayloads.emptyPayloads(arrayOf(ByteArray(0))))
        assertFalse(MwaPayloads.emptyPayloads(arrayOf("airwave mwa test".toByteArray())))
    }

    @Test
    fun utf8MessageKeepsText() {
        val shown = MwaPayloads.display("airwave mwa test".toByteArray())
        assertEquals("airwave mwa test", shown.utf8)
        assertNull(shown.hex)
    }

    @Test
    fun invalidUtf8UsesHex() {
        val shown = MwaPayloads.display(byteArrayOf(0xff.toByte(), 0xfe.toByte()))
        assertNull(shown.utf8)
        assertEquals("fffe", shown.hex)
    }

    @Test
    fun multiPayloadDeclinesBeforeOffer() {
        val one = "hello".toByteArray()
        assertTrue(MwaPayloads.declineBeforeOffer(null))
        assertTrue(MwaPayloads.declineBeforeOffer(arrayOf(ByteArray(0))))
        assertTrue(MwaPayloads.declineBeforeOffer(arrayOf(ByteArray(0), one)))
        assertTrue(MwaPayloads.declineBeforeOffer(arrayOf(one, one)))
        assertFalse(MwaPayloads.declineBeforeOffer(arrayOf(one)))
    }

    @Test
    fun addressListMustBeExactlyTheActiveKey() {
        val active = byteArrayOf(1, 2, 3)
        val other = byteArrayOf(9, 9, 9)
        assertTrue(MwaPayloads.addressesAllow(null, active))
        assertTrue(MwaPayloads.addressesAllow(emptyArray(), active))
        assertTrue(MwaPayloads.addressesAllow(arrayOf(active), active))
        assertFalse(MwaPayloads.addressesAllow(arrayOf(other), active))
        assertFalse(MwaPayloads.addressesAllow(arrayOf(active, other), active))
    }

    @Test
    fun secondPendingAndWatchFailBeforeOffer() {
        assertTrue(MwaPayloads.failBeforeOffer(hasPending = true, isWatch = false))
        assertTrue(MwaPayloads.failBeforeOffer(hasPending = false, isWatch = true))
        assertFalse(MwaPayloads.failBeforeOffer(hasPending = false, isWatch = false))
    }
}
