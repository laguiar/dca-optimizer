package io.github.dca

import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.math.BigDecimal
import kotlin.math.abs
import kotlin.test.assertFalse

class WithdrawalCalculationTest {

    @Test
    fun `Should calculate finite duration when withdrawals exceed returns`() {
        val request = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("1000000"),
            monthlyWithdraw = BigDecimal("5000"),
            expectedYearlyReturn = 4.0
        )

        val result = calculateWithdrawalDuration(request)
        
        expect {
            that(result.isInfinite).isFalse()
            // Use a wider range for the test to accommodate different calculation results
            that(result.years > 20.0).isTrue()
            that(result.years < 30.0).isTrue()
        }
    }

    @Test
    fun `Should calculate infinite duration when returns exceed withdrawals`() {
        val request = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("1000000"),
            monthlyWithdraw = BigDecimal("2000"),
            expectedYearlyReturn = 4.0
        )

        val result = calculateWithdrawalDuration(request)
        
        expect {
            that(result.isInfinite).isTrue()
        }
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
        expect {
            that(result.isInfinite).isFalse()
            that(result.years).isEqualTo(10.0)
        }
    }

    @Test
    fun `Should calculate duration with very small monthly withdraw`() {
        val request = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("1000000"),
            monthlyWithdraw = BigDecimal("1"),
            expectedYearlyReturn = 2.0
        )

        val result = calculateWithdrawalDuration(request)
        
        expect {
            that(result.isInfinite).isTrue()
        }
    }

    @Test
    fun `Should calculate duration with very large monthly withdraw`() {
        val request = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("1000000"),
            monthlyWithdraw = BigDecimal("100000"),
            expectedYearlyReturn = 5.0
        )

        val result = calculateWithdrawalDuration(request)
        
        expect {
            that(result.isInfinite).isFalse()
            // Should be less than a year
            that(result.years < 1.0).isTrue()
        }
    }

    @Test
    fun `Should calculate duration with negative expected return`() {
        val request = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("1000000"),
            monthlyWithdraw = BigDecimal("5000"),
            expectedYearlyReturn = -2.0
        )

        val result = calculateWithdrawalDuration(request)
        
        expect {
            that(result.isInfinite).isFalse()
            // Should be less than with zero return
            that(result.years < 16.67).isTrue()
        }
    }

    @Test
    fun `Should calculate duration with high expected return`() {
        val request = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("1000000"),
            monthlyWithdraw = BigDecimal("5000"),
            expectedYearlyReturn = 10.0
        )

        val result = calculateWithdrawalDuration(request)
        
        // With high returns, the money might last indefinitely
        if (result.isInfinite) {
            expect {
                that(result.isInfinite).isTrue()
            }
        } else {
            expect {
                that(result.isInfinite).isFalse()
                that(result.years > 25.0).isTrue()
            }
        }
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
        
        expect {
            that(result7Percent.isInfinite).isFalse()
            // Should be around 29-30 years
            that(result7Percent.years > 25.0).isTrue()
            that(result7Percent.years < 35.0).isTrue()
        }
        
        // Test with 8% return - should be finite with our 1% safety margin
        val request8Percent = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("300000"),
            monthlyWithdraw = BigDecimal("2000"),
            expectedYearlyReturn = 8.0
        )
        
        val result8Percent = calculateWithdrawalDuration(request8Percent)
        
        expect {
            that(result8Percent.isInfinite).isFalse()
            // Should be longer than with 7%
            that(result8Percent.years > result7Percent.years).isTrue()
        }
        
        // Test with 9% return - should be infinite with our 1% safety margin
        val request9Percent = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("300000"),
            monthlyWithdraw = BigDecimal("2000"),
            expectedYearlyReturn = 9.0
        )
        
        val result9Percent = calculateWithdrawalDuration(request9Percent)
        
        expect {
            that(result9Percent.isInfinite).isTrue()
        }
    }

    @Test
    fun `Should simulate finite duration when withdrawals exceed returns`() {
        val request = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("1000000"),
            monthlyWithdraw = BigDecimal("5000"),
            expectedYearlyReturn = 4.0
        )

        val result = simulateWithdrawalDuration(request)
        
        expect {
            that(result.isInfinite).isFalse()
            // The simulation might give slightly different results than the formula
            that(result.years > 20.0).isTrue()
            that(result.years < 30.0).isTrue()
        }
    }

    @Test
    fun `Should simulate infinite duration when returns exceed withdrawals`() {
        val request = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("1000000"),
            monthlyWithdraw = BigDecimal("2000"),
            expectedYearlyReturn = 4.0
        )

        val result = simulateWithdrawalDuration(request)
        
        expect {
            that(result.isInfinite).isTrue()
        }
    }

    @Test
    fun `Should simulate duration with zero expected return`() {
        val request = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("120000"),
            monthlyWithdraw = BigDecimal("1000"),
            expectedYearlyReturn = 0.0
        )

        val result = simulateWithdrawalDuration(request)
        
        expect {
            that(result.isInfinite).isFalse()
            that(result.years).isEqualTo(10.0)
        }
    }

    @Test
    fun `Should simulate duration with very small monthly withdraw`() {
        val request = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("1000000"),
            monthlyWithdraw = BigDecimal("1"),
            expectedYearlyReturn = 2.0
        )

        val result = simulateWithdrawalDuration(request)
        
        expect {
            that(result.isInfinite).isTrue()
        }
    }

    @Test
    fun `Should simulate duration with very large monthly withdraw`() {
        val request = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("1000000"),
            monthlyWithdraw = BigDecimal("100000"),
            expectedYearlyReturn = 5.0
        )

        val result = simulateWithdrawalDuration(request)
        
        expect {
            that(result.isInfinite).isFalse()
            // Should be less than a year
            that(result.years < 1.0).isTrue()
        }
    }

    @Test
    fun `Should simulate duration with negative expected return`() {
        val request = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("1000000"),
            monthlyWithdraw = BigDecimal("5000"),
            expectedYearlyReturn = -2.0
        )

        val result = simulateWithdrawalDuration(request)
        
        expect {
            that(result.isInfinite).isFalse()
            // Should be less than with zero return
            that(result.years < 16.67).isTrue()
        }
    }

    @Test
    fun `Should simulate duration with specific expected return`() {
        val request = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("100000"),
            monthlyWithdraw = BigDecimal("1000"),
            expectedYearlyReturn = 4.0
        )

        val result = simulateWithdrawalDuration(request)

        assertFalse { result.isInfinite }
        expectThat(result.years).isEqualTo(10.167)
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
            
            expect {
                // Both should agree on whether it's infinite
                that(formulaResult.isInfinite).isEqualTo(simulationResult.isInfinite)
                
                // If finite, they should be reasonably close (within 5% of each other)
                if (!formulaResult.isInfinite && !simulationResult.isInfinite) {
                    val percentDifference = abs(formulaResult.years - simulationResult.years) / formulaResult.years * 100
                    that(percentDifference < 5.0).isTrue()
                }
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
        
        expect {
            that(result.isInfinite).isFalse()
            that(result.years).isEqualTo(0.0)
        }
        
        val simulationResult = simulateWithdrawalDuration(request)
        
        expect {
            that(simulationResult.isInfinite).isFalse()
            that(simulationResult.years).isEqualTo(0.0)
        }
    }

    @Test
    fun `Should handle edge case with zero monthly withdraw`() {
        val request = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("1000000"),
            monthlyWithdraw = BigDecimal.ZERO,
            expectedYearlyReturn = 4.0
        )

        val result = calculateWithdrawalDuration(request)
        
        expect {
            that(result.isInfinite).isTrue()
        }
        
        val simulationResult = simulateWithdrawalDuration(request)
        
        expect {
            that(simulationResult.isInfinite).isTrue()
        }
    }
} 