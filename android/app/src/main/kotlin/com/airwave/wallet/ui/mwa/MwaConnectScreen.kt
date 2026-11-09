/**
 * Draws the connect approval with a letter tile, the dApp name, and the account chip.
 * The live connect page on MwaActivity is a separate composable. This one is the shell route.
 */
package com.airwave.wallet.ui.mwa
import com.airwave.wallet.session.WalletModel
import com.airwave.wallet.session.approve
import com.airwave.wallet.session.reject
import com.airwave.wallet.session.WalletOperationRequest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airwave.wallet.theme.AirwaveColors

@Composable
internal fun ConnectBody(model: WalletModel, pending: WalletOperationRequest) {
    Column(Modifier.fillMaxSize()) {
        ApprovalTop(
            title = model.t("mwa.connect.title"),
            backLabel = model.t("menu.back"),
            lockLabel = model.t("menu.lock"),
            onBack = { model.reject() },
            onLock = { model.lock() },
        )
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AirwaveColors.Fill),
                contentAlignment = Alignment.Center,
            ) {
                Text(letter(pending.identityName), color = AirwaveColors.Text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            Text(pending.identityName.ifBlank { pending.identityUri }, color = AirwaveColors.Text, fontWeight = FontWeight.SemiBold)
            Text(pending.identityUri, color = AirwaveColors.TextMuted)
            Box(Modifier.fillMaxWidth().height(24.dp).background(AirwaveColors.StrokeSoft))
            AccountChip(model, pending)
        }
        BottomPair(
            ghost = model.t("mwa.reject"),
            primary = model.t("mwa.connect.action"),
            enabled = !model.approving && !model.approvalSettled,
            onGhost = { model.reject() },
            onPrimary = { model.approve() },
        )
    }
}

internal fun letter(name: String): String {
    val ch = name.firstOrNull { !it.isWhitespace() } ?: return "A"
    return ch.uppercase()
}
