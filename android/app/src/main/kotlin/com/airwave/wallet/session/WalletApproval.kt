/**
 * Runs the in-memory MWA approval on WalletModel: offer, approve, reject, and teardown.
 * It does not decrypt the vault. Unlock and lock stay on WalletModel.
 */
package com.airwave.wallet.session

import androidx.lifecycle.viewModelScope
import com.airwave.wallet.activeAccount
import com.airwave.wallet.mwa.MwaPayloads
import com.airwave.wallet.settings.RpcResolver
import com.airwave.wallet.solana.LegacyTransaction
import com.airwave.wallet.solana.SolanaJsonRpc
import com.airwave.wallet.vault.AccountKind
import com.airwave.wallet.vault.BurnerKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun WalletModel.offer(request: WalletOperationRequest) {
    if (approvalSettled) dropSessionChrome()
    val watch = activeAccount()?.kind == AccountKind.Watch
    if (MwaPayloads.failBeforeOffer(walletOperationRequest != null, watch)) {
        if (watch) {
            mwaError = t("mwa.watchAccount")
            route = Route.Home
        }
        request.finish(ApprovalResult.Rejected)
        return
    }
    mwaError = null
    if (!store.exists() || (address == null && route != Route.Unlock)) {
        request.finish(ApprovalResult.Rejected)
        return
    }
    walletOperationRequest = request
    approvalSettled = false
    route = when {
        address == null -> Route.Unlock
        request.kind == WalletOperationKind.Authorize ||
            request.kind == WalletOperationKind.Reauthorize ||
            request.kind == WalletOperationKind.SignMessage ||
            request.kind == WalletOperationKind.SignTransaction -> Route.Home
        else -> Route.Approval
    }
}

fun WalletModel.beginMwaSession() {
    mwaError = null
}

internal fun WalletModel.detachOperation() {
    walletOperationRequest = null
}

fun WalletModel.approve() {
    val request = walletOperationRequest ?: return
    val secret = signingSeed ?: run {
        finishApproval(request, ApprovalResult.Failed)
        return
    }
    approving = true
    viewModelScope.launch {
        val result = withContext(Dispatchers.IO) { request.approve(secret) }
        approving = false
        finishApproval(request, result)
    }
}

private fun WalletModel.finishApproval(request: WalletOperationRequest, result: ApprovalResult) {
    approvalSettled = true
    request.finish(result)
    if (request.kind == WalletOperationKind.Authorize ||
        request.kind == WalletOperationKind.Reauthorize ||
        request.kind == WalletOperationKind.SignMessage ||
        request.kind == WalletOperationKind.SignTransaction
    ) return
    if (address != null) route = Route.Home
    syncHoldings(refresh = false)
}

fun WalletModel.reject() {
    if (walletOperationRequest?.kind == WalletOperationKind.Authorize ||
        walletOperationRequest?.kind == WalletOperationKind.Reauthorize ||
        walletOperationRequest?.kind == WalletOperationKind.SignMessage ||
        walletOperationRequest?.kind == WalletOperationKind.SignTransaction
    ) {
        if (approvalSettled) return
        approvalSettled = true
        try {
            walletOperationRequest?.finish(ApprovalResult.Rejected)
        } catch (_: RuntimeException) {
            // The dApp already dropped the association. The screen still closes below.
        }
        dropSessionChrome()
        onMwaDeclineStale?.invoke()
        return
    }
    if (approvalSettled) {
        dropSessionChrome()
        if (address != null) route = Route.Home
        return
    }
    val request = walletOperationRequest
    walletOperationRequest = null
    request?.finish(ApprovalResult.Rejected)
    if (address != null) {
        route = Route.Home
        syncHoldings(refresh = false)
    }
}

fun WalletModel.endAssociation() {
    if (approvalSettled) {
        dropSessionChrome()
        return
    }
    val request = walletOperationRequest
    walletOperationRequest = null
    approving = false
    try {
        request?.finish(ApprovalResult.Rejected)
    } catch (_: RuntimeException) {
        // The association is already gone. The in-memory walletOperationRequest is cleared above.
    }
}

fun WalletModel.dropSessionChrome() {
    walletOperationRequest = null
    approvalSettled = false
    approving = false
}

internal fun WalletModel.failPending() {
    if (approvalSettled) {
        dropSessionChrome()
        return
    }
    val request = walletOperationRequest
    walletOperationRequest = null
    request?.finish(ApprovalResult.Rejected)
}

fun WalletModel.rpcForSubmit(chain: String?): String? =
    RpcResolver.rpcUrlForSubmit(chain, settings)

fun WalletModel.signAndSend(payload: ByteArray, rpcUrl: String, secret: ByteArray): ApprovalResult {
    val signed = LegacyTransaction.sign(payload, secret) ?: return ApprovalResult.Failed
    val signatureB64 = SolanaJsonRpc.sendTransaction(rpcUrl, signed) ?: return ApprovalResult.Failed
    return try {
        val sigBytes = BurnerKeys.base58Decode(signatureB64)
        ApprovalResult.Signatures(arrayOf(sigBytes))
    } catch (_: Exception) {
        ApprovalResult.Failed
    }
}
