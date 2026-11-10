/**
 * Draws shared chrome for add-account inner pages, including the name field, word grid, dice, pubkey row, and CTA.
 * It does not commit vault rows.
 */
package com.airwave.wallet.ui
import com.airwave.wallet.session.WalletModel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.airwave.wallet.R
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
internal fun SeedWarnLine(text: String, modifier: Modifier = Modifier) {
    Text(text, color = SeedWarn, fontSize = 12.sp, modifier = modifier)
}

/** Burner: web field margin plus toolbar padding. */
internal val BurnerDicePadding = PaddingValues(top = 22.dp, bottom = 8.dp)

private val DiceEase = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

/** Web `dice-shake`: 210ms, origin 50% 60%, per-keyframe CSS ease. Offsets are fractions; y is dp. */
private val DiceShakeKeys = listOf(
    0f to (0f to 0f),
    0.18f to (-3f to -10f),
    0.36f to (-2.5f to 8f),
    0.54f to (-2f to -5f),
    0.72f to (-1f to 2f),
    1f to (0f to 0f),
)

@Composable
internal fun DiceButton(
    label: String,
    padding: PaddingValues = BurnerDicePadding,
    inline: Boolean = false,
    onClick: () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var shakeJob by remember { mutableStateOf<Job?>(null) }
    val hopPx = with(LocalDensity.current) { 1.dp.toPx() }
    val sample = diceShake(progress.value)
    val circle = @Composable {
        Box(
            Modifier
                .size(45.dp)
                .clip(CircleShape)
                .border(2.dp, DiePurple, CircleShape)
                .clickable {
                    val previous = shakeJob
                    shakeJob = scope.launch {
                        previous?.cancel()
                        previous?.join()
                        progress.snapTo(0f)
                        progress.animateTo(1f, tween(210, easing = LinearEasing))
                    }
                    onClick()
                }
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_die_3d),
                contentDescription = null,
                tint = DieTint,
                modifier = Modifier
                    .size(34.dp)
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0.5f, 0.6f)
                        translationY = sample.first * hopPx
                        rotationZ = sample.second
                    },
            )
        }
    }
    if (inline) {
        circle()
    } else {
        Row(Modifier.fillMaxWidth().padding(padding), horizontalArrangement = Arrangement.End) {
            circle()
        }
    }
}

private fun diceShake(t: Float): Pair<Float, Float> {
    val keys = DiceShakeKeys
    val clamped = t.coerceIn(0f, 1f)
    val i = keys.indexOfLast { it.first <= clamped }.coerceAtLeast(0)
    val (aT, a) = keys[i]
    val (bT, b) = keys[(i + 1).coerceAtMost(keys.lastIndex)]
    val span = (bT - aT).takeIf { it > 0f } ?: return b
    val eased = DiceEase.transform(((clamped - aT) / span).coerceIn(0f, 1f))
    return (a.first + (b.first - a.first) * eased) to (a.second + (b.second - a.second) * eased)
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
