/**
 * Draws the unlock-screen radial gradient to match the extension CSS.
 * It does not own the password field or the route.
 */
package com.airwave.wallet.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.max

fun Modifier.airwaveUnlockBackground(): Modifier = drawBehind {
    drawRect(AirwaveColors.Bg)
    val w = size.width
    val h = size.height
    val r = max(w, h)
    fun glow(center: Offset, radius: Float, color: Color) {
        drawCircle(
            brush = Brush.radialGradient(
                0f to color,
                0.72f to Color.Transparent,
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
    }
    glow(Offset(w * 0.12f, h * 0.82f), r * 0.55f, Color(0x1C9945FF))
    glow(Offset(w * 0.92f, h * 0.14f), r * 0.48f, Color(0x1414F195))
    glow(Offset(w * 0.72f, h * 0.68f), r * 0.40f, Color(0x125B8CFF))
}
