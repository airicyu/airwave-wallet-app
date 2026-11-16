package com.airwave.wallet.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

/**
 * Checks activity row mapping for enhanced transactions and signature lists.
 * These tests do not call the network.
 */
class ActivityMapTest {
    private val owner = "OwnerPubkey11111111111111111111111111111111"
    private val sol = "So11111111111111111111111111111111111111112"
    private val usdc = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"

    @Test
    fun heliusKeyMatchesWeb() {
        assertEquals("bare-key", activityHeliusKey("  bare-key  "))
        assertNull(activityHeliusKey("two words"))
        assertNull(activityHeliusKey(""))
        assertEquals("from-rpc", activityHeliusKey("https://mainnet.helius-rpc.com/?api-key=from-rpc"))
        assertEquals("from-api", activityHeliusKey("https://api.helius.xyz/?apiKey=from-api"))
        assertEquals("prefer", activityHeliusKey("https://example.com/?apiKey=alt&api-key=prefer"))
    }

    @Test
    fun heliusUiAmountAndIntegerLamports() {
        val token = """{"signature":"UiSig11111111111111111111111111111111111111111111111111111111","type":"TRANSFER","tokenTransfers":[{"fromUserAccount":"Other1111111111111111111111111111111111111111","toUserAccount":"$owner","mint":"$usdc","tokenAmount":50}]}"""
        val row = rowsFromEnhanced("[$token]", owner, false)!!.rows[0]
        assertEquals(ActivityKind.Receive, row.kind)
        assertEquals("+50 USDC", row.detail)
        val sol = """{"signature":"LamSig1111111111111111111111111111111111111111111111111111111","type":"TRANSFER","nativeTransfers":[{"fromUserAccount":"$owner","toUserAccount":"DestPubkey111111111111111111111111111111111","amount":100000}]}"""
        val sent = rowsFromEnhanced("[$sol]", owner, false)!!.rows[0]
        assertEquals(ActivityKind.Send, sent.kind)
        assertTrue(sent.detail.startsWith("−0.0001 SOL"))
    }

    @Test
    fun swapBothSides() {
        val body = """[{"signature":"SwapSig111111111111111111111111111111111111111111111111111111","type":"SWAP","timestamp":1700000000,"events":{"swap":{"tokenInputs":[{"mint":"$sol","rawTokenAmount":{"tokenAmount":"11500000","decimals":9}}],"tokenOutputs":[{"mint":"$usdc","rawTokenAmount":{"tokenAmount":"1188000","decimals":6}}]}}}]"""
        val page = rowsFromEnhanced(body, owner, false)!!
        assertEquals(1, page.rows.size)
        assertEquals(ActivityKind.Swap, page.rows[0].kind)
        assertEquals("0.0115 SOL → 1.188 USDC", page.rows[0].detail)
        assertEquals(ActivityLead.None, page.rows[0].lead)
    }

    @Test
    fun transferSendReceiveAndBoth() {
        val send = transfer("SendSig111111111111111111111111111111111111111111111111111111", """{"fromUserAccount":"$owner","toUserAccount":"DestPubkey111111111111111111111111111111111","amount":"100000"}""", "")
        val receive = transfer("RecvSig111111111111111111111111111111111111111111111111111111", "", """{"fromUserAccount":"DestPubkey111111111111111111111111111111111","toUserAccount":"$owner","amount":"70000000"}""")
        val both = transfer(
            "BothSig111111111111111111111111111111111111111111111111111111",
            """{"fromUserAccount":"$owner","toUserAccount":"DestPubkey111111111111111111111111111111111","amount":"1"}""",
            """{"fromUserAccount":"DestPubkey111111111111111111111111111111111","toUserAccount":"$owner","amount":"2"}""",
        )
        assertEquals(ActivityKind.Send, rowsFromEnhanced("[$send]", owner, false)!!.rows[0].kind)
        assertTrue(rowsFromEnhanced("[$send]", owner, false)!!.rows[0].detail.startsWith("−0.0001 SOL → Dest"))
        assertEquals(ActivityKind.Receive, rowsFromEnhanced("[$receive]", owner, false)!!.rows[0].kind)
        assertEquals("+0.07 SOL", rowsFromEnhanced("[$receive]", owner, false)!!.rows[0].detail)
        val mixed = rowsFromEnhanced("[$both]", owner, false)!!.rows[0]
        assertEquals(ActivityKind.Tx, mixed.kind)
        assertTrue(mixed.detail.startsWith("Both"))
    }

    @Test
    fun otherTypeAndFailedEnhanced() {
        val body = """[{"signature":"OthrSig111111111111111111111111111111111111111111111111111111","type":"UNKNOWN","transactionError":{"InstructionError":[0,"Custom"]}}]"""
        val row = rowsFromEnhanced(body, owner, false)!!.rows[0]
        assertEquals(ActivityKind.Tx, row.kind)
        assertEquals(ActivityLead.Fail, row.lead)
        assertTrue(row.detail.startsWith("Othr"))
    }

    @Test
    fun signatureListLead() {
        val ok = """[{"signature":"OkSig11111111111111111111111111111111111111111111111111111111","err":null,"blockTime":1700000001}]"""
        val bad = """[{"signature":"BadSig1111111111111111111111111111111111111111111111111111111","err":{"InstructionError":[0,"Custom"]},"blockTime":1700000002}]"""
        assertEquals(ActivityLead.Ok, rowsFromSignatures(ok, true)!!.rows[0].lead)
        assertEquals(ActivityKind.Tx, rowsFromSignatures(ok, true)!!.rows[0].kind)
        assertEquals(ActivityLead.Fail, rowsFromSignatures(bad, false)!!.rows[0].lead)
        assertTrue(rowsFromSignatures(ok, true)!!.rows[0].orbUrl.contains("cluster=devnet"))
    }

    @Test
    fun pageCapAndCursor() {
        val items = (1..21).joinToString(",") {
            """{"signature":"Sig%02d1111111111111111111111111111111111111111111111111111111","type":"UNKNOWN"}""".format(it)
        }
        val page = rowsFromEnhanced("[$items]", owner, false)!!
        assertEquals(20, page.rows.size)
        assertEquals(21, page.rawCount)
        assertTrue(page.cursor!!.startsWith("Sig21"))
    }

    @Test
    fun amountFromDecimalStringNotDouble() {
        assertEquals("1.5", formatUnits(BigInteger("1500000000"), 9))
        val body = """[{"signature":"NumSig1111111111111111111111111111111111111111111111111111111","type":"TRANSFER","nativeTransfers":[{"fromUserAccount":"$owner","toUserAccount":"DestPubkey111111111111111111111111111111111","amount":1.5}]}]"""
        val row = rowsFromEnhanced(body, owner, false)!!.rows[0]
        assertEquals(ActivityKind.Tx, row.kind)
        assertTrue(row.detail.startsWith("NumS"))
    }

    @Test
    fun missingDecimalsHasNoAmount() {
        val body = """[{"signature":"DecSig1111111111111111111111111111111111111111111111111111111","type":"TRANSFER","tokenTransfers":[{"fromUserAccount":"$owner","toUserAccount":"DestPubkey111111111111111111111111111111111","mint":"$usdc","rawTokenAmount":{"tokenAmount":"10"}}]}]"""
        val row = rowsFromEnhanced(body, owner, false)!!.rows[0]
        assertEquals(ActivityKind.Tx, row.kind)
        assertTrue(row.detail.startsWith("DecS"))
        val wide = """[{"signature":"WideSig111111111111111111111111111111111111111111111111111111","type":"TRANSFER","tokenTransfers":[{"fromUserAccount":"$owner","toUserAccount":"DestPubkey111111111111111111111111111111111","mint":"$usdc","rawTokenAmount":{"tokenAmount":"10","decimals":19}}],"nativeTransfers":[{"fromUserAccount":"$owner","toUserAccount":"DestPubkey111111111111111111111111111111111","amount":"1000"}]}]"""
        val wideRow = rowsFromEnhanced(wide, owner, false)!!.rows[0]
        assertEquals(ActivityKind.Tx, wideRow.kind)
        assertTrue(wideRow.detail.startsWith("Wide"))
        assertNull(rowsFromEnhanced("{", owner, false))
    }

    private fun transfer(signature: String, outgoing: String, incoming: String): String {
        val legs = listOf(outgoing, incoming).filter { it.isNotEmpty() }.joinToString(",")
        return """{"signature":"$signature","type":"TRANSFER","nativeTransfers":[$legs]}"""
    }
}
