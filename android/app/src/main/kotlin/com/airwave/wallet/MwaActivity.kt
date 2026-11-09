package com.airwave.wallet
import com.airwave.wallet.session.WalletModel
import com.airwave.wallet.session.reject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airwave.wallet.mwa.MwaHostViewModel
import com.airwave.wallet.mwa.MwaStep
import com.airwave.wallet.theme.AirwaveColors
import com.airwave.wallet.theme.AirwaveTheme
import androidx.activity.compose.BackHandler
import com.airwave.wallet.ui.mwa.ConnectBody
import com.airwave.wallet.ui.mwa.SignMessageBody
import com.airwave.wallet.ui.mwa.SignTxBody

/**
 * Dedicated MWA host Activity.
 * It shows the associate wait, then the authorize screen, and finish()es when the Scenario tears down.
 */
class MwaActivity : ComponentActivity() {
    private val session: MwaHostViewModel by viewModels()
    private val wallet: WalletModel by lazy { sharedWalletModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session.bindWallet(wallet)
        setContent {
            AirwaveTheme {
                val step = session.screen.step
                LaunchedEffect(step) {
                    if (step is MwaStep.Finished && !isFinishing) finish()
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .background(AirwaveColors.Bg),
                ) {
                    val vaultLocked = wallet.store.exists() && wallet.address == null
                    when (step) {
                        is MwaStep.UnlockConnect,
                        is MwaStep.UnlockReauthorize,
                        is MwaStep.UnlockSignMessage,
                        is MwaStep.UnlockSignTransaction,
                        -> UnlockScreen(wallet)
                        is MwaStep.ConnectApproval ->
                            ConnectBody(wallet, step.request)
                        is MwaStep.SignMessageApproval ->
                            SignMessageBody(wallet, step.request)
                        is MwaStep.SignTransactionApproval -> {
                            BackHandler { wallet.reject() }
                            SignTxBody(wallet, step.request, session.screen)
                        }
                        is MwaStep.WatchBlocked -> WatchNotice(step.message)
                        MwaStep.Finished,
                        MwaStep.Waiting,
                        -> if (vaultLocked) UnlockScreen(wallet) else AssociateWait()
                    }
                }
            }
        }
        val ok = session.processLaunch(intent)
        if (!ok) {
            Log.w(TAG, "Invalid launch intent; finishing activity")
            finish()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (!session.processLaunch(intent)) {
            Log.w(TAG, "Invalid new intent; finishing activity")
            finish()
        }
    }

    companion object {
        private val TAG = MwaActivity::class.simpleName
    }
}

@Composable
private fun WatchNotice(message: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(message, color = AirwaveColors.Text)
    }
}

@Composable
private fun AssociateWait() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AirwaveColors.Text)
    }
}
