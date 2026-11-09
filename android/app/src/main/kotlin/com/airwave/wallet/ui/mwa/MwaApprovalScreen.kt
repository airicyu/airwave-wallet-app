/**
 * Picks the MWA approval screen for the in-memory request on the shell route.
 * Each kind is drawn by its own screen. This file does not talk to walletlib.
 */
package com.airwave.wallet.ui.mwa
import com.airwave.wallet.session.WalletModel
import com.airwave.wallet.session.WalletOperationKind

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.airwave.wallet.theme.AirwaveColors

@Composable
fun MwaApprovalScreen(model: WalletModel) {
    val pending = model.walletOperationRequest ?: return
    Column(
        Modifier
            .fillMaxSize()
            .background(AirwaveColors.Bg)
,
    ) {
        when (pending.kind) {
            WalletOperationKind.Authorize -> ConnectBody(model, pending)
            WalletOperationKind.Reauthorize -> Unit
            WalletOperationKind.SignMessage -> SignMessageBody(model, pending)
            WalletOperationKind.SignTransaction -> Unit
            WalletOperationKind.SignAndSend -> Unit
        }
    }
}
