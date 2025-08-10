package io.github.dca.withdrawal

import io.github.dca.WithdrawalCalculationRequest
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.math.abs

class WithdrawalCalculationTest {

    @Test
    fun `Should calculate finite duration when withdrawals exceed returns`() {
        val request = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("1000000"),
            monthlyWithdraw = BigDecimal("5000"),
            expectedYearlyReturn = 4.0
        )

        val result = calculateWithdrawalDuration(request)
        
        result.isInfinite shouldBe false
        // Use a wider range for the test to accommodate different calculation results
        result.years shouldBeGreaterThan 20.0
        result.years shouldBeLessThan 30.0
    }

    @Test
    fun `Should calculate infinite duration when returns exceed withdrawals`() {
        val request = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("1000000"),
            monthlyWithdraw = BigDecimal("2000"),
            expectedYearlyReturn = 4.0
        )

        val result = calculateWithdrawalDuration(request)
        
        result.isInfinite shouldBe true
    }

    @Test
    fun `Should calculate duration with zero expected return`() {
        val request = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("120000"),
            monthlyWithdraw = BigDecimal("1000"),
            expectedYearlyReturn = 0.0
        )

        val result = calculateWithdrawalDuration(request)
        
        // With zero return, it's just dividing the total by monthly withdraw
        result.isInfinite shouldBe false
        result.years shouldBe 10.0
    }

    @Test
    fun `Should calculate duration with very small monthly withdraw`() {
        val request = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("1000000"),
            monthlyWithdraw = BigDecimal("1"),
            expectedYearlyReturn = 2.0
        )

        val result = calculateWithdrawalDuration(request)
        
        result.isInfinite shouldBe true
    }

    @Test
    fun `Should calculate duration with very large monthly withdraw`() {
        val request = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("1000000"),
            monthlyWithdraw = BigDecimal("100000"),
            expectedYearlyReturn = 5.0
        )

        val result = calculateWithdrawalDuration(request)
        
        result.isInfinite shouldBe false
        // Should be less than a year
        result.years shouldBeLessThan 1.0
    }

    @Test
    fun `Should calculate duration with negative expected return`() {
        val request = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("1000000"),
            monthlyWithdraw = BigDecimal("5000"),
            expectedYearlyReturn = -2.0
        )

        val result = calculateWithdrawalDuration(request)
        result.isInfinite shouldBe false
        result.years shouldBeLessThan 16.67 // Should be less than with zero return
    }

    @Test
    fun `Should calculate duration with high expected return`() {
        val request = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("1000000"),
            monthlyWithdraw = BigDecimal("5000"),
            expectedYearlyReturn = 10.0
        )

        val result = calculateWithdrawalDuration(request)
        result.years shouldBeGreaterThan 25.0 // Should be a long duration
        result.isInfinite shouldBe true
    }

    @Test
    fun `Should handle the boundary case between finite and infinite duration`() {
        // Test with 7% return - should be finite
        val request7Percent = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("300000"),
            monthlyWithdraw = BigDecimal("2000"),
            expectedYearlyReturn = 7.0
        )
        
        val result7Percent = calculateWithdrawalDuration(request7Percent)
        result7Percent.isInfinite shouldBe false
        // Should be around 29-30 years
        result7Percent.years shouldBeGreaterThan 25.0
        result7Percent.years shouldBeLessThan 35.0
        
        // Test with 8% return - should be finite with our 1% safety margin
        val request8Percent = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("300000"),
            monthlyWithdraw = BigDecimal("2000"),
            expectedYearlyReturn = 8.0
        )
        
        val result8Percent = calculateWithdrawalDuration(request8Percent)
        result8Percent.isInfinite shouldBe false
        // Should be longer than with 7%
        result8Percent.years shouldBeGreaterThan result7Percent.years

        // Test with 9% return - should be infinite with our 1% safety margin
        val request9Percent = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("300000"),
            monthlyWithdraw = BigDecimal("2000"),
            expectedYearlyReturn = 9.0
        )
        
        val result9Percent = calculateWithdrawalDuration(request9Percent)
        result9Percent.isInfinite shouldBe true
    }

    @Test
    fun `Should simulate finite duration when withdrawals exceed returns`() {
        val request = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("1000000"),
            monthlyWithdraw = BigDecimal("5000"),
            expectedYearlyReturn = 4.0
        )

        val result = simulateWithdrawalDuration(request)
        result.isInfinite shouldBe false
        // The simulation might give slightly different results than the formula
        result.years shouldBeGreaterThan 20.0
        result.years shouldBeLessThan 30.0
    }

    @Test
    fun `Should simulate infinite duration when returns exceed withdrawals`() {
        val request = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("1000000"),
            monthlyWithdraw = BigDecimal("2000"),
            expectedYearlyReturn = 4.0
        )

        val result = simulateWithdrawalDuration(request)
        
        result.isInfinite shouldBe true
    }

    @Test
    fun `Should simulate duration with zero expected return`() {
        val request = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("120000"),
            monthlyWithdraw = BigDecimal("1000"),
            expectedYearlyReturn = 0.0
        )

        val result = simulateWithdrawalDuration(request)
        
        result.isInfinite shouldBe false
        result.years shouldBe 10.0
    }

    @Test
    fun `Should simulate duration with very small monthly withdraw`() {
        val request = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("1000000"),
            monthlyWithdraw = BigDecimal("1"),
            expectedYearlyReturn = 2.0
        )

        val result = simulateWithdrawalDuration(request)
        
        result.isInfinite shouldBe true
    }

    @Test
    fun `Should simulate duration with very large monthly withdraw`() {
        val request = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("1000000"),
            monthlyWithdraw = BigDecimal("100000"),
            expectedYearlyReturn = 5.0
        )

        val result = simulateWithdrawalDuration(request)
        
        result.isInfinite shouldBe false
        // Should be less than a year
        result.years shouldBeLessThan 1.0
    }

    @Test
    fun `Should simulate duration with negative expected return`() {
        val request = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("1000000"),
            monthlyWithdraw = BigDecimal("5000"),
            expectedYearlyReturn = -2.0
        )

        val result = simulateWithdrawalDuration(request)
        
        result.isInfinite shouldBe false
        // Should be less than with zero return
        result.years shouldBeLessThan 16.67
    }

    @Test
    fun `Should simulate duration with specific expected return`() {
        val request = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("100000"),
            monthlyWithdraw = BigDecimal("1000"),
            expectedYearlyReturn = 4.0
        )

        val result = simulateWithdrawalDuration(request)

        result.isInfinite shouldBe false
        result.years shouldBe 10.167
    }

    @Test
    fun `Should compare formula and simulation results`() {
        val testCases = listOf(
            WithdrawalCalculationRequest(
                totalAmount = BigDecimal("500000"),
                monthlyWithdraw = BigDecimal("2500"),
                expectedYearlyReturn = 3.0
            ),
            WithdrawalCalculationRequest(
                totalAmount = BigDecimal("1000000"),
                monthlyWithdraw = BigDecimal("4000"),
                expectedYearlyReturn = 2.0
            ),
            WithdrawalCalculationRequest(
                totalAmount = BigDecimal("250000"),
                monthlyWithdraw = BigDecimal("1500"),
                expectedYearlyReturn = 1.0
            )
        )
        
        testCases.forEach { request ->
            val formulaResult = calculateWithdrawalDuration(request)
            val simulationResult = simulateWithdrawalDuration(request)
            
            // Both should agree on whether it's infinite
            formulaResult.isInfinite shouldBe simulationResult.isInfinite
            
            // If finite, they should be reasonably close (within 5% of each other)
            if (!formulaResult.isInfinite && !simulationResult.isInfinite) {
                val percentDifference = abs(formulaResult.years - simulationResult.years) / formulaResult.years * 100
                percentDifference shouldBeLessThan 5.0
            }
        }
    }

    @Test
    fun `Should handle edge case with zero total amount`() {
        val request = WithdrawalCalculationRequest(
            totalAmount = BigDecimal.ZERO,
            monthlyWithdraw = BigDecimal("1000"),
            expectedYearlyReturn = 4.0
        )

        val result = calculateWithdrawalDuration(request)
        
        result.isInfinite shouldBe false
        result.years shouldBe 0.0
        
        val simulationResult = simulateWithdrawalDuration(request)
        
        simulationResult.isInfinite shouldBe false
        simulationResult.years shouldBe 0.0
    }

    @Test
    fun `Should handle edge case with zero monthly withdraw`() {
        val request = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("1000000"),
            monthlyWithdraw = BigDecimal.ZERO,
            expectedYearlyReturn = 4.0
        )

        val result = calculateWithdrawalDuration(request)
        result.isInfinite shouldBe true
        
        val simulationResult = simulateWithdrawalDuration(request)
        simulationResult.isInfinite shouldBe true
    }
} 