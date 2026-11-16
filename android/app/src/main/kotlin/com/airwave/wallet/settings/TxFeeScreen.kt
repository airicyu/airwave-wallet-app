/**
 * Draws the transaction-fee settings page: v1 priority fee and legacy CU price.
 * A row writes only when its confirm control is pressed.
 */
package com.airwave.wallet.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import com.airwave.wallet.ui.ConfirmDiscardActions
import com.airwave.wallet.session.WalletModel
import com.airwave.wallet.shell.SubTopBar
import com.airwave.wallet.theme.AirwaveColors

@Composable
internal fun TxFeeScreen(model: WalletModel) {
    Column(
        Modifier
            .fillMaxSize()
            .background(AirwaveColors.Bg)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SubTopBar(model, "settings.txFee")
        FeeField(
            label = model.t("settings.fee.priority"),
            saved = model.settings.defaultPriorityFeeLamports,
            confirm = model.t("common.confirm"),
            discard = model.t("common.discard"),
        ) { raw ->
            val parsed = AirwaveSettings.parsePriorityFeeLamports(raw) ?: return@FeeField false
            if (parsed != model.settings.defaultPriorityFeeLamports) {
                model.updateSettings { it.copy(defaultPriorityFeeLamports = parsed) }
            }
            true
        }
        FeeField(
            label = model.t("settings.fee.cuPrice"),
            saved = model.settings.defaultCuPrice.toString(),
            confirm = model.t("common.confirm"),
            discard = model.t("common.discard"),
        ) { raw ->
            val parsed = raw.trim().toLongOrNull()
            if (parsed == null || parsed !in 0L..1_000_000_000L) return@FeeField false
            if (parsed != model.settings.defaultCuPrice) {
                model.updateSettings { it.copy(defaultCuPrice = parsed) }
            }
            true
        }
    }
}

@Composable
private fun FeeField(
    label: String,
    saved: String,
    confirm: String,
    discard: String,
    onCommit: (String) -> Boolean,
) {
    var draft by remember(saved) { mutableStateOf(saved) }
    val dirty = draft != saved
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = AirwaveColors.TextMuted)
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                draft,
                { draft = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            if (dirty) {
                ConfirmDiscardActions(
                    confirmLabel = confirm,
                    discardLabel = discard,
                    onConfirm = { if (!onCommit(draft)) draft = saved },
                    onDiscard = { draft = saved },
                )
            }
        }
    }
}
