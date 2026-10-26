/**
 * Draws the wallet account list so the user can switch, rename, or delete an account.
 * The add-account chooser and inner forms live in AddAccountChooser.kt and AddAccountForms.kt.
 */
package com.airwave.wallet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.airwave.wallet.HomeDestination
import com.airwave.wallet.WalletModel
import com.airwave.wallet.removeAccount
import com.airwave.wallet.renameAccount
import com.airwave.wallet.setActiveAccountId
import com.airwave.wallet.theme.AirwaveColors
import com.airwave.wallet.vault.AccountKind
import com.airwave.wallet.vault.BurnerKeys

@Composable
internal fun AccountsScreen(model: WalletModel) {
    var renamingId by remember { mutableStateOf<String?>(null) }
    var renameDraft by remember { mutableStateOf("") }
    Column(
        Modifier
            .fillMaxSize()
            .background(AirwaveColors.Bg)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        SubTopBar(model, "menu.accounts")
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { model.push(HomeDestination.AddAccount) }) {
                Text(model.t("accounts.add"), color = AirwaveColors.Accent)
            }
        }
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            model.accountRows.forEach { row ->
                val active = model.settings.activeAccountId == row.id
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AirwaveColors.Fill)
                        .border(
                            width = if (active) 2.dp else 1.dp,
                            color = if (active) AirwaveColors.Accent else AirwaveColors.Stroke,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .clickable {
                            model.setActiveAccountId(row.id)
                            model.popToHome()
                        }
                        .padding(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (row.kind == AccountKind.Watch) Icons.Default.Visibility else Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = if (row.kind == AccountKind.Watch) AirwaveColors.TextMuted else Color(0xFFD08B5B),
                        )
                        Text(row.label, color = AirwaveColors.Text, modifier = Modifier.padding(start = 8.dp).weight(1f))
                        if (active) Text(model.t("accounts.active"), color = AirwaveColors.Accent, style = MaterialTheme.typography.labelSmall)
                    }
                    Text(
                        BurnerKeys.short(row.address),
                        color = AirwaveColors.TextMuted,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (renamingId == row.id) {
                        OutlinedTextField(renameDraft, { renameDraft = it }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                        TextButton(onClick = {
                            model.renameAccount(row.id, renameDraft)
                            renamingId = null
                        }) { Text(model.t("common.confirm"), color = AirwaveColors.Accent) }
                    } else {
                        Row(Modifier.padding(top = 8.dp)) {
                            TextButton(onClick = {
                                renamingId = row.id
                                renameDraft = row.label
                            }) { Text(model.t("accounts.rename"), color = AirwaveColors.TextMuted) }
                            TextButton(onClick = { model.removeAccount(row.id) }) {
                                Text(model.t("accounts.delete"), color = AirwaveColors.Danger)
                            }
                        }
                    }
                }
            }
        }
    }
}
