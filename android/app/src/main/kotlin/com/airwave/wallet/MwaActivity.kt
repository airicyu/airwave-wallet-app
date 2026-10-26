package com.airwave.wallet

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airwave.wallet.theme.AirwaveColors
import com.airwave.wallet.theme.AirwaveTheme
import com.airwave.wallet.ui.AirwavePrimaryButton

/**
 * Dedicated MWA host Activity, the same role as FakeWallet's MobileWalletAdapterActivity.
 * It shows the associate wait, then the authorize screen, and finish()es when the Scenario tears down.
 */
class MwaActivity : ComponentActivity() {
    private val session: MwaHostViewModel by viewModels()
    private val wallet: WalletModel by lazy { sharedWalletModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AirwaveTheme {
                val event by session.mobileWalletAdapterServiceEvents.collectAsState(
                    MwaHostViewModel.MwaEvent.None,
                )
                LaunchedEffect(event) {
                    if (event is MwaHostViewModel.MwaEvent.SessionTerminated && !isFinishing) {
                        finish()
                    }
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(AirwaveColors.Bg),
                ) {
                    val authorize = event as? MwaHostViewModel.MwaEvent.AuthorizeDapp
                    when {
                        wallet.route == Route.Unlock ||
                            (wallet.store.exists() && wallet.address == null) ->
                            UnlockScreen(wallet)
                        authorize != null ->
                            FakeWalletAuthorize(wallet, session, authorize)
                        else -> AssociateWait()
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

    companion object {
        private val TAG = MwaActivity::class.simpleName
    }
}

@Composable
private fun AssociateWait() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AirwaveColors.Text)
    }
}

@Composable
private fun FakeWalletAuthorize(
    wallet: WalletModel,
    session: MwaHostViewModel,
    event: MwaHostViewModel.MwaEvent.AuthorizeDapp,
) {
    val request = event.request
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(request.identityName ?: "", color = AirwaveColors.Text)
        Text(request.identityUri?.toString() ?: "", color = AirwaveColors.TextMuted)
        Spacer(Modifier.height(24.dp))
        AirwavePrimaryButton(
            wallet.t("mwa.connect.action"),
            {
                val seed = wallet.signingSeed
                val pub = seed?.let { publicKeyFromSeed(it) }
                session.authorizeDapp(
                    request,
                    pub != null,
                    pub,
                    wallet.activeAccount()?.label ?: "",
                )
            },
        )
        Spacer(Modifier.height(12.dp))
        AirwavePrimaryButton(
            wallet.t("mwa.reject"),
            { session.authorizeDapp(request, false, null, "") },
        )
    }
}
