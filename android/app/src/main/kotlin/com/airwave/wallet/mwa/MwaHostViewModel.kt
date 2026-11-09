package com.airwave.wallet.mwa
import com.airwave.wallet.session.WalletModel

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.solana.mobilewalletadapter.walletlib.association.AssociationUri
import com.solana.mobilewalletadapter.walletlib.association.LocalAssociationUri
import com.solana.mobilewalletadapter.walletlib.authorization.AuthIssuerConfig
import com.solana.mobilewalletadapter.walletlib.protocol.MobileWalletAdapterConfig
import com.solana.mobilewalletadapter.walletlib.scenario.AuthorizeRequest
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

/**
 * Owns one Mobile Wallet Adapter Scenario for this Activity.
 * Method handling and the screen step live on [MwaSession]. This class starts, replaces, and closes the association.
 */
class MwaHostViewModel(application: Application) : AndroidViewModel(application) {
    private val events = MutableStateFlow<MwaEvent>(MwaEvent.None)
    val mobileWalletAdapterServiceEvents = events.asSharedFlow()

    val screen = MwaSession()
    private var scenario: LocalScenario? = null
    private var dropTeardown = false

    fun bindWallet(model: WalletModel) {
        screen.bindWallet(model)
        screen.bindScope(viewModelScope)
    }

    fun processLaunch(intent: Intent?): Boolean {
        screen.noteLaunch()
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
        val previous = scenario
        scenario = null
        if (previous != null) {
            screen.abandonOperation()
            dropTeardown = true
            previous.close()
        }
        scenario = associationUri.createScenario(
            getApplication<Application>().applicationContext,
            MobileWalletAdapterConfig(
                10,
                10,
                arrayOf<Any>(MobileWalletAdapterConfig.LEGACY_TRANSACTION_VERSION, 0, 1),
                LOW_POWER_NO_CONNECTION_TIMEOUT_MS,
                arrayOf("solana:signMessages", "solana:signTransactions"),
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

    fun completeReauthorize(allowed: Boolean) {
        screen.completeReauthorize(allowed)
        events.value = MwaEvent.None
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
            if (dropTeardown) {
                dropTeardown = false
                return
            }
            viewModelScope.launch(Dispatchers.Main) {
                screen.markFinished()
            }
        }
        override fun onLowPowerAndNoConnection() {
            viewModelScope.launch {
                events.value = MwaEvent.LowPowerNoConnection
            }
        }

        override fun onAuthorizeRequest(request: AuthorizeRequest) {
            viewModelScope.launch(Dispatchers.Main) { screen.offerAuthorize(request) }
        }

        override fun onReauthorizeRequest(request: ReauthorizeRequest) {
            viewModelScope.launch(Dispatchers.Main) { screen.offerReauthorize(request) }
        }

        override fun onSignTransactionsRequest(request: SignTransactionsRequest) {
            viewModelScope.launch(Dispatchers.Main) { screen.offerSignTransaction(request) }
        }

        override fun onSignMessagesRequest(request: SignMessagesRequest) {
            viewModelScope.launch(Dispatchers.Main) { screen.offerSignMessage(request) }
        }

        override fun onSignAndSendTransactionsRequest(request: SignAndSendTransactionsRequest) {
            request.completeWithDecline()
        }

        override fun onDeauthorizedEvent(event: DeauthorizedEvent) {
            event.complete()
        }
    }

    companion object {
        private val TAG = MwaHostViewModel::class.simpleName
        private const val LOW_POWER_NO_CONNECTION_TIMEOUT_MS = 3000L
    }
}
