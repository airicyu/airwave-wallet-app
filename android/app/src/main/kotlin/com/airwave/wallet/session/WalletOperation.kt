package com.airwave.wallet.session

/** Carries a result that goes back only to the MWA request that created the walletOperationRequest approval. */
sealed class ApprovalResult {
    /** Returns the public key or message signature bytes for that originating request. */
    data class Bytes(val value: ByteArray) : ApprovalResult()
    /** Returns the transaction signatures after a successful sign-and-send. */
    data class Signatures(val value: Array<ByteArray>) : ApprovalResult()
    /** The user declined or locked, or the request was refused before approval chrome. */
    data object Rejected : ApprovalResult()
    /** Signing or submitting failed after the user approved. */
    data object Failed : ApprovalResult()
}

/** Names the approval chrome the MWA screens should draw for the current walletOperationRequest. */
enum class WalletOperationKind { Authorize, Reauthorize, SignMessage, SignAndSend }

/**
 * One in-memory MWA approval waiting for the user.
 * It is not written to disk. A second offer fails this value in place instead of replacing it, so the originating request still gets a result.
 */
data class WalletOperationRequest(
    val kind: WalletOperationKind,
    val identityName: String = "",
    val identityUri: String = "",
    val chain: String = "",
    val accountLabel: String = "",
    val accountShort: String = "",
    val messageUtf8: String? = null,
    val messageHex: String? = null,
    val amountLamports: java.math.BigInteger? = null,
    val feeLamports: java.math.BigInteger? = null,
    val instructionNames: List<String> = emptyList(),
    val approve: (ByteArray) -> ApprovalResult,
    val finish: (ApprovalResult) -> Unit,
)
