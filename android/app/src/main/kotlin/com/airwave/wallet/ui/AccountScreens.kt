/**
 * Draws the wallet account list so the user can switch, rename, or delete an account.
 * The add-account chooser and inner forms live in AddAccountChooser.kt and AddAccountForms.kt.
 */
package com.airwave.wallet.ui
import com.airwave.wallet.session.WalletModel
import com.airwave.wallet.session.HomeDestination

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ripple
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airwave.wallet.R
import com.airwave.wallet.removeAccount
import com.airwave.wallet.renameAccount
import com.airwave.wallet.reorderAccounts
import com.airwave.wallet.setActiveAccountId
import com.airwave.wallet.theme.AirwaveColors
import com.airwave.wallet.vault.AccountKind
import com.airwave.wallet.vault.BurnerKeys

@Composable
internal fun AccountsScreen(model: WalletModel) {
    var renamingId by remember { mutableStateOf<String?>(null) }
    var renameDraft by remember { mutableStateOf("") }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dropLine by remember { mutableStateOf<AccountDropLine?>(null) }
    val cardBounds = remember { mutableStateMapOf<String, Rect>() }
    Column(
        Modifier
            .fillMaxSize()
            .background(AirwaveColors.Bg)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        SubTopBar(model, "menu.accounts", menuIcon = Icons.Default.MoreVert)
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                model.t("menu.accounts"),
                color = AirwaveColors.Text,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, AirwaveColors.Accent, RoundedCornerShape(10.dp))
                    .clickable { model.push(HomeDestination.AddAccount) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Add, contentDescription = model.t("accounts.add"), tint = AirwaveColors.Accent)
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = draggingId == null,
        ) {
            items(model.accountRows, key = { it.id }) { row ->
                val dragging = draggingId == row.id
                AccountListRow(
                    model = model,
                    label = row.label,
                    address = row.address,
                    watch = row.kind == AccountKind.Watch,
                    active = model.settings.activeAccountId == row.id,
                    renaming = renamingId == row.id,
                    renameDraft = renameDraft,
                    onRenameDraft = { renameDraft = it },
                    onSelect = {
                        model.setActiveAccountId(row.id)
                        model.popToHome()
                    },
                    onStartRename = {
                        renamingId = row.id
                        renameDraft = row.label
                    },
                    onConfirmRename = {
                        model.renameAccount(row.id, renameDraft)
                        renamingId = null
                    },
                    onDelete = { model.removeAccount(row.id) },
                    dragging = dragging,
                    dropBefore = dropLine?.id == row.id && dropLine?.before == true,
                    dropAfter = dropLine?.id == row.id && dropLine?.before == false,
                    onCardBounds = { cardBounds[row.id] = it },
                    onDragStart = { draggingId = row.id },
                    onDrag = { y ->
                        dropLine = cardBounds.entries.firstOrNull { (_, rect) -> y >= rect.top && y < rect.bottom }
                            ?.let { (id, rect) ->
                                AccountDropLine(id, before = y < rect.top + rect.height / 2f)
                            }
                    },
                    onDragEnd = {
                        val line = dropLine
                        val id = draggingId
                        draggingId = null
                        dropLine = null
                        if (id == null || line == null) return@AccountListRow
                        val next = model.accountRows.map { it.id }.filter { it != id }.toMutableList()
                        var index = next.indexOf(line.id)
                        if (index < 0) return@AccountListRow
                        if (!line.before) index += 1
                        next.add(index, id)
                        model.reorderAccounts(next, persist = true)
                    },
                )
            }
        }
    }
}

@Composable
private fun AccountListRow(
    model: WalletModel,
    label: String,
    address: String,
    watch: Boolean,
    active: Boolean,
    renaming: Boolean,
    renameDraft: String,
    onRenameDraft: (String) -> Unit,
    onSelect: () -> Unit,
    onStartRename: () -> Unit,
    onConfirmRename: () -> Unit,
    onDelete: () -> Unit,
    dragging: Boolean,
    dropBefore: Boolean,
    dropAfter: Boolean,
    onCardBounds: (Rect) -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(1200)
            copied = false
        }
    }
    var menuOpen by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    val mark = if (watch) R.drawable.ic_stroke_eye else R.drawable.ic_stroke_key
    val tint = if (watch) Color(0xFF6CB4E8) else Color(0xFFD08B5B)
    Column(Modifier.fillMaxWidth().zIndex(if (dragging) 1f else 0f)) {
        if (dropBefore) AccountDropLine()
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(AirwaveColors.BgElevated)
                .border(1.dp, if (active) AirwaveColors.Accent else Color(0xFF3A4654), shape)
                .onGloballyPositioned { onCardBounds(it.boundsInRoot()) }
                .alpha(if (dragging) 0.55f else 1f)
                .padding(end = 8.dp, top = 10.dp, bottom = 16.dp),
        ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DragHandle(
                label = model.t("accounts.reorder"),
                enabled = !renaming,
                onDragStart = onDragStart,
                onDrag = onDrag,
                onDragEnd = onDragEnd,
            )
            Icon(
                painterResource(mark),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.padding(end = 8.dp).size(16.dp),
            )
            Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).clickable(onClick = onSelect),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    label,
                    color = AirwaveColors.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 14.sp,
                )
                if (active) {
                    Text(
                        model.t("accounts.active"),
                        color = AirwaveColors.Accent,
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .align(Alignment.CenterVertically)
                            .border(1.dp, AirwaveColors.Accent, RoundedCornerShape(999.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
            }
            AccountSideButton(
                icon = painterResource(R.drawable.ic_stroke_rename),
                description = model.t("accounts.rename"),
                onClick = onStartRename,
            )
            Box {
                AccountSideButton(
                    icon = painterResource(R.drawable.ic_kebab),
                    description = model.t("accounts.delete"),
                    onClick = { menuOpen = true },
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(model.t("accounts.delete"), color = AirwaveColors.Danger) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
        Row(
            Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                BurnerKeys.short(address),
                color = AirwaveColors.TextMuted,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
            )
            if (copied) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = model.t("accounts.copy"),
                    tint = AirwaveColors.Ok,
                    modifier = Modifier.padding(start = 6.dp).size(14.dp),
                )
            } else {
                Icon(
                    painterResource(R.drawable.ic_stroke_copy),
                    contentDescription = model.t("accounts.copy"),
                    tint = AirwaveColors.TextMuted,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(14.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false, radius = 12.dp),
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("address", address))
                                copied = true
                            },
                        ),
                )
            }
        }
            }
        }
        }
        if (dropAfter) AccountDropLine()
        if (renaming) {
            OutlinedTextField(
                renameDraft,
                onRenameDraft,
                modifier = Modifier.fillMaxWidth().padding(start = 52.dp, top = 8.dp),
                singleLine = true,
            )
            TextButton(onClick = onConfirmRename, modifier = Modifier.padding(start = 44.dp)) {
                Text(model.t("common.confirm"), color = AirwaveColors.Accent)
            }
        }
    }
}

private data class AccountDropLine(val id: String, val before: Boolean)

@Composable
private fun AccountSideButton(
    icon: androidx.compose.ui.graphics.painter.Painter,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .padding(start = 6.dp)
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(AirwaveColors.Fill)
            .border(1.dp, AirwaveColors.Stroke, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = AirwaveColors.TextMuted, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun AccountDropLine() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .height(2.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(AirwaveColors.Accent),
    )
}

@Composable
private fun DragHandle(
    label: String,
    enabled: Boolean,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    val topInRoot = remember { mutableFloatStateOf(0f) }
    Canvas(
        Modifier
            .size(width = 28.dp, height = 44.dp)
            .semantics { contentDescription = label }
            .onGloballyPositioned { topInRoot.floatValue = it.boundsInRoot().top }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                    onDrag = { change, _ ->
                        change.consume()
                        onDrag(topInRoot.floatValue + change.position.y)
                    },
                )
            },
    ) {
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
