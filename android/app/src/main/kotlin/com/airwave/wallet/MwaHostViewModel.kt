package com.airwave.wallet

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.airwave.wallet.vault.BurnerKeys
import com.solana.mobilewalletadapter.common.ProtocolContract
import com.solana.mobilewalletadapter.walletlib.association.AssociationUri
import com.solana.mobilewalletadapter.walletlib.association.LocalAssociationUri
import com.solana.mobilewalletadapter.walletlib.authorization.AuthIssuerConfig
import com.solana.mobilewalletadapter.walletlib.protocol.MobileWalletAdapterConfig
import com.solana.mobilewalletadapter.walletlib.scenario.AuthorizeRequest
import com.solana.mobilewalletadapter.walletlib.scenario.AuthorizedAccount
import com.solana.mobilewalletadapter.walletlib.scenario.DeauthorizedEvent
import com.solana.mobilewalletadapter.walletlib.scenario.LocalScenario
import com.solana.mobilewalletadapter.walletlib.scenario.ReauthorizeRequest
import com.solana.mobilewalletadapter.walletlib.scenario.SignAndSendTransactionsRequest
import com.solana.mobilewalletadapter.walletlib.scenario.SignMessagesRequest
import com.solana.mobilewalletadapter.walletlib.scenario.SignTransactionsRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters

/**
 * Activity-scoped MWA session host, copied from FakeWallet's MobileWalletAdapterViewModel.
 * It owns the Scenario, exposes one request at a time, and does not own the vault. WalletModel still holds unlock key material.
 */
class MwaHostViewModel(application: Application) : AndroidViewModel(application) {
    private val events = MutableStateFlow<MwaEvent>(MwaEvent.None)
    val mobileWalletAdapterServiceEvents = events.asSharedFlow()

    private var scenario: LocalScenario? = null

    fun processLaunch(intent: Intent?): Boolean {
        val data = intent?.data
        if (data == null) {
            Log.e(TAG, "Intent has no data URI")
            return false
        }
        val associationUri = AssociationUri.parse(data)
        if (associationUri == null) {
            Log.e(TAG, "Unsupported association URI '$data'")
            return false
        }
        if (associationUri !is LocalAssociationUri) {
            Log.w(TAG, "Remote association is not supported")
            return false
        }
        scenario = associationUri.createScenario(
            getApplication<Application>().applicationContext,
            MobileWalletAdapterConfig(
                10,
                10,
                arrayOf(MobileWalletAdapterConfig.LEGACY_TRANSACTION_VERSION, 0),
                LOW_POWER_NO_CONNECTION_TIMEOUT_MS,
                arrayOf(
                    ProtocolContract.FEATURE_ID_SIGN_TRANSACTIONS,
                    ProtocolContract.FEATURE_ID_SIGN_IN_WITH_SOLANA,
                ),
            ),
            AuthIssuerConfig("Airwave"),
            Callbacks(),
        ).also { it.start() }
        return true
    }

    override fun onCleared() {
        scenario?.close()
        scenario = null
    }

    fun authorizeDapp(request: AuthorizeRequest, authorized: Boolean, publicKey: ByteArray?, label: String) {
        viewModelScope.launch {
            if (!authorized || publicKey == null) {
                request.completeWithDecline()
                return@launch
            }
            val account = AuthorizedAccount(
                publicKey,
                BurnerKeys.base58(publicKey),
                "base58",
                label,
                null,
                null,
                null,
            )
            request.completeWithAuthorize(arrayOf(account), null, "web".toByteArray(), null)
        }
    }

    private inner class Callbacks : LocalScenario.Callbacks {
        override fun onScenarioReady() = Unit
        override fun onScenarioServingClients() = Unit
        override fun onScenarioServingComplete() {
            viewModelScope.launch(Dispatchers.Main) {
                scenario?.close()
                events.value = MwaEvent.None
            }
        }
        override fun onScenarioComplete() = Unit
        override fun onScenarioError() = Unit
        override fun onScenarioTeardownComplete() {
            viewModelScope.launch {
                events.value = MwaEvent.SessionTerminated
            }
        }
        override fun onLowPowerAndNoConnection() {
            viewModelScope.launch {
                events.value = MwaEvent.LowPowerNoConnection
            }
        }

        override fun onAuthorizeRequest(request: AuthorizeRequest) {
            events.value = MwaEvent.AuthorizeDapp(request)
        }

        override fun onReauthorizeRequest(request: ReauthorizeRequest) {
            request.completeWithReauthorize()
        }

        override fun onSignTransactionsRequest(request: SignTransactionsRequest) {
            request.completeWithDecline()
        }

        override fun onSignMessagesRequest(request: SignMessagesRequest) {
            request.completeWithDecline()
        }

        override fun onSignAndSendTransactionsRequest(request: SignAndSendTransactionsRequest) {
            request.completeWithDecline()
        }

        override fun onDeauthorizedEvent(event: DeauthorizedEvent) {
            event.complete()
        }
    }

    sealed interface MwaEvent {
        data object None : MwaEvent
        data object SessionTerminated : MwaEvent
        data object LowPowerNoConnection : MwaEvent
        data class AuthorizeDapp(val request: AuthorizeRequest) : MwaEvent
    }

    companion object {
        private val TAG = MwaHostViewModel::class.simpleName
        private const val LOW_POWER_NO_CONNECTION_TIMEOUT_MS = 3000L
    }
}

fun publicKeyFromSeed(seed: ByteArray): ByteArray =
    Ed25519PrivateKeyParameters(seed, 0).generatePublicKey().encoded
