package com.airwave.wallet.mwa

import java.nio.charset.CharacterCodingException
import java.nio.charset.CharsetDecoder
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/** Holds UTF-8 text when the bytes decode, and always the lowercase hex of those same bytes. */
data class MessageDisplay(
    val utf8: String?,
    val hex: String,
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
        val hex = bytes.joinToString("") { b -> "%02x".format(b) }
        return try {
            val text = decoder.decode(java.nio.ByteBuffer.wrap(bytes)).toString()
            MessageDisplay(text, hex)
        } catch (_: CharacterCodingException) {
            MessageDisplay(null, hex)
        }
    }

    fun failBeforeOffer(hasPending: Boolean, isWatch: Boolean): Boolean =
        hasPending || isWatch

    /** True when the wallet must decline before offer: no payload, an empty first payload, or more than one. */
    fun declineBeforeOffer(payloads: Array<ByteArray>?): Boolean {
        if (emptyPayloads(payloads)) return true
        return payloads!!.size != 1
    }

    /**
     * True when there is no address list, or the list is exactly the active public key.
     * Any other list is not valid for this account.
     */
    fun addressesAllow(addresses: Array<ByteArray>?, active: ByteArray): Boolean {
        if (addresses == null || addresses.isEmpty()) return true
        return addresses.size == 1 && addresses[0].contentEquals(active)
    }
}
