package com.airwave.wallet.mwa
import com.airwave.wallet.session.WalletModel
import com.airwave.wallet.session.ApprovalResult
import com.airwave.wallet.session.WalletOperationKind
import com.airwave.wallet.session.WalletOperationRequest

import com.airwave.wallet.activeAccount
import com.airwave.wallet.vault.BurnerKeys
import com.solana.mobilewalletadapter.walletlib.scenario.AuthorizeRequest
import com.solana.mobilewalletadapter.walletlib.scenario.AuthorizedAccount
import com.solana.mobilewalletadapter.walletlib.scenario.ReauthorizeRequest
import com.solana.mobilewalletadapter.walletlib.scenario.SignMessagesRequest
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters

/**
 * Builds one `WalletOperationRequest` from a walletlib method.
 * It does not own the Scenario or the screen step. The session applies the result.
 */
object MwaOffers {
    fun authorize(request: AuthorizeRequest, model: WalletModel): WalletOperationRequest {
        val label = model.activeAccount()?.label.orEmpty()
        return WalletOperationRequest(
            kind = WalletOperationKind.Authorize,
            identityName = request.identityName.orEmpty(),
            identityUri = request.identityUri?.toString().orEmpty(),
            chain = request.chain.orEmpty(),
            accountLabel = label,
            accountShort = BurnerKeys.short(model.address.orEmpty()),
            approve = { secret -> ApprovalResult.Bytes(publicKey(secret)) },
            finish = { result ->
                when (result) {
                    is ApprovalResult.Bytes -> request.completeWithAuthorize(
                        arrayOf(
                            AuthorizedAccount(
                                result.value,
                                BurnerKeys.base58(result.value),
                                "base58",
                                label,
                                null,
                                null,
                                null,
                            ),
                        ),
                        null,
                        "web".toByteArray(),
                        null,
                    )
                    else -> request.completeWithDecline()
                }
            },
        )
    }

    fun reauthorize(request: ReauthorizeRequest): WalletOperationRequest =
        WalletOperationRequest(
            kind = WalletOperationKind.Reauthorize,
            identityName = request.identityName.orEmpty(),
            identityUri = request.identityUri?.toString().orEmpty(),
            finish = { result ->
                try {
                    if (result is ApprovalResult.Rejected || result is ApprovalResult.Failed) {
                        request.completeWithDecline()
                    } else {
                        request.completeWithReauthorize()
                    }
                } catch (_: RuntimeException) {
                    // The association is already gone.
                }
            },
            approve = { ApprovalResult.Bytes(byteArrayOf()) },
        )

    /** Null when the payload must be declined before offer. The protocol request is already completed. */
    fun signMessage(request: SignMessagesRequest, model: WalletModel): WalletOperationRequest? {
        val payloads = request.payloads
        if (MwaPayloads.declineBeforeOffer(payloads)) {
            request.completeWithDecline()
            return null
        }
        val bytes = payloads!![0]
        val shown = MwaPayloads.display(bytes)
        val account = model.activeAccount()
        return WalletOperationRequest(
            kind = WalletOperationKind.SignMessage,
            identityName = request.identityName.orEmpty(),
            identityUri = request.identityUri?.toString().orEmpty(),
            chain = request.chain.orEmpty(),
            accountLabel = account?.label.orEmpty(),
            accountShort = BurnerKeys.short(model.address.orEmpty()),
            messageUtf8 = shown.utf8,
            messageHex = shown.hex,
            approve = { secret -> ApprovalResult.Bytes(BurnerKeys.sign(secret, bytes)) },
            finish = { result ->
                when (result) {
                    is ApprovalResult.Bytes -> request.completeWithSignedPayloads(arrayOf(result.value))
                    else -> request.completeWithDecline()
                }
            },
        )
    }

    fun addressesAllow(request: SignMessagesRequest, activeAddress: String?): Boolean {
        val active = activeAddress?.let { BurnerKeys.base58Decode(it) }
        val listed = request.addresses
        if (listed != null && listed.isNotEmpty() &&
            (active == null || !MwaPayloads.addressesAllow(listed, active))
        ) {
            request.completeWithAuthorizationNotValid()
            return false
        }
        return true
    }

    private fun publicKey(seed: ByteArray): ByteArray =
        Ed25519PrivateKeyParameters(seed, 0).generatePublicKey().encoded
}
