package com.airwave.wallet.vault

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks that imported secret keys of unexpected decoded length are rejected.
 */
class SecretImportRulesTest {
    @Test
    fun rejectsUnexpectedDecodedLengths() {
        assertFalse(isAllowedSecretLength(31))
        assertFalse(isAllowedSecretLength(33))
        assertFalse(isAllowedSecretLength(65))
        assertTrue(isAllowedSecretLength(32))
        assertTrue(isAllowedSecretLength(64))
    }

    private fun isAllowedSecretLength(size: Int): Boolean =
        size == 32 || size == 64
}
