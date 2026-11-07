package com.airwave.wallet
import com.airwave.wallet.session.WalletModel
import com.airwave.wallet.session.approve
import com.airwave.wallet.session.reject
import com.airwave.wallet.session.WalletOperationRequest

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.airwave.wallet.ui.AirwavePrimaryButton
import com.airwave.wallet.ui.mwa.SignMessageBody

/**
 * Dedicated MWA host Activity.
 * It shows the associate wait, then the authorize screen, and finish()es when the Scenario tears down.
 */
class MwaActivity : ComponentActivity() {
    private val session: MwaHostViewModel by viewModels()
    private val wallet: WalletModel by lazy { sharedWalletModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                        .background(AirwaveColors.Bg),
                ) {
                    val vaultLocked = wallet.store.exists() && wallet.address == null
                    when (step) {
                        is MwaStep.UnlockConnect,
                        is MwaStep.UnlockReauthorize,
                        is MwaStep.UnlockSignMessage,
                        -> UnlockScreen(wallet)
                        is MwaStep.ConnectApproval ->
                            ConnectApproval(wallet, step.request)
                        is MwaStep.SignMessageApproval ->
                            SignMessageBody(wallet, step.request)
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

@Composable
private fun ConnectApproval(wallet: WalletModel, approval: WalletOperationRequest) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(approval.identityName, color = AirwaveColors.Text)
        Text(approval.identityUri, color = AirwaveColors.TextMuted)
        Spacer(Modifier.height(24.dp))
        AirwavePrimaryButton(wallet.t("mwa.connect.action"), { wallet.approve() })
        Spacer(Modifier.height(12.dp))
        AirwavePrimaryButton(wallet.t("mwa.reject"), { wallet.reject() })
    }
}
