package com.airwave.wallet.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RpcResolverTest {
    @Test
    fun mapRequestChain() {
        assertEquals(RpcResolver.SubmitCluster.Devnet, RpcResolver.mapRequestChain("solana:devnet"))
        assertEquals(RpcResolver.SubmitCluster.Mainnet, RpcResolver.mapRequestChain("mainnet-beta"))
        assertEquals(RpcResolver.SubmitCluster.Testnet, RpcResolver.mapRequestChain("testnet"))
        assertEquals(RpcResolver.SubmitCluster.Unknown, RpcResolver.mapRequestChain("solana:localnet"))
    }

    @Test
    fun submitUsesCustomDevnet() {
        val settings = AirwaveSettings(
            rpcByCluster = mapOf(
                Cluster.Devnet to RpcClusterSettings(urls = listOf("https://dev.custom"), active = "https://dev.custom"),
            ),
        )
        assertEquals("https://dev.custom", RpcResolver.rpcUrlForSubmit("solana:devnet", settings))
    }

    @Test
    fun mainnetUnsetFails() {
        val settings = AirwaveSettings(cluster = Cluster.Mainnet)
        assertNull(RpcResolver.rpcUrlForSubmit("solana:mainnet", settings))
    }

    @Test
    fun allowedUrls() {
        assertTrue(RpcResolver.isAllowedCustomUrl("https://api.example.com"))
        assertTrue(RpcResolver.isAllowedCustomUrl("http://localhost:8899"))
        assertTrue(!RpcResolver.isAllowedCustomUrl("http://evil.com"))
        assertTrue(!RpcResolver.isAllowedCustomUrl("https://user:pass@host"))
    }
}
