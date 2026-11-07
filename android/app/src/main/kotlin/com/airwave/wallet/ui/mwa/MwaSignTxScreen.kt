/**
 * Draws the sign-and-send preview: expected SOL change, fee, and instruction names.
 * The method still declines before this screen is offered. The layout stays for that later approval.
 */
package com.airwave.wallet.ui.mwa
import com.airwave.wallet.session.WalletModel
import com.airwave.wallet.session.approve
import com.airwave.wallet.session.reject
import com.airwave.wallet.session.WalletOperationRequest

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airwave.wallet.holdings.BaseUnits
import com.airwave.wallet.theme.AirwaveColors
import java.math.BigInteger

@Composable
internal fun SignTxBody(model: WalletModel, pending: WalletOperationRequest) {
    var openDetails by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccountChip(pending, Modifier.weight(1f))
            LockButton(model.t("menu.lock")) { model.lock() }
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SiteLine(model, pending)
            Text(model.t("mwa.signTx.title"), color = AirwaveColors.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            AiStub(model)
            Text(model.t("mwa.expected"), color = AirwaveColors.Text, fontWeight = FontWeight.SemiBold)
            val amount = pending.amountLamports
            if (amount != null) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(AirwaveColors.Fill))
                    Text("SOL", color = AirwaveColors.Text, modifier = Modifier.weight(1f))
                    Text(
                        "−${BaseUnits.formatBaseUnits(amount, 9)} SOL",
                        color = AirwaveColors.Danger,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            } else {
                Text(model.t("mwa.expected.unavailable"), color = AirwaveColors.TextMuted)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(model.t("mwa.fee"), color = AirwaveColors.TextMuted)
                val fee = pending.feeLamports ?: BigInteger.ZERO
                Text("${BaseUnits.formatBaseUnits(fee, 9)} SOL", color = AirwaveColors.Text, fontWeight = FontWeight.SemiBold)
            }
            Text(
                "▸ ${model.t("mwa.details")}",
                color = AirwaveColors.Accent,
                modifier = Modifier
                    .clickable { openDetails = !openDetails }
                    .padding(vertical = 4.dp),
            )
            if (openDetails) {
                pending.instructionNames.forEach { name ->
                    Text(name, color = AirwaveColors.TextMuted)
                }
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
