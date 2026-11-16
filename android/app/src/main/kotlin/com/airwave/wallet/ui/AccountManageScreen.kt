/**
 * Manage page for a signing or watch account: full address, reveal, and remove.
 * Combined accounts use CombinedManageScreen. This file does not encrypt the vault.
 */
package com.airwave.wallet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import com.airwave.wallet.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airwave.wallet.exportAccountSecret
import com.airwave.wallet.removeAccount
import com.airwave.wallet.session.HomeDestination
import com.airwave.wallet.session.WalletModel
import com.airwave.wallet.theme.AirwaveColors
import com.airwave.wallet.vault.AccountKind
import com.airwave.wallet.vault.BurnerKeys

@Composable
internal fun AccountManageScreen(model: WalletModel) {
    val row = model.accountRows.firstOrNull { it.id == model.combinedEditId && it.kind != AccountKind.Combined }
    if (row == null) {
        model.pop()
        return
    }
    var confirmRemove by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .background(AirwaveColors.Bg)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        SubTopBar(model, "accounts.manage", showMenu = false)
        Text(row.label, color = AirwaveColors.Text, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
        Text(
            row.address,
            color = AirwaveColors.TextMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        if (row.canSign()) {
            ManageAction(model.t("accounts.revealPrivateKey"), danger = false) {
                model.push(HomeDestination.AccountReveal)
            }
        }
        ManageAction(model.t("accounts.removeWalletAccount"), danger = true) {
            confirmRemove = true
        }
    }
    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            text = { Text(model.t("accounts.removeConfirm")) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRemove = false
                    model.removeAccount(row.id)
                    model.combinedEditId = null
                    model.pop()
                }) { Text(model.t("accounts.removeMember"), color = AirwaveColors.Danger) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) { Text(model.t("common.cancel")) }
            },
        )
    }
}

@Composable
internal fun AccountRevealScreen(model: WalletModel) {
    val row = model.accountRows.firstOrNull { it.id == model.combinedEditId && it.canSign() }
    if (row == null) {
        model.pop()
        return
    }
    var password by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .background(AirwaveColors.Bg)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        SubTopBar(model, "accounts.revealTitle", showMenu = false)
        Text(
            "${row.label} · ${BurnerKeys.short(row.address)}",
            color = AirwaveColors.TextMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
        )
        val shown = secret
        val card = RoundedCornerShape(12.dp)
        if (shown == null) {
            Text(
                "••••••••••••••••",
                color = AirwaveColors.TextDim,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AirwaveColors.Fill, card)
                    .border(1.dp, AirwaveColors.Stroke, card)
                    .padding(12.dp),
            )
            Text(
                model.t("accounts.walletPassword"),
                color = AirwaveColors.TextDim,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            )
            BasicTextField(
                value = password,
                onValueChange = { password = it },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                textStyle = androidx.compose.ui.text.TextStyle(color = AirwaveColors.Text, fontSize = 15.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AirwaveColors.Bg, RoundedCornerShape(8.dp))
                    .border(1.dp, AirwaveColors.Stroke, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            )
            if (failed) {
                Text(model.t("password.wrong"), color = AirwaveColors.Danger, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            }
            Box(
                Modifier
                    .padding(top = 12.dp)
                    .size(34.dp)
                    .border(1.dp, AirwaveColors.AccentDim, RoundedCornerShape(8.dp))
                    .clickable {
                        val exported = model.exportAccountSecret(row.id, password)
                        failed = exported == null
                        secret = exported
                        if (exported != null) password = ""
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_stroke_eye),
                    contentDescription = model.t("accounts.revealPrivateKey"),
                    tint = AirwaveColors.Accent,
                    modifier = Modifier.size(18.dp),
                )
            }
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(AirwaveColors.Fill, card)
                    .border(1.dp, AirwaveColors.Stroke, card)
                    .padding(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    shown,
                    color = AirwaveColors.Text,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                )
                CopyIcon(shown, model.t("accounts.copyPrivateKey"), model.t("copy.copied"))
            }
            Text(
                model.t("accounts.revealWarn"),
                color = AirwaveColors.TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun ManageAction(label: String, danger: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Text(
        label,
        color = if (danger) Color(0xFFE08080) else AirwaveColors.Text,
        fontSize = 15.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(AirwaveColors.Fill, shape)
            .border(1.dp, if (danger) Color(0xFF8A4040) else AirwaveColors.Stroke, shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
    )
}
