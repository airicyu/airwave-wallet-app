/**
 * Orders a ready holdings list. Does not fetch or format display strings.
 */
package com.airwave.wallet.holdings

object HoldingSort {
    fun ready(rows: List<HoldingRow>, byUsd: Boolean): List<HoldingRow> {
        val native = rows.filter { it.id == HoldingIds.NATIVE }
        val wrapped = rows.filter { it.id == HoldingIds.WRAPPED }
        val rest = rows.filter { it.id != HoldingIds.NATIVE && it.id != HoldingIds.WRAPPED }
        val ordered = if (!byUsd) {
            rest.sortedBy { it.symbol }
        } else {
            val priced = rest.filter { it.usdSort != null }.sortedWith(
                compareByDescending<HoldingRow> { it.usdSort }.thenBy { it.symbol },
            )
            val missing = rest.filter { it.usdSort == null }.sortedBy { it.symbol }
            priced + missing
        }
        return native + wrapped + ordered
    }
}
