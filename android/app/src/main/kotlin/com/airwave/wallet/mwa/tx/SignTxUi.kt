package com.airwave.wallet.mwa.tx

import java.math.BigInteger

/** The original payload and the in-memory working copy for one sign-transaction request. */
class SignTxBytes {
    @Volatile var original: ByteArray = byteArrayOf()
    @Volatile var working: ByteArray = byteArrayOf()
}

/** One expected balance row on the sign-transaction screen. */
data class SignTxDelta(
    val name: String,
    val mint: String,
    val raw: String,
    val decimals: Int,
    val symbol: String,
    val positive: Boolean,
    val verified: Boolean = false,
    val organicScore: String? = null,
)

/**
 * What the sign-transaction screen draws for the in-memory request.
 * A mismatch hides the fee editor. A failure keeps Approve enabled.
 */
data class SignTxUi(
    val loading: Boolean,
    val mismatch: String?,
    val devnetStrip: Boolean,
    val failureTitle: Boolean,
    val failureReason: String?,
    val deltas: List<SignTxDelta>,
    val feeTotal: BigInteger?,
    val signatureFee: BigInteger?,
    val priorityFee: BigInteger?,
    val cuLimit: String,
    val secondField: String,
    val v1: Boolean,
    val editable: Boolean,
    val cuWriteError: Boolean,
    val instructions: List<String>,
)
