/**
 * Holding row types for the in-memory assets snapshot.
 * Does not fetch balances or format amounts.
 */
package com.airwave.wallet.holdings

import java.math.BigInteger

enum class HoldingPhase {
    Idle,
    Loading,
    Ready,
    Failed,
    Guide,
    Skipped,
}

enum class TokenProgramKind {
    SplToken,
    Token2022,
}

data class HoldingRow(
    val id: String,
    val name: String,
    val symbol: String,
    val raw: BigInteger,
    val decimals: Int,
    val tokenProgram: TokenProgramKind?,
    val iconUrl: String? = null,
    val verified: Boolean = false,
    val organicScore: String? = null,
    val usdLabel: String = "—",
    val usdSort: BigInteger? = null,
)

data class HoldingsSnapshot(
    val rows: List<HoldingRow> = emptyList(),
    val phase: HoldingPhase = HoldingPhase.Idle,
    val error: String? = null,
)

object HoldingIds {
    const val NATIVE = "native-sol"
    const val WRAPPED = "So11111111111111111111111111111111111111112"
    const val TOKEN_PROGRAM = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
    const val TOKEN_2022 = "TokenzQdBNbLqP5VEhdkAS6EPFLC1PHnBqCXEpPxuEb"
}
