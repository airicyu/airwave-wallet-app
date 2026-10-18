package com.airwave.wallet.vault

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import java.security.SecureRandom

data class BurnerKey(val seed: ByteArray, val address: String) {
    fun zero() = seed.fill(0)
}

object BurnerKeys {
    private val alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

    fun generate(random: SecureRandom = SecureRandom()): BurnerKey {
        val seed = ByteArray(32).also(random::nextBytes)
        val pub = Ed25519PrivateKeyParameters(seed, 0).generatePublicKey().encoded
        return BurnerKey(seed.copyOf(), base58(pub))
    }

    fun addressOf(seed: ByteArray): String {
        val pub = Ed25519PrivateKeyParameters(seed, 0).generatePublicKey().encoded
        return base58(pub)
    }

    fun sign(seed: ByteArray, message: ByteArray): ByteArray {
        val priv = Ed25519PrivateKeyParameters(seed, 0)
        val signer = org.bouncycastle.crypto.signers.Ed25519Signer()
        signer.init(true, priv)
        signer.update(message, 0, message.size)
        return signer.generateSignature()
    }

    fun short(address: String): String =
        if (address.length <= 12) address else address.take(8) + "…" + address.takeLast(6)

    fun base58(data: ByteArray): String {
        var zeros = 0
        while (zeros < data.size && data[zeros] == 0.toByte()) zeros++
        val encoded = ByteArray(data.size * 2)
        var length = 0
        var start = zeros
        while (start < data.size) {
            var carry = data[start].toInt() and 0xff
            var i = 0
            var j = encoded.size - 1
            while (j >= encoded.size - length || carry != 0) {
                carry += 256 * (encoded[j].toInt() and 0xff)
                encoded[j] = (carry % 58).toByte()
                carry /= 58
                j--
                i++
            }
            length = i
            start++
        }
        val skip = encoded.indexOfFirst { it.toInt() != 0 }.let { if (it < 0) encoded.size else it }
        return "1".repeat(zeros) + encoded.copyOfRange(skip, encoded.size).joinToString("") { alphabet[it.toInt()].toString() }
    }

    @Suppress("unused")
    private fun touchPublic(pub: Ed25519PublicKeyParameters) = pub.encoded
}
