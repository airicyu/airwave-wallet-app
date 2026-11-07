/**
 * Shared rows for the MWA approval screens: top bar, account chip, site line, and the two bottom buttons.
 * A screen file owns its own middle content.
 */
package com.airwave.wallet.ui.mwa
import com.airwave.wallet.session.WalletModel
import com.airwave.wallet.session.WalletOperationRequest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import com.airwave.wallet.R
import com.airwave.wallet.theme.AirwaveColors
import com.airwave.wallet.ui.AirwavePrimaryButton

@Composable
internal fun ApprovalTop(
    title: String,
    backLabel: String,
    lockLabel: String,
    onBack: () -> Unit,
    onLock: () -> Unit,
    centeredTitle: Boolean = false,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = backLabel }) {
            Icon(painterResource(R.drawable.ic_stroke_back), backLabel, tint = AirwaveColors.Text)
        }
        Text(
            title,
            color = AirwaveColors.Text,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            fontSize = 16.sp,
            textAlign = if (centeredTitle) TextAlign.Center else TextAlign.Start,
        )
        LockButton(lockLabel, onLock)
    }
}

@Composable
internal fun LockButton(label: String, onLock: () -> Unit) {
    IconButton(onClick = onLock, modifier = Modifier.semantics { contentDescription = label }) {
        Icon(painterResource(R.drawable.ic_stroke_lock), label, tint = AirwaveColors.Text)
    }
}

@Composable
internal fun AccountChip(pending: WalletOperationRequest, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(AirwaveColors.Fill)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).background(AirwaveColors.BgElevated),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_stroke_key), contentDescription = null, tint = AirwaveColors.Text, modifier = Modifier.size(16.dp))
        }
        Column {
            if (pending.accountLabel.isNotBlank()) {
                Text(pending.accountLabel, color = AirwaveColors.Text, fontWeight = FontWeight.Medium)
            }
            Text(pending.accountShort, color = AirwaveColors.TextMuted)
        }
    }
}

@Composable
internal fun SiteLine(model: WalletModel, pending: WalletOperationRequest) {
    val site = pending.identityUri.ifBlank { pending.identityName }
    Text("${model.t("mwa.site")} $site", color = AirwaveColors.TextMuted)
}

@Composable
internal fun AiStub(model: WalletModel) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AirwaveColors.BgElevated)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(painterResource(R.drawable.ic_stroke_ai), contentDescription = null, tint = AirwaveColors.Text, modifier = Modifier.size(20.dp))
        Text(model.t("mwa.ai.label"), color = AirwaveColors.Text, modifier = Modifier.weight(1f))
        Text(model.t("mwa.ai.unknown"), color = AirwaveColors.TextMuted)
        IconButton(onClick = { }, modifier = Modifier.size(36.dp)) {
            Icon(painterResource(R.drawable.ic_stroke_report), contentDescription = null, tint = AirwaveColors.Accent)
        }
    }
}

@Composable
internal fun BottomPair(
    ghost: String,
    primary: String,
    enabled: Boolean,
    onGhost: () -> Unit,
    onPrimary: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = onGhost, enabled = enabled, modifier = Modifier.weight(1f)) {
            Text(ghost, color = AirwaveColors.Text)
        }
        AirwavePrimaryButton(primary, onPrimary, modifier = Modifier.weight(1f), enabled = enabled)
    }
}
