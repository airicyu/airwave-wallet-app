package com.airwave.wallet.mwa.tx

import java.math.BigInteger

/**
 * Integer fee and compute-limit formulas for a sign-transaction approval.
 * It does not read transaction bytes or call RPC.
 */
object SignTxMath {
    val SIG_LAMPORTS: BigInteger = BigInteger.valueOf(5_000)
    const val CU_LIMIT_MIN = 1L
    const val CU_LIMIT_MAX = 1_400_000L
    const val CU_PRICE_MAX = 1_000_000_000L
    val PRIORITY_FEE_MAX: BigInteger = BigInteger.valueOf(1_000_000_000L)
    val V1_DEFAULT_PRIORITY: BigInteger = BigInteger.valueOf(5_000)

    fun signatureFee(requiredSignatures: Int): BigInteger =
        SIG_LAMPORTS.multiply(BigInteger.valueOf(requiredSignatures.toLong()))

    /** Priority fee in lamports from a legacy or v0 limit and micro-lamport price. */
    fun priorityFromPrice(limit: Long, price: Long): BigInteger {
        val micro = BigInteger.valueOf(limit).multiply(BigInteger.valueOf(price))
        val unit = BigInteger.valueOf(1_000_000)
        return micro.add(unit.subtract(BigInteger.ONE)).divide(unit)
    }

    fun suggestLimit(unitsConsumed: Long, originalLimit: Long?): Long {
        val fromUsage = ceilMul(unitsConsumed, 11, 10)
        val base = if (originalLimit != null) maxOf(fromUsage, originalLimit) else fromUsage
        return base.coerceIn(CU_LIMIT_MIN, CU_LIMIT_MAX)
    }

    fun staticLimit(builtinInstructions: Int, otherInstructions: Int): Long {
        val sum = builtinInstructions.toLong() * 3_000L + otherInstructions.toLong() * 200_000L
        return ceilMul(sum, 5, 4).coerceIn(CU_LIMIT_MIN, CU_LIMIT_MAX)
    }

    fun limitInRange(value: Long): Boolean = value in CU_LIMIT_MIN..CU_LIMIT_MAX

    fun priceInRange(value: Long): Boolean = value in 0..CU_PRICE_MAX

    fun priorityInRange(value: BigInteger): Boolean =
        value.signum() >= 0 && value <= PRIORITY_FEE_MAX

    private fun ceilMul(value: Long, numerator: Long, denominator: Long): Long {
        val n = BigInteger.valueOf(value).multiply(BigInteger.valueOf(numerator))
        val d = BigInteger.valueOf(denominator)
        return n.add(d.subtract(BigInteger.ONE)).divide(d).toLong()
    }
}
