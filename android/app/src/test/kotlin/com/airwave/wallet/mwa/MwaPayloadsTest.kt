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
    fun secondPendingAndWatchFailBeforeOffer() {
        assertTrue(MwaPayloads.failBeforeOffer(hasPending = true, isWatch = false))
        assertTrue(MwaPayloads.failBeforeOffer(hasPending = false, isWatch = true))
        assertFalse(MwaPayloads.failBeforeOffer(hasPending = false, isWatch = false))
    }
}
