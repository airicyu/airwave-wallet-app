package com.airwave.wallet.mwa.tx

import com.airwave.wallet.settings.AirwaveSettings
import com.airwave.wallet.settings.Cluster
import com.airwave.wallet.settings.RpcClusterSettings
import com.airwave.wallet.vault.BurnerKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks sign-and-send offer gates and signature matching.
 * These tests do not call the network and do not use a secret.
 */
class SignAndSendGateTest {
    @Test
    fun emptyAndMultiplePayloadsDecline() {
        assertTrue(SignAndSendGate.declinePayload(null))
        assertTrue(SignAndSendGate.declinePayload(arrayOf()))
        assertTrue(SignAndSendGate.declinePayload(arrayOf(byteArrayOf())))
        assertTrue(SignAndSendGate.declinePayload(arrayOf(byteArrayOf(1), byteArrayOf(2))))
        assertFalse(SignAndSendGate.declinePayload(arrayOf(byteArrayOf(1))))
        assertEquals(null, SignTxWire.inspect(byteArrayOf(9)))
    }

    @Test
    fun unknownChainTestnetAndMainnetWithoutRpcDecline() {
        val devnet = AirwaveSettings(cluster = Cluster.Devnet)
        assertTrue(SignAndSendGate.offer("solana:localnet", devnet) is SignAndSendGate.Offer.Decline)
        assertTrue(SignAndSendGate.offer("solana:testnet", devnet) is SignAndSendGate.Offer.Decline)
        assertTrue(SignAndSendGate.offer("testnet", devnet) is SignAndSendGate.Offer.Decline)
        val mainnet = AirwaveSettings(cluster = Cluster.Mainnet)
        assertTrue(SignAndSendGate.offer("solana:mainnet", mainnet) is SignAndSendGate.Offer.Decline)
    }

    @Test
    fun devnetOpensAndMainnetWithRpcMismatches() {
        val devnet = AirwaveSettings(cluster = Cluster.Devnet)
        val open = SignAndSendGate.offer("solana:devnet", devnet)
        assertTrue(open is SignAndSendGate.Offer.Open)
        assertFalse((open as SignAndSendGate.Offer.Open).clusterMismatch)
        val both = AirwaveSettings(
            cluster = Cluster.Devnet,
            rpcByCluster = mapOf(
                Cluster.Mainnet to RpcClusterSettings(urls = listOf("https://rpc.example"), active = "https://rpc.example"),
            ),
        )
        val mismatch = SignAndSendGate.offer("solana:mainnet", both)
        assertTrue(mismatch is SignAndSendGate.Offer.Open)
        assertTrue((mismatch as SignAndSendGate.Offer.Open).clusterMismatch)
    }

    @Test
    fun signatureMatchRequiresTheSame64Bytes() {
        val slot = ByteArray(64) { 7 }
        val encoded = BurnerKeys.base58(slot)
        assertTrue(SignAndSendGate.signatureMatches(encoded, slot))
        val other = slot.copyOf()
        other[0] = 8
        assertFalse(SignAndSendGate.signatureMatches(encoded, other))
        assertFalse(SignAndSendGate.signatureMatches(encoded, ByteArray(64)))
    }
}
