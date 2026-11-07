/**
 * Draws the sign-message approval: centered title, site line, short address, and the message bytes.
 * MwaActivity shows this composable. It does not complete the walletlib request.
 */
package com.airwave.wallet.ui.mwa
import com.airwave.wallet.session.WalletModel
import com.airwave.wallet.session.approve
import com.airwave.wallet.session.reject
import com.airwave.wallet.session.WalletOperationRequest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airwave.wallet.theme.AirwaveColors

@Composable
internal fun SignMessageBody(model: WalletModel, pending: WalletOperationRequest) {
    Column(Modifier.fillMaxSize()) {
        ApprovalTop(
            title = model.t("mwa.signMessage.title"),
            backLabel = model.t("menu.back"),
            lockLabel = model.t("menu.lock"),
            onBack = { model.reject() },
            onLock = { model.lock() },
            centeredTitle = true,
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SiteLine(model, pending)
            Text(pending.accountShort, color = AirwaveColors.TextMuted)
            if (pending.messageHex != null) {
                Text(model.t("mwa.message.hex"), color = AirwaveColors.TextMuted)
                Text(pending.messageHex, color = AirwaveColors.Text)
            } else {
                Text(pending.messageUtf8.orEmpty(), color = AirwaveColors.Text)
            }
        }
        BottomPair(
            ghost = model.t("mwa.reject"),
            primary = model.t("mwa.approve"),
            enabled = !model.approving && !model.approvalSettled,
            onGhost = { model.reject() },
            onPrimary = { model.approve() },
        )
    }
}
