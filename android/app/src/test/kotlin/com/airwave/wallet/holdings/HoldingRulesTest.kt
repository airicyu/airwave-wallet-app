package com.airwave.wallet.holdings

import com.airwave.wallet.settings.AirwaveSettings
import com.airwave.wallet.settings.Cluster
import com.airwave.wallet.settings.RpcResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

/**
 * Checks base-unit formatting, row filters, and the Helius path switch.
 * These tests do not call the network.
 */
class HoldingRulesTest {
    @Test
    fun formatOneLamportAndHalf() {
        assertEquals("0.000000001", BaseUnits.formatBaseUnits(BigInteger.ONE, 9))
        assertEquals("1", BaseUnits.formatBaseUnits(BigInteger.TEN.pow(9), 9))
        assertEquals(BigInteger.ZERO, BaseUnits.halfTowardZero(BigInteger.ONE))
    }

    @Test
    fun mainnetWithoutRpcIsNotPublic() {
        val endpoint = RpcResolver.endpointForCluster(Cluster.Mainnet, AirwaveSettings(cluster = Cluster.Mainnet))
        assertNull(endpoint)
        assertTrue(endpoint != RpcResolver.PUBLIC_MAINNET)
    }

    @Test
    fun heliusKeyAndCluster() {
        val url = "https://api.helius.xyz/?apiKey=alt&api-key=REPLACE_ME"
        assertEquals("REPLACE_ME", HeliusHoldings.apiKey(url))
        assertNull(HeliusHoldings.apiKey("https://example.com/?api-key=REPLACE_ME"))
        assertFalse(HeliusHoldings.useWalletApi(Cluster.Devnet, url))
        assertTrue(HeliusHoldings.useWalletApi(Cluster.Mainnet, url))
        val settings = AirwaveSettings(heliusApiUrl = "https://rpc.example.com")
        assertTrue(settings.heliusConfigured)
        assertFalse(HeliusHoldings.useWalletApi(Cluster.Mainnet, settings.heliusApiUrl))
    }

    @Test
    fun sumsSameMintAndSkipsMalformed() {
        val body = """
            {"result":{"value":[
              {"account":{"data":{"parsed":{"info":{"mint":"MintA","tokenAmount":{"amount":"2","decimals":2}}}}}},
              {"account":{"data":{"parsed":{"info":{"mint":"MintA","tokenAmount":{"amount":"3","decimals":2}}}}}},
              {"account":{"data":{"parsed":{"info":{"mint":"Bad"}}}}}
            ]}}
        """.trimIndent()
        val accounts = RpcHoldings.tokenAccounts(body, TokenProgramKind.SplToken)
        assertEquals(2, accounts?.size)
        val rows = RpcHoldings.rowsFromRpc(BigInteger.ZERO, accounts!!)
        val mint = rows.first { it.id == "MintA" }
        assertEquals(BigInteger.valueOf(5), mint.raw)
        assertEquals(TokenProgramKind.SplToken, mint.tokenProgram)
        assertTrue(rows.any { it.id == HoldingIds.NATIVE && it.raw == BigInteger.ZERO })
    }

    @Test
    fun dropsZeroDecimalsAndKeepsWrappedSeparate() {
        val accounts = listOf(
            RpcHoldings.ParsedAccount(HoldingIds.WRAPPED, BigInteger.TEN, 9, TokenProgramKind.SplToken),
            RpcHoldings.ParsedAccount("NftMint", BigInteger.ONE, 0, TokenProgramKind.SplToken),
            RpcHoldings.ParsedAccount("EmptyMint", BigInteger.ZERO, 6, TokenProgramKind.Token2022),
        )
        val rows = RpcHoldings.rowsFromRpc(BigInteger.ONE, accounts)
        assertEquals(HoldingIds.NATIVE, rows[0].id)
        assertEquals(HoldingIds.WRAPPED, rows[1].id)
        assertEquals("wSOL", rows[1].symbol)
        assertEquals("SOL", rows[0].symbol)
        assertEquals(2, rows.size)
        val wide = RpcHoldings.tokenAccounts(
            """{"result":{"value":[{"mint":"Wide","amount":"1","decimals":19}]}}""",
            TokenProgramKind.SplToken,
        )
        assertEquals(0, wide?.size)
    }

    @Test
    fun heliusProgramString() {
        val body = """
            {"balances":[{"mint":"MintB","balance":1.5,"decimals":2,"tokenProgram":"token-2022"}],
             "pagination":{"hasMore":false}}
        """.trimIndent()
        val page = HeliusHoldings.page(body)
        assertEquals(TokenProgramKind.Token2022, page?.rows?.single()?.tokenProgram)
        assertEquals(BigInteger.valueOf(150), page?.rows?.single()?.raw)
    }
}
