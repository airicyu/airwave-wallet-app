package com.airwave.wallet.mwa
import com.airwave.wallet.session.WalletOperationRequest


/** Signals from the association that are not an approval step. */
sealed interface MwaEvent {
    /** No association signal is waiting. */
    data object None : MwaEvent

    /** The dApp session ended. */
    data object SessionTerminated : MwaEvent

    /** The phone is in a low-power state and the websocket has no connection. */
    data object LowPowerNoConnection : MwaEvent
}

/**
 * The one MWA screen step `MwaActivity` draws.
 * The request stays on the step until it is answered. The activity does not invent a second pending.
 */
sealed interface MwaStep {
    /** No user step. The activity shows the associate wait, or unlock if the vault is locked. */
    data object Waiting : MwaStep

    /** Connect is waiting for the vault to decrypt the signing key. */
    data class UnlockConnect(val request: WalletOperationRequest) : MwaStep

    /** Connect is waiting for the user to approve or reject. */
    data class ConnectApproval(val request: WalletOperationRequest) : MwaStep

    /** Reauthorize is waiting for the vault to decrypt the signing key. */
    data class UnlockReauthorize(val request: WalletOperationRequest) : MwaStep

    /** Sign message is waiting for the vault to decrypt the signing key. */
    data class UnlockSignMessage(val request: WalletOperationRequest) : MwaStep

    /** Sign message is waiting for the user to approve or reject. */
    data class SignMessageApproval(val request: WalletOperationRequest) : MwaStep

    /** Sign transaction is waiting for the vault to decrypt the signing key. */
    data class UnlockSignTransaction(val request: WalletOperationRequest) : MwaStep

    /** Sign transaction is waiting for the user to approve or reject. */
    data class SignTransactionApproval(val request: WalletOperationRequest) : MwaStep

    /** The account cannot sign. The activity shows this one sentence until teardown. */
    data class WatchBlocked(val message: String) : MwaStep

    /** The association ended. The activity finishes. */
    data object Finished : MwaStep
}
