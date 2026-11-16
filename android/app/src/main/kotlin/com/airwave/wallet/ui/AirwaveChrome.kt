/**
 * Draws shared Compose chrome such as the primary button, unlock backdrop, and secret-field colors.
 * It does not own routes or settings persistence.
 */
package com.airwave.wallet.ui
import com.airwave.wallet.shell.Tab

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.painterResource
import com.airwave.wallet.R
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.airwave.wallet.theme.AirwaveColors
import com.airwave.wallet.theme.airwaveUnlockBackground

/** Two 36dp confirm and discard buttons plus the 8dp gap between them. */
val TrailingInlineActionsWidth = 80.dp

/** Width of the radio column on RPC URL rows. Draft rows indent by this amount so they line up. */
val RpcRadioGutter = 48.dp

@Composable
fun ConfirmDiscardActions(
    confirmLabel: String,
    discardLabel: String,
    onConfirm: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .width(TrailingInlineActionsWidth)
            .fillMaxHeight(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DraftMarkButton(
            label = confirmLabel,
            icon = R.drawable.ic_stroke_check,
            tint = AirwaveColors.Ok,
            border = androidx.compose.ui.graphics.lerp(AirwaveColors.Stroke, AirwaveColors.Ok, 0.5f),
            onClick = onConfirm,
        )
        DraftMarkButton(
            label = discardLabel,
            icon = R.drawable.ic_stroke_x,
            tint = AirwaveColors.Danger,
            border = androidx.compose.ui.graphics.lerp(AirwaveColors.Stroke, AirwaveColors.Danger, 0.5f),
            onClick = onDiscard,
        )
    }
}

@Composable
private fun DraftMarkButton(
    label: String,
    icon: Int,
    tint: Color,
    border: Color,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .width(36.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(AirwaveColors.Fill)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), label, tint = tint, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun RevealEditActions(
    showLabel: String,
    editLabel: String,
    revealed: Boolean,
    revealEnabled: Boolean,
    onToggleReveal: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.width(96.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onToggleReveal,
            enabled = revealEnabled,
            modifier = Modifier.semantics { contentDescription = showLabel },
        ) {
            Icon(
                if (revealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                showLabel,
                tint = AirwaveColors.TextMuted,
            )
        }
        IconButton(
            onClick = onEdit,
            modifier = Modifier.semantics { contentDescription = editLabel },
        ) {
            Icon(Icons.Default.Edit, editLabel, tint = AirwaveColors.TextMuted)
        }
    }
}

@Composable
fun AirwaveUnlockBackdrop(
    centerContent: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .airwaveUnlockBackground()
,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 36.dp)
                .then(
                    if (centerContent) {
                        Modifier
                    } else {
                        Modifier.padding(top = 24.dp)
                    },
                ),
            verticalArrangement = if (centerContent) Arrangement.Center else Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

@Composable
fun airwaveUnlockFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AirwaveColors.Accent,
    unfocusedBorderColor = AirwaveColors.Stroke,
    focusedContainerColor = AirwaveColors.Fill,
    unfocusedContainerColor = AirwaveColors.Fill,
    cursorColor = AirwaveColors.Accent,
    focusedTextColor = AirwaveColors.Text,
    unfocusedTextColor = AirwaveColors.Text,
)

@Composable
fun AirwavePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AirwaveColors.Accent,
            contentColor = Color(0xFF061018),
            disabledContainerColor = AirwaveColors.Accent.copy(alpha = 0.45f),
            disabledContentColor = Color(0xFF061018).copy(alpha = 0.6f),
        ),
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HomeBottomTabs(
    selected: Tab,
    onAssets: () -> Unit,
    onActivity: () -> Unit,
    assetsLabel: String,
    activityLabel: String,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(AirwaveColors.BgElevated)
,
    ) {
        Row(Modifier.fillMaxWidth()) {
            homeTabCell(selected == Tab.Assets, assetsLabel, onAssets, Modifier.weight(1f))
            homeTabCell(selected == Tab.Activity, activityLabel, onActivity, Modifier.weight(1f))
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun homeTabCell(selected: Boolean, label: String, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .width(56.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    .background(AirwaveColors.Accent),
            )
        }
        Text(
            label,
            modifier = Modifier.padding(top = 8.dp),
            color = if (selected) AirwaveColors.Accent else AirwaveColors.TextMuted,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun PickRow(
    selected: Boolean,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = if (selected) AirwaveColors.Accent else AirwaveColors.Stroke,
                shape = RoundedCornerShape(8.dp),
            )
            .background(AirwaveColors.BgElevated)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .border(
                        width = 2.dp,
                        color = if (selected) AirwaveColors.Accent else AirwaveColors.Stroke,
                        shape = CircleShape,
                    ),
            )
            if (selected) {
                Box(
                    Modifier
                        .size(6.dp)
                        .background(AirwaveColors.Accent, CircleShape),
                )
            }
        }
        Text(title, color = AirwaveColors.Text, style = MaterialTheme.typography.bodyLarge)
    }
}
