package com.airwave.wallet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.airwave.wallet.HomeDestination
import com.airwave.wallet.Tab
import com.airwave.wallet.WalletModel
import com.airwave.wallet.settings.AirwaveSettings
import com.airwave.wallet.settings.Cluster
import com.airwave.wallet.settings.LocaleTag
import com.airwave.wallet.settings.ModelMode
import com.airwave.wallet.settings.RpcResolver
import com.airwave.wallet.theme.AirwaveColors
import com.airwave.wallet.vault.BurnerKeys

@Composable
fun HomeShell(model: WalletModel) {
    val top = model.homeStack.lastOrNull()
    if (top == null) {
        HomeTabs(model)
    } else {
        when (top) {
            HomeDestination.EmptyAccounts -> EmptyScreen(model, "menu.accounts", "empty.accounts")
            HomeDestination.EmptyAddressBook -> EmptyScreen(model, "menu.addressBook", "empty.addressBook")
            HomeDestination.EmptyConnected -> EmptyScreen(model, "menu.connectedSites", "empty.connectedSites")
            HomeDestination.EmptyAbout -> EmptyScreen(model, "menu.about", "empty.about")
            HomeDestination.Network -> NetworkScreen(model)
            HomeDestination.SettingsHub -> SettingsHubScreen(model)
            HomeDestination.Language -> LanguageScreen(model)
            HomeDestination.Rpc -> RpcScreen(model)
            HomeDestination.ApiKeys -> ApiKeysScreen(model)
            HomeDestination.AiSettings -> AiSettingsScreen(model)
            HomeDestination.CuPrice -> CuPriceScreen(model)
            HomeDestination.WalletLockSettings -> WalletLockSettingsScreen(model)
        }
    }
    if (model.menuOpen) {
        HamburgerMenu(model)
    }
}

@Composable
private fun HomeTabs(model: WalletModel) {
    Column(Modifier.fillMaxSize().background(AirwaveColors.Bg).statusBarsPadding()) {
        Column(
            Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (model.tab == Tab.Assets) model.t("home.assets") else model.t("home.activity"),
                    style = MaterialTheme.typography.headlineLarge,
                    color = AirwaveColors.Text,
                )
                IconButton(
                    onClick = { model.menuOpen = true },
                    modifier = Modifier.semantics { contentDescription = model.t("menu.menu") },
                ) {
                    Icon(
                        Icons.Default.Menu,
                        model.t("menu.menu"),
                        tint = AirwaveColors.Accent,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            val address = model.address.orEmpty()
            Text(model.t("home.wallet"), color = AirwaveColors.TextMuted, style = MaterialTheme.typography.bodyMedium)
            Text(
                BurnerKeys.short(address),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = AirwaveColors.Text,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                if (model.tab == Tab.Assets) model.t("home.noAssets") else model.t("home.noActivity"),
                color = AirwaveColors.TextMuted,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        HomeBottomTabs(
            selected = model.tab,
            onAssets = { model.tab = Tab.Assets },
            onActivity = { model.tab = Tab.Activity },
            assetsLabel = model.t("home.assets"),
            activityLabel = model.t("home.activity"),
        )
    }
}

@Composable
private fun HamburgerMenu(model: WalletModel) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(AirwaveColors.Bg.copy(alpha = 0.72f))
                .clickable { model.menuOpen = false },
        )
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(AirwaveColors.BgElevated)
                .padding(bottom = 8.dp),
        ) {
            MenuSheetTopBar(model)
            HorizontalDivider(color = AirwaveColors.StrokeSoft)
            menuRow(model, "menu.accounts", Icons.Default.People) { model.push(HomeDestination.EmptyAccounts) }
            menuRow(model, "menu.addressBook", Icons.Default.MenuBook) { model.push(HomeDestination.EmptyAddressBook) }
            menuRow(model, "menu.network", Icons.Default.Public) { model.push(HomeDestination.Network) }
            menuRow(model, "menu.settings", Icons.Default.Settings) { model.push(HomeDestination.SettingsHub) }
            menuRow(model, "menu.connectedSites", Icons.Default.Link) { model.push(HomeDestination.EmptyConnected) }
            menuRow(model, "menu.about", Icons.Default.Info) { model.push(HomeDestination.EmptyAbout) }
            HorizontalDivider(color = AirwaveColors.StrokeSoft)
            menuRow(model, "menu.lock", Icons.Default.Lock) { model.lock() }
        }
    }
}

@Composable
private fun MenuSheetTopBar(model: WalletModel) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { model.menuOpen = false },
            modifier = Modifier.semantics { contentDescription = model.t("menu.back") },
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = AirwaveColors.Accent)
        }
        Text(
            model.t("menu.menu"),
            style = MaterialTheme.typography.titleMedium,
            color = AirwaveColors.Text,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Box(Modifier.size(48.dp))
    }
}

@Composable
private fun menuRow(
    model: WalletModel,
    key: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = AirwaveColors.TextMuted, modifier = Modifier.size(22.dp))
        Text(model.t(key), color = AirwaveColors.Text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SubTopBar(model: WalletModel, titleKey: String, showMenu: Boolean = true) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { model.pop() },
            modifier = Modifier.semantics { contentDescription = model.t("menu.back") },
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = AirwaveColors.Accent)
        }
        Text(
            model.t(titleKey),
            style = MaterialTheme.typography.titleMedium,
            color = AirwaveColors.Text,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (showMenu) {
            IconButton(onClick = { model.menuOpen = true }) {
                Icon(Icons.Default.Menu, model.t("menu.menu"), tint = AirwaveColors.Accent)
            }
        } else {
            Box(Modifier.padding(12.dp))
        }
    }
}

@Composable
private fun EmptyScreen(model: WalletModel, titleKey: String, bodyKey: String) {
    Column(Modifier.fillMaxSize().background(AirwaveColors.Bg).statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
        SubTopBar(model, titleKey)
        Box(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(model.t(bodyKey), color = AirwaveColors.TextMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun NetworkScreen(model: WalletModel) {
    Column(Modifier.fillMaxSize().background(AirwaveColors.Bg).statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
        SubTopBar(model, "network.title")
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Cluster.entries.forEach { cluster ->
                val selected = model.settings.cluster == cluster
                PickRow(
                    selected = selected,
                    title = when (cluster) {
                        Cluster.Devnet -> model.t("network.devnet")
                        Cluster.Mainnet -> model.t("network.mainnet")
                    },
                    onClick = { model.setCluster(cluster) },
                )
            }
        }
    }
}

@Composable
private fun SettingsHubScreen(model: WalletModel) {
    Column(
        Modifier
            .fillMaxSize()
            .background(AirwaveColors.Bg)
            .statusBarsPadding()
            .padding(20.dp),
    ) {
        SubTopBar(model, "settings.title")
        Surface(
            color = AirwaveColors.Fill,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = 4.dp)) {
                hubRow(model, "settings.language", localeEndonym(model.settings.locale)) { model.push(HomeDestination.Language) }
                HorizontalDivider(color = AirwaveColors.StrokeSoft)
                hubRow(model, "settings.rpc", summary = null) { model.push(HomeDestination.Rpc) }
                HorizontalDivider(color = AirwaveColors.StrokeSoft)
                hubRow(model, "settings.apiKeys", summary = null) { model.push(HomeDestination.ApiKeys) }
                HorizontalDivider(color = AirwaveColors.StrokeSoft)
                hubRow(
                    model,
                    "settings.ai",
                    if (model.settings.txAiAnalysisEnabled) model.t("settings.aiOn") else model.t("settings.aiOff"),
                ) { model.push(HomeDestination.AiSettings) }
                HorizontalDivider(color = AirwaveColors.StrokeSoft)
                hubRow(model, "settings.cuPrice", model.settings.defaultCuPrice.toString()) { model.push(HomeDestination.CuPrice) }
                HorizontalDivider(color = AirwaveColors.StrokeSoft)
                hubRow(model, "settings.walletLock", summary = null) { model.push(HomeDestination.WalletLockSettings) }
            }
        }
    }
}

@Composable
private fun hubRow(model: WalletModel, labelKey: String, summary: String?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(model.t(labelKey), color = AirwaveColors.Text)
        if (summary != null) {
            Text(summary, color = AirwaveColors.TextMuted)
        }
    }
}

private fun localeEndonym(tag: LocaleTag): String = when (tag) {
    LocaleTag.En -> "English"
    LocaleTag.ZhHant -> "繁體中文"
    LocaleTag.ZhHans -> "简体中文"
}

@Composable
private fun LanguageScreen(model: WalletModel) {
    Column(Modifier.fillMaxSize().background(AirwaveColors.Bg).statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
        SubTopBar(model, "settings.language")
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LocaleTag.entries.forEach { tag ->
                PickRow(
                    selected = model.settings.locale == tag,
                    title = when (tag) {
                        LocaleTag.En -> model.t("locale.en")
                        LocaleTag.ZhHant -> model.t("locale.zhHant")
                        LocaleTag.ZhHans -> model.t("locale.zhHans")
                    },
                    onClick = { model.setLocale(tag) },
                )
            }
        }
    }
}

@Composable
private fun RpcScreen(model: WalletModel) {
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

@Composable
private fun ApiKeysScreen(model: WalletModel) {
    Column(Modifier.fillMaxSize().background(AirwaveColors.Bg).statusBarsPadding().padding(20.dp).verticalScroll(rememberScrollState())) {
        SubTopBar(model, "settings.apiKeys")
        secretField(model, SecretFieldId.Helius, model.t("settings.helius"), model.settings.heliusApiUrl) {
            model.updateSettings { s -> s.copy(heliusApiUrl = it) }
        }
        secretField(model, SecretFieldId.Jupiter, model.t("settings.jupiter"), model.settings.jupiterApiKey) {
            model.updateSettings { s -> s.copy(jupiterApiKey = it) }
        }
    }
}

@Composable
private fun secretField(
    model: WalletModel,
    id: SecretFieldId,
    label: String,
    value: String,
    onSave: (String) -> Unit,
) {
    val state = model.secretState(id)
    SecretKeyField(
        label = label,
        value = value,
        fieldState = state,
        optionalLabel = model.t("common.optional"),
        showLabel = model.t("common.show"),
        editLabel = model.t("common.edit"),
        confirmLabel = model.t("common.confirm"),
        discardLabel = model.t("common.discard"),
        onStartEdit = { model.beginSecretEdit(id, value) },
        onDraftChange = { model.updateSecretDraft(id, it) },
        onConfirm = { model.confirmSecret(id, onSave) },
        onDiscard = { model.discardSecret(id) },
        onToggleReveal = { model.toggleReveal(id) },
        modifier = Modifier.padding(vertical = 12.dp),
    )
}

@Composable
private fun AiSettingsScreen(model: WalletModel) {
    Column(Modifier.fillMaxSize().background(AirwaveColors.Bg).statusBarsPadding().padding(20.dp).verticalScroll(rememberScrollState())) {
        SubTopBar(model, "settings.ai")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(model.t("settings.ai"), color = AirwaveColors.Text)
            Switch(
                checked = model.settings.txAiAnalysisEnabled,
                onCheckedChange = { enabled -> model.updateSettings { it.copy(txAiAnalysisEnabled = enabled) } },
            )
        }
        secretField(model, SecretFieldId.OpenRouter, model.t("settings.openRouter"), model.settings.openRouterApiKey) {
            model.updateSettings { s -> s.copy(openRouterApiKey = it) }
        }
        modelChainBlock(model, model.t("settings.aiReportModel"), true)
        modelChainBlock(model, model.t("settings.aiGradingModel"), false)
    }
}

@Composable
private fun modelChainBlock(model: WalletModel, title: String, report: Boolean) {
    val settings = model.settings
    val mode = if (report) settings.openRouterReportModelMode else settings.openRouterDecisionsModelMode
    val chain = if (report) settings.openRouterReportModelChain else settings.openRouterDecisionsModelChain
    val defaults = if (report) AirwaveSettings.DEFAULT_REPORT_MODELS else AirwaveSettings.DEFAULT_DECISIONS_MODELS
    Column(Modifier.padding(vertical = 12.dp)) {
        Text(title, color = AirwaveColors.Text)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = {
                model.updateSettings {
                    if (report) it.copy(openRouterReportModelMode = ModelMode.Default)
                    else it.copy(openRouterDecisionsModelMode = ModelMode.Default)
                }
            }) { Text(model.t("settings.aiModeDefault"), color = if (mode == ModelMode.Default) AirwaveColors.Accent else AirwaveColors.TextMuted) }
            TextButton(onClick = {
                model.updateSettings {
                    if (report) it.copy(openRouterReportModelMode = ModelMode.Custom)
                    else it.copy(openRouterDecisionsModelMode = ModelMode.Custom)
                }
            }) { Text(model.t("settings.aiModeCustom"), color = if (mode == ModelMode.Custom) AirwaveColors.Accent else AirwaveColors.TextMuted) }
        }
        val slots = (0 until 5).map { index ->
            when (mode) {
                ModelMode.Default -> defaults.getOrElse(index) { "" }
                ModelMode.Custom -> chain.getOrElse(index) { "" }
            }
        }
        slots.forEachIndexed { index, slot ->
            OutlinedTextField(
                value = slot,
                onValueChange = { value ->
                    if (mode != ModelMode.Custom) return@OutlinedTextField
                    model.updateSettings {
                        val base = if (report) it.openRouterReportModelChain else it.openRouterDecisionsModelChain
                        val next = base.toMutableList()
                        while (next.size < 5) next.add("")
                        next[index] = value
                        val normalized = AirwaveSettings.normalizeModelChain(next)
                        if (report) it.copy(openRouterReportModelChain = normalized)
                        else it.copy(openRouterDecisionsModelChain = normalized)
                    }
                },
                readOnly = mode == ModelMode.Default,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                singleLine = true,
            )
        }
    }
}

@Composable
private fun CuPriceScreen(model: WalletModel) {
    var draft by remember(model.settings.defaultCuPrice) { mutableStateOf(model.settings.defaultCuPrice.toString()) }
    Column(Modifier.fillMaxSize().background(AirwaveColors.Bg).statusBarsPadding().padding(20.dp)) {
        SubTopBar(model, "settings.cuPrice")
        OutlinedTextField(
            draft,
            { draft = it },
            label = { Text(model.t("settings.cuLabel")) },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                val parsed = AirwaveSettings.parseCuPriceInput(draft, model.settings.defaultCuPrice)
                model.updateSettings { it.copy(defaultCuPrice = parsed) }
                draft = parsed.toString()
            },
            modifier = Modifier.padding(top = 12.dp),
        ) { Text(model.t("common.confirm")) }
    }
}

@Composable
private fun WalletLockSettingsScreen(model: WalletModel) {
    var minutesDraft by remember(model.settings.idleAutoLockMinutes) {
        mutableStateOf(model.settings.idleAutoLockMinutes.toString())
    }
    var showPasswordForm by remember { mutableStateOf(false) }
    var current by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    Column(
        Modifier
            .fillMaxSize()
            .background(AirwaveColors.Bg)
            .statusBarsPadding()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SubTopBar(model, "settings.walletLock")
        settingsCard {
            Text(model.t("settings.idleAutoLock"), color = AirwaveColors.Text, style = MaterialTheme.typography.titleSmall)
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(model.t("settings.aiOn"), color = AirwaveColors.TextMuted)
                Switch(
                    checked = model.settings.idleAutoLockEnabled,
                    onCheckedChange = { enabled ->
                        model.updateSettings { it.copy(idleAutoLockEnabled = enabled) }
                    },
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(model.t("settings.lockAfterMinutes"), color = AirwaveColors.TextMuted, modifier = Modifier.weight(1f))
                OutlinedTextField(
                    minutesDraft,
                    {
                        minutesDraft = it
                        val parsed = AirwaveSettings.parseIdleLockMinutes(it, model.settings.idleAutoLockMinutes)
                        model.updateSettings { s -> s.copy(idleAutoLockMinutes = parsed) }
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }
        }
        settingsCard {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(model.t("settings.walletLockPassword"), color = AirwaveColors.Text)
                TextButton(onClick = { showPasswordForm = !showPasswordForm }) {
                    Text(model.t("settings.change"), color = AirwaveColors.TextMuted)
                }
            }
            if (showPasswordForm) {
                OutlinedTextField(
                    current,
                    { current = it },
                    label = { Text(model.t("password.current")) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    newPass,
                    { newPass = it },
                    label = { Text(model.t("password.new")) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    confirm,
                    { confirm = it },
                    label = { Text(model.t("password.confirm")) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                localError?.let { Text(it, color = AirwaveColors.Danger) }
                Button(
                    onClick = {
                        localError = model.changePassword(current, newPass, confirm)
                        if (localError == null) {
                            current = ""
                            newPass = ""
                            confirm = ""
                            showPasswordForm = false
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp),
                ) { Text(model.t("password.save")) }
            }
        }
    }
}

@Composable
private fun settingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AirwaveColors.Fill)
            .border(1.dp, AirwaveColors.Stroke, RoundedCornerShape(12.dp))
            .padding(16.dp),
        content = content,
    )
}
