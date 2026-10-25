/**
 * Shared chrome for add-account inner pages: name field, word grid, dice, pubkey row, CTA.
 * Does not commit vault rows.
 */
package com.airwave.wallet.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airwave.wallet.R
import com.airwave.wallet.WalletModel
import com.airwave.wallet.theme.AirwaveColors
import com.airwave.wallet.vault.BurnerKeys

private val DiePurple = Color(0xFF9945FF)
private val DieTint = Color(0xFFB794F6)
private val SeedWarn = Color(0xFFF07178)
private val CellShape = RoundedCornerShape(8.dp)
private val FieldShape = RoundedCornerShape(10.dp)

@Composable
internal fun AddAccountPage(
    model: WalletModel,
    titleKey: String,
    cta: String,
    onCta: () -> Unit,
    ctaEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(AirwaveColors.Bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        SubTopBar(model, titleKey, showMenu = false)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            content()
        }
        AirwavePrimaryButton(text = cta, onClick = onCta, enabled = ctaEnabled, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
    }
}

@Composable
internal fun OptionalNameField(model: WalletModel, value: String, onValue: (String) -> Unit) {
    LabeledFillField(
        label = model.t("accounts.label"),
        value = value,
        onValue = onValue,
        placeholder = model.t("common.optional"),
        singleLine = true,
    )
}

@Composable
internal fun LabeledFillField(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    mono: Boolean = false,
    minLines: Int = 1,
) {
    Text(
        label,
        color = AirwaveColors.TextMuted,
        fontSize = 13.sp,
        modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
    )
    val style = TextStyle(
        color = AirwaveColors.Text,
        fontSize = 14.sp,
        fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
    )
    BasicTextField(
        value = value,
        onValueChange = onValue,
        singleLine = singleLine,
        minLines = minLines,
        textStyle = style,
        cursorBrush = SolidColor(AirwaveColors.Accent),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (minLines > 1) 96.dp else 44.dp)
                    .clip(FieldShape)
                    .background(AirwaveColors.Fill)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, color = AirwaveColors.TextDim, fontSize = 14.sp, fontFamily = style.fontFamily)
                }
                inner()
            }
        },
    )
}

@Composable
internal fun SeedWarnLine(text: String) {
    Text(text, color = SeedWarn, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
}

@Composable
internal fun DiceButton(label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Box(
            Modifier
                .padding(vertical = 4.dp)
                .size(45.dp)
                .clip(CircleShape)
                .border(2.dp, DiePurple, CircleShape)
                .clickable(onClick = onClick)
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_die_3d),
                contentDescription = null,
                tint = DieTint,
                modifier = Modifier.size(34.dp),
            )
        }
    }
}

@Composable
internal fun WordGrid(
    words: List<String>,
    readOnly: Boolean,
    onWords: (List<String>) -> Unit,
) {
    val rows = (words.indices).chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        rows.forEach { idxs ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                idxs.forEach { i ->
                    WordCell(
                        index = i + 1,
                        value = words.getOrElse(i) { "" },
                        readOnly = readOnly,
                        modifier = Modifier.weight(1f),
                    ) { incoming ->
                        val next = words.toMutableList()
                        if (incoming.contains(Regex("\\s")) && incoming.trim().split(Regex("\\s+")).size >= 12) {
                            val parsed = incoming.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
                            val size = if (parsed.size >= 24) 24 else 12
                            onWords((0 until size).map { parsed.getOrElse(it) { "" } })
                        } else {
                            while (next.size <= i) next.add("")
                            next[i] = incoming.trim().lowercase()
                            onWords(next)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WordCell(
    index: Int,
    value: String,
    readOnly: Boolean,
    modifier: Modifier,
    onValue: (String) -> Unit,
) {
    Row(
        modifier
            .clip(CellShape)
            .background(AirwaveColors.Fill)
            .border(1.dp, AirwaveColors.Stroke, CellShape)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$index", color = AirwaveColors.TextDim, fontSize = 11.sp, modifier = Modifier.padding(end = 6.dp))
        if (readOnly) {
            Text(value, color = AirwaveColors.Text, fontSize = 12.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
        } else {
            BasicTextField(
                value = value,
                onValueChange = onValue,
                singleLine = true,
                textStyle = TextStyle(color = AirwaveColors.Text, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                cursorBrush = SolidColor(AirwaveColors.Accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun PubkeyRow(label: String, copyLabel: String, address: String) {
    val context = LocalContext.current
    Text(label, color = AirwaveColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (address.isEmpty()) "" else BurnerKeys.short(address),
            color = AirwaveColors.Text,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
        )
        if (address.isNotEmpty()) {
            Icon(
                painter = painterResource(R.drawable.ic_copy_addr),
                contentDescription = copyLabel,
                tint = AirwaveColors.TextMuted,
                modifier = Modifier
                    .size(18.dp)
                    .clickable {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("address", address))
                    },
            )
        }
    }
}

internal fun phraseOf(words: List<String>): String =
    words.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.joinToString(" ")
