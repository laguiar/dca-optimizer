package io.github.dca

import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.assertions.*
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
        
        expect {
            that(result.isInfinite).isFalse()
            that(result.years).isGreaterThan(0.0)
            // Real return should be nominal return minus inflation
            that(result.realReturn).isEqualTo(5.0)
            // With inflation, duration should be shorter than without inflation
            // but we don't need to check the exact value
        }
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
        
        expect {
            that(result.isInfinite).isFalse()
            that(result.years).isGreaterThan(0.0)
            // Should have paid some tax
            that(result.totalTaxPaid.compareTo(BigDecimal.ZERO)).isGreaterThan(0)
            // With tax, duration should be shorter than without tax
            // but we don't need to check the exact value
        }
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
        
        expect {
            that(result.isInfinite).isTrue()
            // Real return should be nominal return minus inflation
            that(result.realReturn).isEqualTo(5.0)
        }
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
        
        expect {
            that(result.yearlyBreakdown).isNotNull()
            that(result.yearlyBreakdown!!).isNotEmpty()
            
            // First year should start with the initial amount
            val firstYear = result.yearlyBreakdown!!.first()
            that(firstYear.year).isEqualTo(1)
            that(firstYear.startingBalance).isEqualTo(request.totalAmount)
            
            // Each year should have some returns
            that(firstYear.returns.compareTo(BigDecimal.ZERO)).isGreaterThan(0)
            
            // Each year should have withdrawals
            that(firstYear.withdrawals.compareTo(BigDecimal.ZERO)).isGreaterThan(0)
            
            // Should have some inflation impact
            that(firstYear.inflationImpact.compareTo(BigDecimal.ZERO)).isGreaterThan(0)
        }
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
        
        expect {
            that(result.isInfinite).isFalse()
            that(result.years).isEqualTo(0.0)
            that(result.totalTaxPaid).isEqualTo(BigDecimal.ZERO)
        }
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
        
        expect {
            that(result.isInfinite).isTrue()
            that(result.totalTaxPaid).isEqualTo(BigDecimal.ZERO)
        }
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
        
        expect {
            // Final withdrawal amount should be higher than initial due to inflation
            that(result.inflationAdjustedWithdrawal.compareTo(request.monthlyWithdraw)).isGreaterThan(0)
        }
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
        
        expect {
            // Both should agree on whether it's infinite
            that(basicResult.isInfinite).isEqualTo(advancedResult.isInfinite)
            
            // If finite, they should be reasonably close (within 5% of each other)
            if (!basicResult.isInfinite && !advancedResult.isInfinite) {
                val percentDifference = abs(basicResult.years - advancedResult.years) / basicResult.years * 100
                that(percentDifference < 5.0).isTrue()
            }
        }
    }
} 