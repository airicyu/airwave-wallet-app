package com.airwave.wallet.vault

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import javax.crypto.AEADBadTagException

class VaultCipherTest {
    @Test
    fun roundTrip() {
        val cipher = VaultCipher()
        val plain = "keypair".encodeToByteArray()
        val file = cipher.seal("correct-password".toCharArray(), plain)
        assertArrayEquals(plain, cipher.open("correct-password".toCharArray(), file))
    }

    @Test
    fun wrongPasswordFails() {
        val cipher = VaultCipher()
        val file = cipher.seal("correct-password".toCharArray(), byteArrayOf(1, 2, 3))
        assertThrows(AEADBadTagException::class.java) {
            cipher.open("other-password".toCharArray(), file)
        }
    }
}
