/**
 * Downloads an https token icon. Does not choose where the image sits on screen.
 */
package com.airwave.wallet.holdings

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.net.URL

object TokenIconLoader {
    fun bitmap(url: String): Bitmap? {
        if (!url.startsWith("https:")) return null
        return try {
            URL(url).openStream().use { BitmapFactory.decodeStream(it) }
        } catch (_: Exception) {
            null
        }
    }
}
