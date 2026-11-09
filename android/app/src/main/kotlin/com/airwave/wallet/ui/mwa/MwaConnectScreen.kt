/**
 * Draws the connect approval in the web popout order: title, site tile, link, account, then Reject and Connect.
 * MwaActivity shows this composable. It does not complete the walletlib request.
 */
package com.airwave.wallet.ui.mwa
import com.airwave.wallet.session.WalletModel
import com.airwave.wallet.session.approve
import com.airwave.wallet.session.reject
import com.airwave.wallet.session.WalletOperationRequest

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airwave.wallet.activeAccount
import com.airwave.wallet.R
import com.airwave.wallet.theme.AirwaveColors
import com.airwave.wallet.vault.BurnerKeys
import java.net.URI

@Composable
internal fun ConnectBody(model: WalletModel, pending: WalletOperationRequest) {
    BackHandler { model.reject() }
    val siteName = siteLabel(pending)
    val accountName = pending.accountLabel.ifBlank { model.activeAccount()?.label.orEmpty() }
    val accountShort = pending.accountShort.ifBlank { BurnerKeys.short(model.address.orEmpty()) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { model.reject() },
                modifier = Modifier.semantics { contentDescription = model.t("menu.back") },
            ) {
                Icon(painterResource(R.drawable.ic_stroke_back), model.t("menu.back"), tint = AirwaveColors.Text)
            }
            Text(
                model.t("mwa.connect.title"),
                color = AirwaveColors.Text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Box(Modifier.width(48.dp))
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(AirwaveColors.Fill)
                    .border(1.dp, AirwaveColors.Stroke, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(letter(siteName), color = AirwaveColors.Text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                siteName,
                color = AirwaveColors.Text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
            if (pending.identityUri.isNotEmpty()) {
                Text(pending.identityUri, color = AirwaveColors.TextMuted, fontSize = 12.sp)
            }
            Box(Modifier.padding(top = 8.dp).width(2.dp).height(16.dp).background(AirwaveColors.TextDim))
            Box(Modifier.size(8.dp).clip(CircleShape).background(AirwaveColors.Accent))
            Box(Modifier.width(2.dp).height(16.dp).background(AirwaveColors.TextDim))
            Box(Modifier.height(10.dp))
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(AirwaveColors.Accent),
                contentAlignment = Alignment.Center,
            ) {
                Text(letter(accountName), color = AirwaveColors.Bg, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                accountName,
                color = AirwaveColors.Text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(accountShort, color = AirwaveColors.TextMuted, fontSize = 13.sp)
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

private fun siteLabel(pending: WalletOperationRequest): String {
    if (pending.identityName.isNotBlank()) return pending.identityName
    val host = runCatching { URI(pending.identityUri).host }.getOrNull()
    if (!host.isNullOrBlank()) return host
    return pending.identityUri
}
