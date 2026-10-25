/**
 * BIP39 English word list loaded from the packaged raw resource.
 * Does not validate mnemonics or derive keys.
 */
package com.airwave.wallet.vault

import android.content.res.Resources
import com.airwave.wallet.R

object Bip39English {
    private var cached: List<String>? = null

    fun words(resources: Resources): List<String> {
        cached?.let { return it }
        val text = resources.openRawResource(R.raw.bip39_english).bufferedReader().readText()
        val list = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        require(list.size == 2048) { "expected 2048 words" }
        cached = list
        return list
    }

    fun indexOf(resources: Resources, word: String): Int =
        words(resources).indexOf(word.lowercase())
}
