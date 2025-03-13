package io.github.dca.withdrawal

import io.github.dca.BigDecimalNumber
import io.github.dca.YearlyBreakdown
import java.math.BigDecimal
import kotlin.math.max

/**
 * Calculates the tax and breakdown for a year.
 *  
 * @param isYearEnd Whether the current month is the end of the year.
 * @param newYearReturns The returns for the year.
 * @param state The current state of the simulation.
 * @param newYearWithdrawals The withdrawals for the year.
 * @param newYearInflationImpact The inflation impact for the year.
 * @param newBalance The balance for the year.
 */
internal fun taxAndBreakdownCalculation(
    isYearEnd: Boolean,
    newYearReturns: BigDecimal,
    state: SimulationState,
    newYearWithdrawals: BigDecimal,
    newYearInflationImpact: BigDecimal,
    newBalance: BigDecimal,
    yearlyTaxAllowance: BigDecimalNumber,
    averageTaxRate: Double
) = if (isYearEnd) {
    // Calculate taxable gains (total returns minus allowance)
    val taxableGains = max(0.0, newYearReturns.subtract(yearlyTaxAllowance).toDouble())
    val taxAmount = BigDecimal(taxableGains * averageTaxRate)

    // Create yearly breakdown entry
    val breakdown = YearlyBreakdown(
        year = state.year,
        startingBalance = state.yearStartBalance,
        returns = newYearReturns,
        withdrawals = newYearWithdrawals,
        taxPaid = taxAmount,
        inflationImpact = newYearInflationImpact,
        endingBalance = newBalance
    )

    // Add to breakdown list
    state.yearlyBreakdown.add(breakdown)

    // Deduct tax from balance
    val balanceAfterTax = newBalance.subtract(taxAmount)

    Quadruple(
        taxAmount,
        state.totalTaxPaid.add(taxAmount),
        balanceAfterTax,
        state.yearlyBreakdown
    )
} else {
    Quadruple(
        state.yearTaxPaid,
        state.totalTaxPaid,
        newBalance,
        state.yearlyBreakdown
    )
}
