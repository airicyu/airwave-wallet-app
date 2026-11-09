/**
 * Draws the sign-message approval in the web popout order: account pill, site chip, title, payload card, raw bytes.
 * MwaActivity shows this composable. It does not complete the walletlib request.
 */
package com.airwave.wallet.ui.mwa
import com.airwave.wallet.session.WalletModel
import com.airwave.wallet.session.approve
import com.airwave.wallet.session.reject
import com.airwave.wallet.session.WalletOperationRequest

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airwave.wallet.R
import com.airwave.wallet.theme.AirwaveColors

@Composable
internal fun SignMessageBody(model: WalletModel, pending: WalletOperationRequest) {
    var openRaw by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AccountChip(model, pending)
            SiteLine(model, pending)
            Text(
                model.t("mwa.signMessage.title"),
                color = AirwaveColors.Text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                model.t("mwa.message.payload").uppercase(),
                color = AirwaveColors.TextDim,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(AirwaveColors.BgElevated, CardShape)
                    .border(1.dp, AirwaveColors.StrokeSoft, CardShape)
                    .padding(14.dp),
            ) {
                if (pending.messageUtf8 != null) {
                    Text(pending.messageUtf8, color = AirwaveColors.Text, fontSize = 14.sp)
                } else {
                    Text(model.t("mwa.message.hex"), color = AirwaveColors.TextMuted, fontSize = 12.sp)
                    Text(pending.messageHex.orEmpty(), color = AirwaveColors.Text, fontSize = 13.sp)
                }
            }
            Row(
                Modifier.clickable { openRaw = !openRaw },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    painterResource(R.drawable.ic_stroke_chevron),
                    contentDescription = null,
                    tint = AirwaveColors.TextMuted,
                    modifier = Modifier.size(14.dp).rotate(if (openRaw) 90f else 0f),
                )
                Text(model.t("mwa.message.raw"), color = AirwaveColors.TextMuted, fontSize = 13.sp)
            }
            if (openRaw) {
                Text(pending.messageHex.orEmpty(), color = AirwaveColors.TextMuted, fontSize = 12.sp)
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

private val CardShape = RoundedCornerShape(12.dp)
