package com.airwave.wallet.mwa

import java.nio.charset.CharacterCodingException
import java.nio.charset.CharsetDecoder
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/** Holds UTF-8 text for a sign-message payload, or hex when the bytes are not valid UTF-8. */
data class MessageDisplay(
    val utf8: String?,
    val hex: String?,
)

/**
 * Decides UTF-8 or hex display for MWA payloads and fails empty payloads before offer.
 * It does not sign or talk to walletlib.
 */
object MwaPayloads {
    fun emptyPayloads(payloads: Array<ByteArray>?): Boolean {
        val first = payloads?.firstOrNull() ?: return true
        return first.isEmpty()
    }

    fun display(bytes: ByteArray): MessageDisplay {
        val decoder: CharsetDecoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try {
            val text = decoder.decode(java.nio.ByteBuffer.wrap(bytes)).toString()
            MessageDisplay(text, null)
        } catch (_: CharacterCodingException) {
            MessageDisplay(null, bytes.joinToString("") { b -> "%02x".format(b) })
        }
    }

    fun failBeforeOffer(hasPending: Boolean, isWatch: Boolean): Boolean =
        hasPending || isWatch
}
