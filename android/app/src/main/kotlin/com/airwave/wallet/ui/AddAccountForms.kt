/**
 * Draws the add-account inner screens that match the wallet-overall-v1.html phone frames.
 * It does not own the five-row chooser or vault encryption.
 */
package com.airwave.wallet.ui
import com.airwave.wallet.session.WalletModel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airwave.wallet.commitImportedMnemonic
import com.airwave.wallet.commitImportedSecret
import com.airwave.wallet.commitPreviewBurner
import com.airwave.wallet.commitSeedAccount
import com.airwave.wallet.commitWatchAddress
import com.airwave.wallet.theme.AirwaveColors
import com.airwave.wallet.vault.Bip39
import com.airwave.wallet.vault.BurnerKeys
import com.airwave.wallet.vault.ImportPathKind

@Composable
internal fun AddBurnerScreen(model: WalletModel) {
    var name by remember { mutableStateOf("") }
    val key = model.preview
    AddAccountPage(
        model = model,
        titleKey = "accounts.screen.burner",
        cta = model.t("accounts.generate"),
        onCta = {
            model.commitPreviewBurner(name)
        },
        ctaEnabled = key != null,
    ) {
        OptionalNameField(model, name, { name = it })
        DiceButton(model.t("accounts.rerollA11y")) {
            model.preview?.zero()
            model.preview = BurnerKeys.generate()
        }
        PubkeyRow(model.t("accounts.pubkey"), model.t("accounts.copy"), key?.address.orEmpty())
    }
}

@Composable
internal fun GenerateSeedScreen(model: WalletModel) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    val words = (model.seedGenerateDraft ?: "").split(Regex("\\s+")).filter { it.isNotEmpty() }
    val padded = if (words.size >= 12) words.take(12) else words + List(12 - words.size) { "" }
    val phrase = phraseOf(padded)
    val previewAddress = remember(phrase) {
        if (phrase.split(" ").size != 12) "" else {
            val seed = Bip39.deriveDefaultSeedAccount(phrase)
            val addr = BurnerKeys.addressOf(seed)
            seed.fill(0)
            addr
        }
    }
    AddAccountPage(
        model = model,
        titleKey = "accounts.screen.seed",
        cta = model.t("accounts.create"),
        onCta = {
            model.commitSeedAccount(phrase, name)
            model.popToHome()
        },
        ctaEnabled = padded.count { it.isNotBlank() } == 12,
    ) {
        OptionalNameField(model, name, { name = it })
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SeedWarnLine(
                model.t("accounts.seedWarn"),
                Modifier.weight(1f).padding(end = 16.dp),
            )
            DiceButton(model.t("accounts.rerollA11y"), inline = true) {
                model.seedGenerateDraft = Bip39.generate12(context.resources)
            }
        }
        WordGrid(padded, readOnly = true) { }
        PubkeyRow(model.t("accounts.pubkey"), model.t("accounts.copy"), previewAddress)
    }
}

@Composable
internal fun ImportMnemonicScreen(model: WalletModel) {
    val context = LocalContext.current
    var words by remember { mutableStateOf(List(12) { "" }) }
    var step by remember { mutableStateOf(0) }
    var pathKind by remember { mutableStateOf(model.importPathKind) }
    var pathIndex by remember { mutableStateOf(model.importPathIndex) }
    val filled = words.count { it.isNotBlank() }
    val phrase = phraseOf(words)
    val checksumOk = Bip39.validateChecksum(context.resources, phrase)
    if (step == 0) {
        AddAccountPage(
            model = model,
            titleKey = "accounts.importMnemonic",
            cta = model.t("accounts.next"),
            onCta = { if (checksumOk) step = 1 },
            ctaEnabled = checksumOk,
        ) {
            Text(
                model.t("accounts.importHint"),
                color = AirwaveColors.TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 10.dp),
            )
            WordGrid(words, readOnly = false) { words = it }
            Text(
                "${model.t("accounts.filled")} $filled／${words.size}",
                color = AirwaveColors.TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    } else {
        val previews = remember(phrase, pathKind) {
            if (!checksumOk) emptyList()
            else (0..4).map { idx ->
                val seed = Bip39.deriveSolanaSeed(context.resources, phrase, pathKind, idx)
                val addr = BurnerKeys.addressOf(seed)
                seed.fill(0)
                idx to addr
            }
        }
        AddAccountPage(
            model = model,
            titleKey = "accounts.importMnemonic",
            cta = model.t("accounts.importAction"),
            onCta = {
                model.importPathKind = pathKind
                model.importPathIndex = pathIndex
                model.commitImportedMnemonic(phrase, pathKind, pathIndex, "")
                model.popToHome()
            },
            ctaEnabled = checksumOk,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                TextButton(onClick = { pathKind = ImportPathKind.Phantom }) {
                    Text(model.t("accounts.pathPhantom"), color = if (pathKind == ImportPathKind.Phantom) AirwaveColors.Accent else AirwaveColors.TextMuted)
                }
                TextButton(onClick = { pathKind = ImportPathKind.Cli }) {
                    Text(model.t("accounts.pathCli"), color = if (pathKind == ImportPathKind.Cli) AirwaveColors.Accent else AirwaveColors.TextMuted)
                }
                TextButton(onClick = { pathKind = ImportPathKind.Change }) {
                    Text(model.t("accounts.pathChange"), color = if (pathKind == ImportPathKind.Change) AirwaveColors.Accent else AirwaveColors.TextMuted)
                }
            }
            previews.forEach { (idx, addr) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (pathIndex == idx) AirwaveColors.BgElevated else AirwaveColors.Fill)
                        .clickable { pathIndex = idx }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("$idx", color = AirwaveColors.TextMuted, modifier = Modifier.padding(end = 12.dp))
                    Text(BurnerKeys.short(addr), color = AirwaveColors.Text, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
internal fun ImportSecretScreen(model: WalletModel) {
    var name by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    AddAccountPage(
        model = model,
        titleKey = "accounts.importSecret",
        cta = model.t("accounts.importAction"),
        onCta = {
            val seed = parseSecretBytes(text) ?: return@AddAccountPage
            model.commitImportedSecret(seed, name)
            model.popToHome()
        },
        ctaEnabled = parseSecretBytes(text) != null,
    ) {
        OptionalNameField(model, name, { name = it })
        LabeledFillField(
            label = model.t("accounts.secretField"),
            value = text,
            onValue = { text = it },
            placeholder = model.t("accounts.secretPlaceholder"),
            singleLine = false,
            mono = true,
            minLines = 4,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 10.dp),
        ) {
            Box(
                Modifier
                    .padding(end = 8.dp)
                    .size(8.dp)
                    .background(AirwaveColors.Ok, CircleShape),
            )
            Text(model.t("accounts.secretFmt"), color = AirwaveColors.TextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
internal fun WatchAddressScreen(model: WalletModel) {
    var name by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    AddAccountPage(
        model = model,
        titleKey = "accounts.screen.watch",
        cta = model.t("accounts.create"),
        onCta = {
            val bytes = try {
                BurnerKeys.base58Decode(text.trim())
            } catch (_: Exception) {
                return@AddAccountPage
            }
            if (bytes.size != 32) return@AddAccountPage
            model.commitWatchAddress(BurnerKeys.base58(bytes), name)
            model.popToHome()
        },
        ctaEnabled = runCatching {
            BurnerKeys.base58Decode(text.trim()).size == 32
        }.getOrDefault(false),
    ) {
        OptionalNameField(model, name, { name = it })
        LabeledFillField(
            label = model.t("accounts.addressField"),
            value = text,
            onValue = { text = it },
            placeholder = model.t("accounts.addressPlaceholder"),
            singleLine = true,
            mono = true,
        )
    }
}

internal fun parseSecretBytes(text: String): ByteArray? {
    val trimmed = text.trim()
    if (trimmed.startsWith("[")) {
        val nums = Regex("\\d+").findAll(trimmed).map { it.value.toInt() }.toList()
        if (nums.any { it !in 0..255 }) return null
        val bytes = nums.map { it.toByte() }.toByteArray()
        return when (bytes.size) {
            32 -> bytes
            64 -> bytes.copyOfRange(0, 32)
            else -> null
        }
    }
    return try {
        val bytes = BurnerKeys.base58Decode(trimmed)
        when (bytes.size) {
            32 -> bytes
            64 -> bytes.copyOfRange(0, 32)
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}
