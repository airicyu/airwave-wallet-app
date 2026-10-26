/**
 * Draws the RPC endpoint list and URL draft rows on the settings stack.
 * It does not own cluster selection or the settings hub.
 */
package com.airwave.wallet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.airwave.wallet.WalletModel
import com.airwave.wallet.addRpcEndpoint
import com.airwave.wallet.isRpcEndpointActive
import com.airwave.wallet.removeRpcEndpoint
import com.airwave.wallet.replaceRpcEndpoint
import com.airwave.wallet.selectRpcEndpoint
import com.airwave.wallet.settings.Cluster
import com.airwave.wallet.settings.RpcResolver
import com.airwave.wallet.theme.AirwaveColors

@Composable
internal fun RpcScreen(model: WalletModel) {
    Column(
        Modifier
            .fillMaxSize()
            .background(AirwaveColors.Bg)
            .statusBarsPadding()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        SubTopBar(model, "settings.rpc")
        Cluster.entries.forEach { cluster ->
            RpcClusterCard(model, cluster)
        }
    }
}

@Composable
private fun RpcClusterCard(model: WalletModel, cluster: Cluster) {
    val entry = model.settings.rpcByCluster[cluster] ?: return
    val isCurrent = model.settings.cluster == cluster
    var adding by remember { mutableStateOf(false) }
    var addDraft by remember { mutableStateOf("") }
    var editingUrl by remember { mutableStateOf<String?>(null) }
    var editDraft by remember { mutableStateOf("") }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AirwaveColors.Fill)
            .border(1.dp, AirwaveColors.Stroke, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(
                when (cluster) {
                    Cluster.Devnet -> model.t("network.devnet")
                    Cluster.Mainnet -> model.t("network.mainnet")
                },
                color = AirwaveColors.TextMuted,
                style = MaterialTheme.typography.labelMedium,
            )
            if (isCurrent) Text(model.t("settings.rpcCurrent"), color = AirwaveColors.Accent, style = MaterialTheme.typography.labelSmall)
        }
        val builtIn = cluster == Cluster.Devnet
        val rows = buildList {
            if (builtIn) add(RpcResolver.PUBLIC_DEVNET)
            addAll(entry.urls.filter { it != RpcResolver.PUBLIC_DEVNET })
        }
        rows.forEach { url ->
            val isBuiltIn = builtIn && url == RpcResolver.PUBLIC_DEVNET
            val selected = model.isRpcEndpointActive(cluster, url, entry)
            val isEditing = editingUrl == url
            if (!isEditing) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) AirwaveColors.BgElevated else AirwaveColors.Fill)
                        .clickable { model.selectRpcEndpoint(cluster, url) }
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected, onClick = { model.selectRpcEndpoint(cluster, url) })
                    Text(
                        url,
                        color = AirwaveColors.Text,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (!isBuiltIn) {
                                    Modifier.clickable {
                                        editingUrl = url
                                        editDraft = url
                                        adding = false
                                    }
                                } else {
                                    Modifier
                                },
                            ),
                    )
                    Box(
                        Modifier.width(TrailingInlineActionsWidth),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        if (isBuiltIn) {
                            Text(
                                model.t("rpc.builtIn"),
                                color = AirwaveColors.TextDim,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        } else {
                            IconButton(
                                onClick = { model.removeRpcEndpoint(cluster, url) },
                                modifier = Modifier.semantics { contentDescription = model.t("rpc.remove") },
                            ) {
                                Icon(Icons.Default.Delete, model.t("rpc.remove"), tint = AirwaveColors.Danger)
                            }
                        }
                    }
                }
            } else {
                RpcUrlDraftRow(
                    model = model,
                    value = editDraft,
                    onValueChange = { editDraft = it },
                    onConfirm = {
                        if (model.replaceRpcEndpoint(cluster, url, editDraft)) {
                            editingUrl = null
                            editDraft = ""
                        }
                    },
                    onDiscard = {
                        editingUrl = null
                        editDraft = ""
                    },
                )
            }
        }
        if (adding) {
            RpcUrlDraftRow(
                model = model,
                value = addDraft,
                onValueChange = { addDraft = it },
                placeholder = model.t("rpc.urlPlaceholder"),
                onConfirm = {
                    if (model.addRpcEndpoint(cluster, addDraft)) {
                        adding = false
                        addDraft = ""
                    }
                },
                onDiscard = {
                    adding = false
                    addDraft = ""
                },
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(
                onClick = {
                    adding = true
                    editingUrl = null
                    addDraft = model.t("rpc.urlPlaceholder")
                },
                modifier = Modifier.semantics { contentDescription = model.t("rpc.add") },
            ) {
                Icon(Icons.Default.Add, model.t("rpc.add"), tint = AirwaveColors.TextMuted)
            }
        }
    }
}

@Composable
private fun RpcUrlDraftRow(
    model: WalletModel,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    onConfirm: () -> Unit,
    onDiscard: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(RpcRadioGutter))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        )
        ConfirmDiscardActions(
            confirmLabel = model.t("common.confirm"),
            discardLabel = model.t("common.discard"),
            onConfirm = onConfirm,
            onDiscard = onDiscard,
        )
    }
}
