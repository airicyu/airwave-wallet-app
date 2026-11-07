/**
 * Draws the settings hub and its sub-screens from the home menu stack.
 * It does not own RPC endpoint editing or the home shell tabs.
 */
package com.airwave.wallet.ui
import com.airwave.wallet.session.WalletModel
import com.airwave.wallet.session.HomeDestination

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.airwave.wallet.beginSecretEdit
import com.airwave.wallet.confirmSecret
import com.airwave.wallet.discardSecret
import com.airwave.wallet.setLocale
import com.airwave.wallet.toggleReveal
import com.airwave.wallet.updateSecretDraft
import com.airwave.wallet.updateSettings
import com.airwave.wallet.settings.AirwaveSettings
import com.airwave.wallet.settings.LocaleTag
import com.airwave.wallet.settings.ModelMode
import com.airwave.wallet.theme.AirwaveColors

@Composable
internal fun SettingsHubScreen(model: WalletModel) {
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
internal fun LanguageScreen(model: WalletModel) {
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
internal fun ApiKeysScreen(model: WalletModel) {
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
internal fun AiSettingsScreen(model: WalletModel) {
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
internal fun CuPriceScreen(model: WalletModel) {
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
internal fun WalletLockSettingsScreen(model: WalletModel) {
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
