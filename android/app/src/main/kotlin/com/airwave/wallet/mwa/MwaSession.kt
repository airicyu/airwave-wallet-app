package com.airwave.wallet.mwa
import com.airwave.wallet.session.WalletModel
import com.airwave.wallet.session.beginMwaSession
import com.airwave.wallet.session.detachOperation
import com.airwave.wallet.session.dropSessionChrome
import com.airwave.wallet.session.offer
import com.airwave.wallet.session.ApprovalResult
import com.airwave.wallet.mwa.tx.SignTxBytes
import com.airwave.wallet.mwa.tx.SignTxRun
import com.airwave.wallet.mwa.tx.SignTxUi
import com.airwave.wallet.session.WalletOperationRequest

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigInteger
import com.airwave.wallet.activeAccount
import com.airwave.wallet.vault.AccountKind
import com.solana.mobilewalletadapter.walletlib.scenario.AuthorizeRequest
import com.solana.mobilewalletadapter.walletlib.scenario.ReauthorizeRequest
import com.solana.mobilewalletadapter.walletlib.scenario.SignMessagesRequest
import com.solana.mobilewalletadapter.walletlib.scenario.SignTransactionsRequest

/**
 * Turns one association's method calls into the screen step.
 * It does not start or close the Scenario. The host does that and forwards each callback here.
 */
class MwaSession {
    private var wallet: WalletModel? = null
    private var reauthorize: ReauthorizeRequest? = null
    private var parkedSign: SignMessagesRequest? = null
    internal val signBytes = SignTxBytes()
    private var scope: CoroutineScope? = null
    private var signJob: Job? = null
    private var signGen = 0
    var signUi by mutableStateOf<SignTxUi?>(null)
        private set
    var step by mutableStateOf<MwaStep>(MwaStep.Waiting)
        private set

    fun bindScope(scope: CoroutineScope) {
        this.scope = scope
    }

    fun noteLaunch() {
        wallet?.beginMwaSession()
    }

    fun bindWallet(model: WalletModel) {
        wallet = model
        model.onMwaUnlocked = { advanceAfterUnlock() }
        model.onMwaInterrupted = { abandonOperation() }
        model.onMwaDeclineStale = { markFinished() }
    }

    fun completeReauthorize(allowed: Boolean) {
        val request = reauthorize ?: return
        reauthorize = null
        if (allowed) request.completeWithReauthorize() else request.completeWithDecline()
    }

    fun markFinished() {
        abandonOperation()
        reauthorize?.completeWithDecline()
        reauthorize = null
        step = MwaStep.Finished
    }

    fun offerAuthorize(request: AuthorizeRequest) {
        val model = wallet
        if (model == null || operationInProgress()) {
            request.completeWithDecline()
            return
        }
        val operation = MwaOffers.authorize(request, model)
        if (needsUnlock(model)) {
            step = MwaStep.UnlockConnect(operation)
            return
        }
        showConnect(operation)
    }

    fun offerReauthorize(request: ReauthorizeRequest) {
        val model = wallet
        if (model == null || operationInProgress()) {
            request.completeWithDecline()
            return
        }
        val operation = MwaOffers.reauthorize(request)
        if (needsUnlock(model)) {
            step = MwaStep.UnlockReauthorize(operation)
            return
        }
        finishReauthorize(operation)
    }

    fun offerSignMessage(request: SignMessagesRequest) {
        val model = wallet
        if (model == null || operationInProgress()) {
            request.completeWithDecline()
            return
        }
        val operation = MwaOffers.signMessage(request, model) ?: return
        if (needsUnlock(model)) {
            parkedSign = request
            step = MwaStep.UnlockSignMessage(operation)
            return
        }
        if (!MwaOffers.addressesAllow(request, model.address)) return
        showSign(operation)
    }

    fun offerSignTransaction(request: SignTransactionsRequest) {
        val model = wallet
        if (model == null || operationInProgress()) {
            request.completeWithDecline()
            return
        }
        val operation = MwaOffers.signTransaction(request, model, signBytes) ?: return
        if (needsUnlock(model)) {
            step = MwaStep.UnlockSignTransaction(operation)
            return
        }
        showSignTx(operation)
    }

    private fun advanceAfterUnlock() {
        when (val current = step) {
            is MwaStep.UnlockConnect -> showConnect(current.request)
            is MwaStep.UnlockReauthorize -> finishReauthorize(current.request)
            is MwaStep.UnlockSignMessage -> showSign(current.request)
            is MwaStep.UnlockSignTransaction -> showSignTx(current.request)
            else -> Unit
        }
    }

    fun abandonOperation() {
        val request = when (val current = step) {
            is MwaStep.UnlockConnect -> current.request
            is MwaStep.ConnectApproval -> current.request
            is MwaStep.UnlockReauthorize -> current.request
            is MwaStep.UnlockSignMessage -> current.request
            is MwaStep.SignMessageApproval -> current.request
            is MwaStep.UnlockSignTransaction -> current.request
            is MwaStep.SignTransactionApproval -> current.request
            else -> null
        }
        parkedSign = null
        signGen++
        signJob?.cancel()
        signUi = null
        if (step != MwaStep.Finished) step = MwaStep.Waiting
        val model = wallet ?: return
        if (model.approvalSettled) {
            model.dropSessionChrome()
            return
        }
        model.detachOperation()
        try {
            request?.finish(ApprovalResult.Rejected)
        } catch (_: RuntimeException) {
            // The association is already gone.
        }
    }

    private fun showConnect(request: WalletOperationRequest) {
        val model = wallet ?: return
        if (model.activeAccount()?.kind == AccountKind.Watch) {
            request.finish(ApprovalResult.Rejected)
            step = MwaStep.WatchBlocked(model.t("mwa.watchAccount"))
            return
        }
        model.offer(request)
        if (model.walletOperationRequest == null) return
        step = MwaStep.ConnectApproval(request)
    }

    private fun finishReauthorize(request: WalletOperationRequest) {
        request.finish(ApprovalResult.Bytes(byteArrayOf()))
        if (step != MwaStep.Finished) step = MwaStep.Waiting
    }

    private fun showSign(request: WalletOperationRequest) {
        val protocol = parkedSign
        parkedSign = null
        val model = wallet ?: return
        if (protocol != null && !MwaOffers.addressesAllow(protocol, model.address)) {
            step = MwaStep.Waiting
            return
        }
        if (model.activeAccount()?.kind == AccountKind.Watch) {
            request.finish(ApprovalResult.Rejected)
            step = MwaStep.WatchBlocked(model.t("mwa.watchAccount"))
            return
        }
        model.offer(request)
        if (model.walletOperationRequest == null) return
        step = MwaStep.SignMessageApproval(request)
    }

    private fun showSignTx(request: WalletOperationRequest) {
        val model = wallet ?: return
        if (model.activeAccount()?.kind == AccountKind.Watch) {
            request.finish(ApprovalResult.Rejected)
            step = MwaStep.WatchBlocked(model.t("mwa.watchAccount"))
            return
        }
        model.offer(request)
        if (model.walletOperationRequest == null) return
        step = MwaStep.SignTransactionApproval(request)
        refreshSignTx(null, null, null)
    }

    fun refreshSignTx(userLimit: Long?, userPrice: Long?, userPriority: BigInteger?) {
        val model = wallet ?: return
        val ticket = ++signGen
        signJob?.cancel()
        signUi = signUi?.copy(loading = true, deltas = emptyList(), failureTitle = false)
        signJob = scope?.launch(Dispatchers.IO) {
            val (bytes, ui) = SignTxRun.load(
                signBytes.original,
                model.settings,
                model.address,
                userLimit,
                userPrice,
                userPriority,
            )
            withContext(Dispatchers.Main) {
                if (ticket != signGen) return@withContext
                signBytes.working = bytes
                signUi = ui
            }
        }
    }

    private fun operationInProgress(): Boolean = when (step) {
        MwaStep.Waiting, MwaStep.Finished -> false
        else -> true
    }

    private fun needsUnlock(model: WalletModel): Boolean =
        model.address == null && model.store.exists()
}
