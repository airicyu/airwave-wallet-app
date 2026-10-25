/**
 * SLIP-0010 ed25519 derivation for Solana account paths.
 * Does not validate mnemonics or build path strings for the UI.
 */
package com.airwave.wallet.vault

import org.bouncycastle.crypto.digests.SHA512Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.macs.HMac
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.crypto.params.KeyParameter
import java.nio.ByteBuffer
import java.nio.ByteOrder

object Slip10 {
    fun deriveEd25519(seed: ByteArray, path: List<Int>): ByteArray {
        var key = masterKey(seed)
        for (segment in path) {
            key = deriveChild(key, segment)
        }
        return key.copyOfRange(0, 32)
    }

    fun pathFor(kind: ImportPathKind, index: Int): List<Int> = when (kind) {
        ImportPathKind.Phantom -> listOf(44, 501, index, 0)
        ImportPathKind.Cli -> listOf(44, 501, index)
        ImportPathKind.Change -> listOf(44, 501, 0, index)
    }

    fun hardened(segment: Int): Int = segment + 0x80000000.toInt()

    fun addressFromSeed(seed32: ByteArray): String {
        val pub = Ed25519PrivateKeyParameters(seed32, 0).generatePublicKey().encoded
        return BurnerKeys.base58(pub)
    }

    private fun masterKey(seed: ByteArray): ByteArray {
        val mac = HMac(SHA512Digest())
        mac.init(KeyParameter("ed25519 seed".toByteArray()))
        val out = ByteArray(64)
        mac.update(seed, 0, seed.size)
        mac.doFinal(out, 0)
        return out
    }

    private fun deriveChild(parent: ByteArray, segment: Int): ByteArray {
        val mac = HMac(SHA512Digest())
        val data = ByteBuffer.allocate(1 + 32 + 4).order(ByteOrder.BIG_ENDIAN)
        data.put(0)
        data.put(parent, 0, 32)
        data.putInt(segment)
        mac.init(KeyParameter(parent, 32, 32))
        val out = ByteArray(64)
        mac.update(data.array(), 0, data.position())
        mac.doFinal(out, 0)
        return out
    }
}
