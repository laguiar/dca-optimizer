package io.github.dca.withdrawal

import io.github.dca.YearlyBreakdown
import java.math.BigDecimal
import java.math.BigDecimal.ZERO

// Data class to track the simulation state
internal data class SimulationState(
    val currentBalance: BigDecimal,
    val month: Int,
    val year: Int,
    val yearStartBalance: BigDecimal,
    val yearReturns: BigDecimal = ZERO,
    val yearWithdrawals: BigDecimal = ZERO,
    val yearTaxPaid: BigDecimal = ZERO,
    val yearInflationImpact: BigDecimal = ZERO,
    val totalTaxPaid: BigDecimal = ZERO,
    val currentMonthlyWithdraw: BigDecimal,
    val yearlyBreakdown: MutableList<YearlyBreakdown> = mutableListOf()
)

/**
 * Utility class to return four values from a function.
 */
internal data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
