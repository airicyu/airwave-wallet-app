/**
 * Draws the combined-account manage menu and the member sheet for an existing row.
 * Create and add stay in CombinedScreens. This file does not encrypt the vault.
 */
package com.airwave.wallet.account
import com.airwave.wallet.shell.SubTopBar
import com.airwave.wallet.ui.CopyIcon
import com.airwave.wallet.ui.AirwavePrimaryButton

import com.airwave.wallet.shell.HomeDestination
import com.airwave.wallet.session.WalletModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airwave.wallet.account.removeAccount
import com.airwave.wallet.account.removeCombinedMember
import com.airwave.wallet.account.setCombinedCurrent
import com.airwave.wallet.theme.AirwaveColors
import com.airwave.wallet.vault.AccountKind
import com.airwave.wallet.vault.BurnerKeys

@Composable
internal fun CombinedManageScreen(model: WalletModel) {
    val row = model.accountRows.firstOrNull { it.id == model.combinedEditId && it.kind == AccountKind.Combined }
    if (row == null) {
        model.pop()
        return
    }
    MenuPage(model, "accounts.manageTitle") {
        Text(row.label, color = AirwaveColors.Text, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
        Text(
            BurnerKeys.short(row.address),
            color = AirwaveColors.TextMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        MenuRow(model.t("accounts.manageCombined")) { model.push(HomeDestination.CombinedMembers) }
        MenuRow(model.t("accounts.removeCombined"), danger = true) {
            model.removeAccount(row.id)
            model.combinedEditId = null
            model.pop()
        }
    }
}

@Composable
internal fun CombinedMembersScreen(model: WalletModel) {
    val row = model.accountRows.firstOrNull { it.id == model.combinedEditId && it.kind == AccountKind.Combined }
    if (row == null) {
        model.pop()
        return
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(AirwaveColors.Bg)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        SubTopBar(model, "accounts.manage", showMenu = false)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            CurrentWalletCard(model, row.address)
            MemberSheet(
                model = model,
                members = row.members,
                current = row.address,
                onAdd = { model.push(HomeDestination.CombinedAdd) },
                onCurrent = { model.setCombinedCurrent(it) },
                onRemove = { model.removeCombinedMember(it) },
            )
        }
    }
}
