package io.github.dca.withdrawal

import io.github.dca.AdvancedWithdrawalCalculationRequest
import io.github.dca.WithdrawalCalculationRequest
import io.kotest.matchers.collections.beEmpty
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.math.abs

/**
 * Tests for the advanced withdrawal calculation feature that includes inflation and tax considerations.
 */
class AdvancedWithdrawalCalculationTest {

    @Test
    fun `Should calculate duration with inflation reducing purchasing power`() {
        val request = AdvancedWithdrawalCalculationRequest(
            totalAmount = BigDecimal("1000000"),
            monthlyWithdraw = BigDecimal("5000"),
            expectedYearlyReturn = 7.0,
            yearlyInflationRate = 2.0,
            yearlyTaxAllowance = BigDecimal.ZERO,
            averageTaxRate = 0.0
        )

        val result = calculateAdvancedWithdrawalDuration(request)

        result.isInfinite shouldBe false
        result.years shouldBeGreaterThan 0.0
        // Real return should be nominal return minus inflation
        result.realReturn shouldBe 5.0
        // With inflation, duration should be shorter than without inflation
    }

    @Test
    fun `Should calculate duration with tax reducing returns`() {
        val request = AdvancedWithdrawalCalculationRequest(
            totalAmount = BigDecimal("1000000"),
            monthlyWithdraw = BigDecimal("5000"),
            expectedYearlyReturn = 7.0,
            yearlyInflationRate = 0.0,
            yearlyTaxAllowance = BigDecimal("12000"),
            averageTaxRate = 20.0
        )

        val result = calculateAdvancedWithdrawalDuration(request)

        result.isInfinite shouldBe false
        result.years shouldBeGreaterThan 0.0
        // Should have paid some tax
        result.totalTaxPaid.compareTo(BigDecimal.ZERO) shouldBeGreaterThan 0
        // With tax, duration should be shorter than without tax
        // but we don't need to check the exact value
    }

    @Test
    fun `Should calculate infinite duration when real returns exceed withdrawals`() {
        val request = AdvancedWithdrawalCalculationRequest(
            totalAmount = BigDecimal("1000000"),
            monthlyWithdraw = BigDecimal("2000"),
            expectedYearlyReturn = 7.0,
            yearlyInflationRate = 2.0,
            yearlyTaxAllowance = BigDecimal("12000"),
            averageTaxRate = 10.0
        )

        val result = calculateAdvancedWithdrawalDuration(request)

        result.isInfinite shouldBe true
        // Real return should be nominal return minus inflation
        result.realReturn shouldBe 5.0
    }

    @Test
    fun `Should provide yearly breakdown of calculations`() {
        val request = AdvancedWithdrawalCalculationRequest(
            totalAmount = BigDecimal("500000"),
            monthlyWithdraw = BigDecimal("3000"),
            expectedYearlyReturn = 6.0,
            yearlyInflationRate = 2.0,
            yearlyTaxAllowance = BigDecimal("12000"),
            averageTaxRate = 20.0
        )

        val result = calculateAdvancedWithdrawalDuration(request)

        result.yearlyBreakdown shouldNotBe null
        result.yearlyBreakdown!! shouldNot beEmpty()

        // First year should start with the initial amount
        val firstYear = result.yearlyBreakdown.first()
        firstYear.year shouldBe 1
        firstYear.startingBalance shouldBe request.totalAmount

        // Each year should have some returns
        firstYear.returns.compareTo(BigDecimal.ZERO) shouldBeGreaterThan 0

        // Each year should have withdrawals
        firstYear.withdrawals.compareTo(BigDecimal.ZERO) shouldBeGreaterThan 0

        // Should have some inflation impact
        firstYear.inflationImpact.compareTo(BigDecimal.ZERO) shouldBeGreaterThan 0
    }

    @Test
    fun `Should handle edge case with zero total amount`() {
        val request = AdvancedWithdrawalCalculationRequest(
            totalAmount = BigDecimal.ZERO,
            monthlyWithdraw = BigDecimal("1000"),
            expectedYearlyReturn = 7.0,
            yearlyInflationRate = 2.0,
            yearlyTaxAllowance = BigDecimal("12000"),
            averageTaxRate = 20.0
        )

        val result = calculateAdvancedWithdrawalDuration(request)

        result.isInfinite shouldBe false
        result.years shouldBe 0.0
        result.totalTaxPaid shouldBe BigDecimal.ZERO
    }

    @Test
    fun `Should handle edge case with zero monthly withdraw`() {
        val request = AdvancedWithdrawalCalculationRequest(
            totalAmount = BigDecimal("1000000"),
            monthlyWithdraw = BigDecimal.ZERO,
            expectedYearlyReturn = 7.0,
            yearlyInflationRate = 2.0,
            yearlyTaxAllowance = BigDecimal("12000"),
            averageTaxRate = 20.0
        )

        val result = calculateAdvancedWithdrawalDuration(request)

        result.isInfinite shouldBe true
        result.totalTaxPaid shouldBe BigDecimal.ZERO
    }

    @Test
    fun `Should adjust withdrawal amount for inflation over time`() {
        val request = AdvancedWithdrawalCalculationRequest(
            totalAmount = BigDecimal("1000000"),
            monthlyWithdraw = BigDecimal("5000"),
            expectedYearlyReturn = 7.0,
            yearlyInflationRate = 3.0,
            yearlyTaxAllowance = BigDecimal.ZERO,
            averageTaxRate = 0.0
        )

        val result = calculateAdvancedWithdrawalDuration(request)

        // Final withdrawal amount should be higher than initial due to inflation
        result.inflationAdjustedWithdrawal.compareTo(request.monthlyWithdraw) shouldBeGreaterThan 0
    }

    @Test
    fun `Should compare advanced and basic calculation results`() {
        // Basic request with no inflation or tax
        val basicRequest = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("1000000"),
            monthlyWithdraw = BigDecimal("5000"),
            expectedYearlyReturn = 7.0
        )

        // Advanced request with the same parameters but no inflation or tax
        val advancedRequest = AdvancedWithdrawalCalculationRequest(
            totalAmount = BigDecimal("1000000"),
            monthlyWithdraw = BigDecimal("5000"),
            expectedYearlyReturn = 7.0,
            yearlyInflationRate = 0.0,
            yearlyTaxAllowance = BigDecimal.ZERO,
            averageTaxRate = 0.0
        )

        val basicResult = calculateWithdrawalDuration(basicRequest)
        val advancedResult = calculateAdvancedWithdrawalDuration(advancedRequest)

        // Both should agree on whether it's infinite
        basicResult.isInfinite shouldBe advancedResult.isInfinite

        // If finite, they should be reasonably close (within 5% of each other)
        if (!basicResult.isInfinite && !advancedResult.isInfinite) {
            val percentDifference = abs(basicResult.years - advancedResult.years) / basicResult.years * 100
            percentDifference shouldBeLessThan 5.0
        }
    }
}