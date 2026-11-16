package com.airwave.wallet.vault

import com.airwave.wallet.parseCombinedPaste
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks that a combined vault row keeps its current wallet inside members and cannot sign.
 */
class CombinedVaultTest {
    @Test
    fun combinedCannotSign() {
        val row = AccountRecord(
            id = "c1",
            kind = AccountKind.Combined,
            label = "Combined",
            address = "addr",
            members = listOf("addr"),
        )
        assertFalse(row.canSign())
        assertTrue(row.address in row.members)
    }

    @Test
    fun pasteSkipsBlankInvalidAndDuplicateLines() {
        val current = BurnerKeys.base58(ByteArray(32) { 3 })
        val added = parseCombinedPaste("\nnot-a-key\n$current\n$current\n", listOf(current))
        assertTrue(added.isEmpty())
        val fresh = BurnerKeys.base58(ByteArray(32) { 4 })
        val next = parseCombinedPaste("$fresh\n$current", listOf(current))
        assertEquals(listOf(fresh), next)
        val spaced = parseCombinedPaste("$fresh, $current $fresh", listOf(current))
        assertEquals(listOf(fresh), spaced)
    }
}
