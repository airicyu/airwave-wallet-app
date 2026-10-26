package com.airwave.wallet.vault

import android.content.res.Resources
import org.bouncycastle.crypto.digests.SHA256Digest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Generates, validates, and extracts a seed from a BIP39 mnemonic.
 * Word-list lookup lives in Bip39English.kt.
 */
object Bip39 {
    fun generate12(resources: Resources, random: SecureRandom = SecureRandom()): String {
        val entropy = ByteArray(16).also(random::nextBytes)
        return encode(resources, entropy)
    }

    fun validateChecksum(resources: Resources, mnemonic: String): Boolean {
        val words = normalizeWords(mnemonic)
        if (words.size != 12 && words.size != 24) return false
        val indexes = words.map { Bip39English.indexOf(resources, it) }
        if (indexes.any { it < 0 }) return false
        val entropyBits = words.size * 11
        val checksumBits = entropyBits / 32
        val dataBits = entropyBits - checksumBits
        val bytes = entropyBytes(indexes, dataBits)
        val hash = SHA256Digest().let { d ->
            d.update(bytes, 0, bytes.size)
            val out = ByteArray(32)
            d.doFinal(out, 0)
            out
        }
        val mask = (1 shl checksumBits) - 1
        val expected = (hash[0].toInt() and 0xff) shr (8 - checksumBits)
        val actual = indexes.last() and mask
        return expected == actual
    }

    fun mnemonicToSeed(mnemonic: String, passphrase: String = ""): ByteArray {
        val spec = PBEKeySpec(
            mnemonic.toCharArray(),
            ("mnemonic$passphrase").toByteArray(Charsets.UTF_8),
            2048,
            512,
        )
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512").generateSecret(spec).encoded
    }

    fun deriveSolanaSeed(resources: Resources, mnemonic: String, pathKind: ImportPathKind, index: Int): ByteArray {
        val seed = mnemonicToSeed(normalizeWords(mnemonic).joinToString(" "))
        val path = Slip10.pathFor(pathKind, index).map { Slip10.hardened(it) }
        return Slip10.deriveEd25519(seed, path)
    }

    fun defaultSeedPath(): List<Int> =
        listOf(Slip10.hardened(44), Slip10.hardened(501), Slip10.hardened(0), Slip10.hardened(0))

    fun deriveDefaultSeedAccount(mnemonic: String): ByteArray {
        val seed = mnemonicToSeed(normalizeWords(mnemonic).joinToString(" "))
        return Slip10.deriveEd25519(seed, defaultSeedPath())
    }

    private fun normalizeWords(mnemonic: String): List<String> =
        mnemonic.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }

    private fun encode(resources: Resources, entropy: ByteArray): String {
        val wordlist = Bip39English.words(resources)
        val checksumBits = entropy.size * 8 / 32
        val hash = SHA256Digest().let { d ->
            d.update(entropy, 0, entropy.size)
            val out = ByteArray(32)
            d.doFinal(out, 0)
            out
        }
        val bits = entropy.toBits() + hash.toBits().take(checksumBits)
        val words = (0 until (entropy.size * 8 + checksumBits) / 11).map { i ->
            val idx = bits.subList(i * 11, i * 11 + 11).toInt()
            wordlist[idx]
        }
        return words.joinToString(" ")
    }

    private fun entropyBytes(indexes: List<Int>, dataBits: Int): ByteArray {
        val bits = indexes.flatMap { idx ->
            (10 downTo 0).map { shift -> (idx shr shift) and 1 }
        }.take(dataBits)
        val out = ByteArray(dataBits / 8)
        for (i in out.indices) {
            var b = 0
            for (j in 0 until 8) {
                b = (b shl 1) or bits[i * 8 + j]
            }
            out[i] = b.toByte()
        }
        return out
    }

    private fun ByteArray.toBits(): List<Int> =
        flatMap { byte ->
            (7 downTo 0).map { shift -> (byte.toInt() shr shift) and 1 }
        }

    private fun List<Int>.toInt(): Int {
        var v = 0
        for (b in this) v = (v shl 1) or b
        return v
    }
}
