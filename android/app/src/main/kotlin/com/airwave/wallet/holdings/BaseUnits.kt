package com.airwave.wallet.holdings

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/**
 * Formats token base units and halves them toward zero.
 * It does not fetch balances or draw a row.
 */
object BaseUnits {
    fun formatBaseUnits(raw: BigInteger, decimals: Int): String {
        if (decimals <= 0) return raw.toString()
        val negative = raw.signum() < 0
        val abs = raw.abs()
        val scale = BigInteger.TEN.pow(decimals)
        val whole = abs.divide(scale)
        val fraction = abs.remainder(scale).toString().padStart(decimals, '0').trimEnd('0')
        val body = if (fraction.isEmpty()) whole.toString() else "$whole.$fraction"
        return if (negative) "-$body" else body
    }

    fun halfTowardZero(raw: BigInteger): BigInteger = raw.divide(BigInteger.valueOf(2))

    fun usdLabel(priceText: String, raw: BigInteger, decimals: Int): String? {
        val price = decimalOrNull(priceText) ?: return null
        val amount = BigDecimal(raw)
        val divisor = BigDecimal.TEN.pow(decimals.coerceAtLeast(0))
        val usd = price.multiply(amount).divide(divisor, 2, RoundingMode.HALF_UP)
        return "$" + usd.setScale(2, RoundingMode.HALF_UP).toPlainString()
    }

    fun usdSort(priceText: String, raw: BigInteger, decimals: Int): BigInteger? {
        val price = decimalOrNull(priceText) ?: return null
        val amount = BigDecimal(raw)
        val divisor = BigDecimal.TEN.pow(decimals.coerceAtLeast(0))
        return price.multiply(amount).divide(divisor, 2, RoundingMode.HALF_UP).unscaledValue()
    }

    fun scoreAboveZero(text: String): Boolean {
        val score = decimalOrNull(text) ?: return false
        return score.compareTo(BigDecimal.ZERO) > 0
    }

    private fun decimalOrNull(text: String): BigDecimal? {
        val trimmed = text.trim()
        if (!trimmed.matches(Regex("\\d+(\\.\\d+)?"))) return null
        return BigDecimal(trimmed)
    }
}
