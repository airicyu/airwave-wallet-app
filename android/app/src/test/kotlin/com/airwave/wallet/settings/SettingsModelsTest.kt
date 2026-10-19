package com.airwave.wallet.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsModelsTest {
    @Test
    fun cuPriceBounds() {
        assertEquals(25_000L, AirwaveSettings.parseCuPriceInput("25000", 1L))
        assertEquals(1L, AirwaveSettings.parseCuPriceInput("-1", 1L))
        assertEquals(1_000_000_000L, AirwaveSettings.parseCuPriceInput("9999999999", 1L))
        assertEquals(5L, AirwaveSettings.parseCuPriceInput("not-a-number", 5L))
    }

    @Test
    fun idleLockMinutesBounds() {
        assertEquals(10, AirwaveSettings.parseIdleLockMinutes("10", 1))
        assertEquals(1, AirwaveSettings.parseIdleLockMinutes("0", 5))
        assertEquals(1440, AirwaveSettings.parseIdleLockMinutes("99999", 5))
        assertEquals(7, AirwaveSettings.parseIdleLockMinutes("bad", 7))
    }

    @Test
    fun normalizeModelChain() {
        val chain = AirwaveSettings.normalizeModelChain(listOf(" a ", "", "b", "c", "d", "e", "f"))
        assertEquals(5, chain.size)
        assertTrue(chain.contains("a"))
    }
}
