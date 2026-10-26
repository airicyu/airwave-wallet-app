package com.airwave.wallet.holdings

import java.math.BigInteger

/** Records where one holdings fetch round sits on the assets tab. */
enum class HoldingPhase {
    Idle,
    Loading,
    Ready,
    Failed,
    Guide,
    Skipped,
}

/** Names which token program minted this SPL row. */
enum class TokenProgramKind {
    SplToken,
    Token2022,
}

/**
 * One in-memory asset row shown on the assets tab.
 * It does not fetch balances or format amounts.
 */
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

/** Holds the current holdings list together with its phase and error for the assets tab. */
data class HoldingsSnapshot(
    val rows: List<HoldingRow> = emptyList(),
    val phase: HoldingPhase = HoldingPhase.Idle,
    val error: String? = null,
)

/** Holds well-known mint and program ids used when building holding rows. */
object HoldingIds {
    const val NATIVE = "native-sol"
    const val WRAPPED = "So11111111111111111111111111111111111111112"
    const val TOKEN_PROGRAM = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
    const val TOKEN_2022 = "TokenzQdBNbLqP5VEhdkAS6EPFLC1PHnBqCXEpPxuEb"
}
