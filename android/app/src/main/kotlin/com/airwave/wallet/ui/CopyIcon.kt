/**
 * Copies text and swaps the icon for a green check for 1.6 seconds.
 * Callers pass the string and the two labels. This file does not choose what is copied.
 */
package com.airwave.wallet.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airwave.wallet.R
import com.airwave.wallet.theme.AirwaveColors
import kotlinx.coroutines.delay

@Composable
internal fun CopyIcon(
    text: String,
    idleLabel: String,
    copiedLabel: String,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    idleTint: Color = AirwaveColors.TextMuted,
) {
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1600)
            copied = false
        }
    }
    val context = LocalContext.current
    Icon(
        painterResource(if (copied) R.drawable.ic_stroke_check else R.drawable.ic_stroke_copy),
        contentDescription = if (copied) copiedLabel else idleLabel,
        tint = if (copied) AirwaveColors.Ok else idleTint,
        modifier = modifier
            .size(size)
            .clickable(enabled = text.isNotEmpty()) {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                try {
                    cm.setPrimaryClip(ClipData.newPlainText("copy", text))
                } catch (_: RuntimeException) {
                    return@clickable
                }
                copied = true
            },
    )
}
