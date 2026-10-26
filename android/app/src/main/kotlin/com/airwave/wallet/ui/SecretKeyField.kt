package com.airwave.wallet.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.airwave.wallet.settings.AirwaveSettings
import com.airwave.wallet.theme.AirwaveColors

/** Names which settings secret row the user is editing. */
enum class SecretFieldId { Helius, Jupiter, OpenRouter }

/** Records whether one secret-key field is idle, editing, or revealing a draft. */
data class SecretFieldState(
    val editing: Boolean = false,
    val revealed: Boolean = false,
    val draft: String = "",
)

@Composable
fun SecretKeyField(
    label: String,
    value: String,
    fieldState: SecretFieldState,
    optionalLabel: String,
    showLabel: String,
    editLabel: String,
    confirmLabel: String,
    discardLabel: String,
    onStartEdit: () -> Unit,
    onDraftChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDiscard: () -> Unit,
    onToggleReveal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val configured = value.isNotBlank()
    val borderColor = if (fieldState.editing) AirwaveColors.Accent else AirwaveColors.Stroke
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = AirwaveColors.TextDim)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = when {
                    fieldState.editing -> fieldState.draft
                    fieldState.revealed && configured -> value
                    configured -> AirwaveSettings.MASKED_SECRET_DISPLAY
                    else -> ""
                },
                onValueChange = { if (fieldState.editing) onDraftChange(it) },
                readOnly = !fieldState.editing,
                placeholder = { Text(optionalLabel) },
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                    .onFocusChanged { if (!it.isFocused && fieldState.editing) onDiscard() },
                visualTransformation = if (fieldState.editing || fieldState.revealed) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions.Default,
                singleLine = true,
            )
            if (fieldState.editing) {
                ConfirmDiscardActions(
                    confirmLabel = confirmLabel,
                    discardLabel = discardLabel,
                    onConfirm = onConfirm,
                    onDiscard = onDiscard,
                )
            } else {
                RevealEditActions(
                    showLabel = showLabel,
                    editLabel = editLabel,
                    revealed = fieldState.revealed,
                    revealEnabled = configured,
                    onToggleReveal = onToggleReveal,
                    onEdit = onStartEdit,
                )
            }
        }
    }
}
