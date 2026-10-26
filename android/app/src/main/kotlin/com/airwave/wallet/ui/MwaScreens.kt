/**
 * Draws the full-screen chrome for MWA connect, sign-message, and sign-and-send.
 * WalletModel owns the pending request. This file does not talk to walletlib.
 */
package com.airwave.wallet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airwave.wallet.Pending
import com.airwave.wallet.PendingKind
import com.airwave.wallet.R
import com.airwave.wallet.WalletModel
import com.airwave.wallet.holdings.BaseUnits
import com.airwave.wallet.theme.AirwaveColors
import java.math.BigInteger

@Composable
fun MwaApprovalScreen(model: WalletModel) {
    val pending = model.pending ?: return
    Column(
        Modifier
            .fillMaxSize()
            .background(AirwaveColors.Bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        when (pending.kind) {
            PendingKind.Authorize -> ConnectBody(model, pending)
            PendingKind.SignMessage -> SignMessageBody(model, pending)
            PendingKind.SignAndSend -> SignTxBody(model, pending)
        }
    }
}

@Composable
private fun ConnectBody(model: WalletModel, pending: Pending) {
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
            AccountChip(pending)
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

@Composable
private fun SignMessageBody(model: WalletModel, pending: Pending) {
    Column(Modifier.fillMaxSize()) {
        ApprovalTop(
            title = model.t("mwa.signMessage.title"),
            backLabel = model.t("menu.back"),
            lockLabel = model.t("menu.lock"),
            onBack = { model.reject() },
            onLock = { model.lock() },
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

@Composable
private fun SignTxBody(model: WalletModel, pending: Pending) {
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

@Composable
private fun ApprovalTop(title: String, backLabel: String, lockLabel: String, onBack: () -> Unit, onLock: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = backLabel }) {
            Icon(painterResource(R.drawable.ic_stroke_back), backLabel, tint = AirwaveColors.Text)
        }
        Text(title, color = AirwaveColors.Text, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), fontSize = 16.sp)
        LockButton(lockLabel, onLock)
    }
}

@Composable
private fun LockButton(label: String, onLock: () -> Unit) {
    IconButton(onClick = onLock, modifier = Modifier.semantics { contentDescription = label }) {
        Icon(painterResource(R.drawable.ic_stroke_lock), label, tint = AirwaveColors.Text)
    }
}

@Composable
private fun AccountChip(pending: Pending, modifier: Modifier = Modifier) {
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
private fun SiteLine(model: WalletModel, pending: Pending) {
    val site = pending.identityUri.ifBlank { pending.identityName }
    Text("${model.t("mwa.site")} $site", color = AirwaveColors.TextMuted)
}

@Composable
private fun AiStub(model: WalletModel) {
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
private fun BottomPair(
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

private fun letter(name: String): String {
    val ch = name.firstOrNull { !it.isWhitespace() } ?: return "A"
    return ch.uppercase()
}
