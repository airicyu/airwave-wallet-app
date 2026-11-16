/**
 * Draws the combined-account create flow and the member rows that sheet uses.
 * The manage menu is CombinedManageScreens. This file does not encrypt the vault.
 */
package com.airwave.wallet.account
import com.airwave.wallet.shell.SubTopBar
import com.airwave.wallet.ui.CopyIcon
import com.airwave.wallet.ui.AirwavePrimaryButton

import com.airwave.wallet.shell.HomeDestination
import com.airwave.wallet.session.WalletModel
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airwave.wallet.R
import com.airwave.wallet.account.appendCombinedMembers
import com.airwave.wallet.account.beginCombinedCreate
import com.airwave.wallet.account.commitCombinedCreate
import com.airwave.wallet.account.parseCombinedPaste
import com.airwave.wallet.account.removeAccount
import com.airwave.wallet.account.removeCombinedMember
import com.airwave.wallet.account.setCombinedCurrent
import com.airwave.wallet.theme.AirwaveColors
import com.airwave.wallet.vault.AccountKind
import com.airwave.wallet.vault.BurnerKeys

@Composable
internal fun CombinedCreateScreen(model: WalletModel) {
    val draft = model.combinedDraft ?: return
    AddAccountPage(
        model = model,
        titleKey = "accounts.screen.combined",
        cta = model.t("accounts.combinedCreate"),
        onCta = { model.commitCombinedCreate() },
        ctaEnabled = draft.members.isNotEmpty(),
    ) {
        OptionalNameField(model, draft.label) { model.combinedDraft = draft.copy(label = it) }
        if (draft.current.isNotEmpty()) {
            CurrentWalletCard(model, draft.current)
        }
        MemberSheet(
            model = model,
            members = draft.members,
            current = draft.current,
            onAdd = {
                model.combinedEditId = null
                model.push(HomeDestination.CombinedAdd)
            },
            onCurrent = { model.setCombinedCurrent(it) },
            onRemove = { model.removeCombinedMember(it) },
        )
    }
}

@Composable
internal fun CombinedAddScreen(model: WalletModel) {
    MenuPage(model, "common.add") {
        MenuRow(model.t("common.address")) { model.push(HomeDestination.CombinedPaste) }
        MenuRow(model.t("common.localAccounts")) { model.push(HomeDestination.CombinedLocal) }
    }
}

@Composable
internal fun CombinedPasteScreen(model: WalletModel) {
    var text by remember { mutableStateOf("") }
    val existing = targetMembers(model)
    val adding = parseCombinedPaste(text, existing)
    AddAccountPage(
        model = model,
        titleKey = "common.address",
        cta = model.t("common.confirm"),
        onCta = {
            model.appendCombinedMembers(adding)
            model.pop()
            model.pop()
        },
        ctaEnabled = adding.isNotEmpty(),
    ) {
        LabeledFillField(
            label = model.t("common.address"),
            value = text,
            onValue = { text = it },
            placeholder = model.t("accounts.pasteMulti"),
            singleLine = false,
            mono = true,
            minLines = 4,
        )
    }
}

@Composable
internal fun CombinedLocalScreen(model: WalletModel) {
    val existing = targetMembers(model)
    var picked by remember { mutableStateOf(listOf<String>()) }
    val locals = model.accountRows.filter { it.kind != AccountKind.Combined }
    AddAccountPage(
        model = model,
        titleKey = "common.localAccounts",
        cta = model.t("common.confirm"),
        onCta = {
            model.appendCombinedMembers(picked)
            model.pop()
            model.pop()
        },
        ctaEnabled = picked.isNotEmpty(),
    ) {
        Text(
            model.t("accounts.pickLocalToAdd"),
            color = AirwaveColors.TextMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        locals.forEach { row ->
            val on = row.address in picked
            val taken = row.address in existing
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !taken) {
                        picked = if (on) picked.filterNot { it == row.address } else picked + row.address
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = on && !taken,
                    onCheckedChange = null,
                    enabled = !taken,
                )
                Text(
                    row.label + if (taken) " · ${model.t("accounts.alreadyMember")}" else "",
                    color = if (taken) AirwaveColors.TextDim else AirwaveColors.Text,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                )
                Text(
                    BurnerKeys.short(row.address),
                    color = AirwaveColors.TextMuted,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

@Composable
internal fun CurrentWalletCard(model: WalletModel, address: String) {
    val local = model.accountRows.firstOrNull { it.address == address && it.kind != AccountKind.Combined }
    Text(
        model.t("accounts.currentWallet"),
        color = AirwaveColors.TextMuted,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
    )
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AirwaveColors.Fill)
            .border(1.dp, AirwaveColors.Accent, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (local != null) {
            Text(local.label, color = AirwaveColors.Text, fontSize = 14.sp)
        }
        AddressCopyLine(model, address)
    }
}

@Composable
internal fun MemberSheet(
    model: WalletModel,
    members: List<String>,
    current: String,
    onAdd: () -> Unit,
    onCurrent: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    Column(
        Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MemberSheetStroke, RoundedCornerShape(12.dp)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(model.t("accounts.members"), color = AirwaveColors.Text, fontSize = 14.sp)
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AirwaveColors.Accent)
                    .clickable(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_stroke_plus),
                    contentDescription = model.t("common.add"),
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        members.forEach { address ->
            MemberLine(
                model = model,
                address = address,
                current = current,
                showTrash = model.combinedEditId == null || members.size > 1,
                onCurrent = onCurrent,
                onTrash = { onRemove(address) },
            )
        }
    }
}

@Composable
internal fun MenuPage(model: WalletModel, titleKey: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(AirwaveColors.Bg)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        SubTopBar(model, titleKey, showMenu = false)
        content()
    }
}

@Composable
internal fun MenuRow(label: String, danger: Boolean = false, onClick: () -> Unit) {
    Text(
        label,
        color = if (danger) Color(0xFFE08080) else AirwaveColors.Text,
        fontSize = 15.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AirwaveColors.Fill)
            .border(1.dp, if (danger) Color(0xFF8A4040) else AirwaveColors.Stroke, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    )
}

@Composable
private fun MemberLine(
    model: WalletModel,
    address: String,
    current: String,
    showTrash: Boolean,
    onCurrent: (String) -> Unit,
    onTrash: () -> Unit,
) {
    val local = model.accountRows.firstOrNull { it.address == address && it.kind != AccountKind.Combined }
    val signing = local?.canSign() == true
    val selected = address == current
    Box(Modifier.fillMaxWidth().height(1.dp).background(MemberSheetStroke))
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MemberGrip()
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (selected) AirwaveColors.Accent else Color(0xFF1A222C))
                .border(1.dp, if (selected) AirwaveColors.Accent else Color(0xFF3A4654), CircleShape)
                .clickable { onCurrent(address) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_stroke_check),
                contentDescription = null,
                tint = if (selected) Color.White else AirwaveColors.TextDim.copy(alpha = 0.45f),
                modifier = Modifier.size(16.dp),
            )
        }
        if (signing) {
            Icon(
                painterResource(R.drawable.ic_stroke_key),
                contentDescription = null,
                tint = Color(0xFFD08B5B),
                modifier = Modifier.size(16.dp),
            )
        } else {
            Box(Modifier.size(16.dp))
        }
        Column(Modifier.weight(1f)) {
            val name = local?.label.orEmpty()
            if (name.isNotEmpty()) {
                Text(name, color = AirwaveColors.Text, fontSize = 14.sp, maxLines = 1)
            }
            AddressCopyLine(model, address)
        }
        if (showTrash) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AirwaveColors.Danger)
                    .clickable(onClick = onTrash),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_stroke_trash),
                    contentDescription = model.t("accounts.removeMember"),
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun AddressCopyLine(model: WalletModel, address: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            BurnerKeys.short(address),
            color = AirwaveColors.TextMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
        )
        CopyIcon(
            text = address,
            idleLabel = model.t("accounts.copy"),
            copiedLabel = model.t("copy.copied"),
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun MemberGrip() {
    Canvas(Modifier.size(width = 28.dp, height = 36.dp)) {
        val dot = 1.6.dp.toPx()
        val gapX = 5.dp.toPx()
        val gapY = 5.dp.toPx()
        val originX = (size.width - gapX) / 2f
        val originY = (size.height - gapY * 5) / 2f
        repeat(6) { row ->
            repeat(2) { col ->
                drawCircle(
                    color = AirwaveColors.TextMuted,
                    radius = dot,
                    center = Offset(originX + col * gapX, originY + row * gapY),
                )
            }
        }
    }
}

private val MemberSheetStroke = Color(0xFF4A5968)

private fun targetMembers(model: WalletModel): List<String> {
    val edit = model.combinedEditId
    if (edit != null) {
        return model.accountRows.firstOrNull { it.id == edit }?.members.orEmpty()
    }
    return model.combinedDraft?.members.orEmpty()
}
