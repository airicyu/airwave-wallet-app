package com.airwave.wallet.mwa.tx

import java.math.BigInteger

/** What the approval should sign and which fee numbers to show. */
data class SignTxPrepared(
    val bytes: ByteArray,
    val editable: Boolean,
    val cuWriteError: Boolean,
    val cuLimit: Long?,
    val cuPrice: Long?,
    val priorityLamports: BigInteger?,
    val signatureLamports: BigInteger,
    val version: TxVersion,
)

/**
 * Chooses the working bytes for one unsigned or already-signed transaction.
 * It does not call RPC. The caller supplies a measured compute-unit count when a probe succeeded.
 */
object SignTxPlan {
    fun prepare(
        original: ByteArray,
        defaultCuPrice: Long,
        measuredUnits: Long?,
        userLimit: Long?,
        userPrice: Long?,
        userPriority: BigInteger?,
    ): SignTxPrepared? {
        val budget = SignTxWire.inspect(original) ?: return null
        val signature = SignTxMath.signatureFee(budget.requiredSignatures)
        if (budget.signed) {
            return done(original, budget, false, false, budget.cuLimit, budget.cuPrice, shownPriority(budget, budget.cuLimit, budget.cuPrice), signature)
        }
        if (userLimit != null || userPrice != null || userPriority != null) {
            val written = SignTxWire.writeBudget(original, userLimit, userPrice, userPriority)
            val priority = when (budget.version) {
                TxVersion.V1 -> userPriority
                else -> if (userLimit != null && userPrice != null) SignTxMath.priorityFromPrice(userLimit, userPrice) else null
            }
            return done(written ?: original, budget, true, written == null, userLimit ?: budget.cuLimit, userPrice, priority, signature)
        }
        if (budget.duplicateComputeBudget) {
            return done(original, budget, true, true, budget.cuLimit, budget.cuPrice, shownPriority(budget, budget.cuLimit, budget.cuPrice), signature)
        }
        if (budget.version == TxVersion.V1) {
            if (budget.priorityFeeLamports != null) {
                return done(original, budget, true, false, budget.cuLimit, null, budget.priorityFeeLamports, signature)
            }
            val written = SignTxWire.writeBudget(original, null, null, SignTxMath.V1_DEFAULT_PRIORITY)
            return done(
                written ?: original,
                budget,
                true,
                written == null,
                budget.cuLimit,
                null,
                if (written != null) SignTxMath.V1_DEFAULT_PRIORITY else null,
                signature,
            )
        }
        val limit = budget.cuLimit ?: limitFor(budget, measuredUnits)
        val price = budget.cuPrice ?: defaultCuPrice
        if (budget.cuLimit != null && budget.cuPrice != null) {
            return done(original, budget, true, false, limit, price, SignTxMath.priorityFromPrice(limit, price), signature)
        }
        val written = SignTxWire.writeBudget(
            original,
            if (budget.cuLimit == null) limit else null,
            if (budget.cuPrice == null) price else null,
            null,
        )
        return done(
            written ?: original,
            budget,
            true,
            written == null,
            limit,
            price,
            SignTxMath.priorityFromPrice(limit, price),
            signature,
        )
    }

    private fun limitFor(budget: TxBudget, measuredUnits: Long?): Long {
        if (measuredUnits != null) return SignTxMath.suggestLimit(measuredUnits, budget.cuLimit)
        if (budget.cuLimit != null) return budget.cuLimit
        val (builtin, other) = SignTxWire.builtinCount(budget.instructionPrograms)
        return SignTxMath.staticLimit(builtin, other)
    }

    private fun shownPriority(budget: TxBudget, limit: Long?, price: Long?): BigInteger? =
        when (budget.version) {
            TxVersion.V1 -> budget.priorityFeeLamports ?: BigInteger.ZERO
            else -> if (limit != null && price != null) SignTxMath.priorityFromPrice(limit, price) else null
        }

    private fun done(
        bytes: ByteArray,
        budget: TxBudget,
        editable: Boolean,
        error: Boolean,
        limit: Long?,
        price: Long?,
        priority: BigInteger?,
        signature: BigInteger,
    ) = SignTxPrepared(bytes, editable, error, limit, price, priority, signature, budget.version)
}
