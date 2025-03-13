package io.github.dca.withdrawal

import io.github.dca.InitialAmountCalculationRequest
import io.github.dca.WithdrawalCalculationRequest
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.math.BigDecimal
import kotlin.math.abs

class InitialAmountCalculationTest {

    @Test
    fun `Should calculate initial amount for zero return`() {
        val request = InitialAmountCalculationRequest(
            shouldLastForYears = 10.0,
            monthlyWithdraw = BigDecimal("1000"),
            expectedYearlyReturn = 0.0
        )

        val result = calculateInitialAmount(request)
        
        // With zero return, it's just monthly withdraw * months
        expectThat(result.totalAmount).isEqualTo(BigDecimal("120000.00"))
    }

    @Test
    fun `Should calculate initial amount for positive return`() {
        val request = InitialAmountCalculationRequest(
            shouldLastForYears = 30.0,
            monthlyWithdraw = BigDecimal("4000"),
            expectedYearlyReturn = 6.0
        )

        val result = calculateInitialAmount(request)
        
        // Verify by calculating the duration with the resulting amount
        val verificationRequest = WithdrawalCalculationRequest(
            totalAmount = result.totalAmount,
            monthlyWithdraw = BigDecimal("4000"),
            expectedYearlyReturn = 6.0
        )
        
        val duration = calculateWithdrawalDuration(verificationRequest)
        
        expect {
            that(duration.isInfinite).isFalse()
            that(abs(duration.years - 30.0) < 0.1).isTrue() // Within 0.1 years
        }
    }

    @Test
    fun `Should calculate initial amount for negative return`() {
        val request = InitialAmountCalculationRequest(
            shouldLastForYears = 5.0,
            monthlyWithdraw = BigDecimal("2000"),
            expectedYearlyReturn = -2.0
        )

        val result = calculateInitialAmount(request)
        
        // Verify by calculating the duration with the resulting amount
        val verificationRequest = WithdrawalCalculationRequest(
            totalAmount = result.totalAmount,
            monthlyWithdraw = BigDecimal("2000"),
            expectedYearlyReturn = -2.0
        )
        
        val duration = calculateWithdrawalDuration(verificationRequest)
        
        expect {
            that(duration.isInfinite).isFalse()
            that(abs(duration.years - 5.0) < 0.1).isTrue() // Within 0.1 years
        }
    }

    @Test
    fun `Should handle edge case with zero years`() {
        val request = InitialAmountCalculationRequest(
            shouldLastForYears = 0.0,
            monthlyWithdraw = BigDecimal("1000"),
            expectedYearlyReturn = 4.0
        )

        val result = calculateInitialAmount(request)
        
        expectThat(result.totalAmount).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `Should handle edge case with zero monthly withdraw`() {
        val request = InitialAmountCalculationRequest(
            shouldLastForYears = 10.0,
            monthlyWithdraw = BigDecimal.ZERO,
            expectedYearlyReturn = 4.0
        )

        val result = calculateInitialAmount(request)
        
        expectThat(result.totalAmount).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `Should simulate initial amount for positive return`() {
        val request = InitialAmountCalculationRequest(
            shouldLastForYears = 30.0,
            monthlyWithdraw = BigDecimal("4000"),
            expectedYearlyReturn = 6.0
        )

        val result = simulateInitialAmount(request)
        
        // Verify by simulating the duration with the resulting amount
        val verificationRequest = WithdrawalCalculationRequest(
            totalAmount = result.totalAmount,
            monthlyWithdraw = BigDecimal("4000"),
            expectedYearlyReturn = 6.0
        )
        
        val duration = simulateWithdrawalDuration(verificationRequest)
        
        expect {
            that(duration.isInfinite).isFalse()
            that(duration.years >= 30.0).isTrue() // Should last at least 30 years
        }
    }

    @Test
    fun `Should simulate initial amount for medium duration`() {
        val request = InitialAmountCalculationRequest(
            shouldLastForYears = 20.0,
            monthlyWithdraw = BigDecimal("3000"),
            expectedYearlyReturn = 5.0
        )

        val result = simulateInitialAmount(request)
        
        // Verify by simulating the duration with the resulting amount
        val verificationRequest = WithdrawalCalculationRequest(
            totalAmount = result.totalAmount,
            monthlyWithdraw = BigDecimal("3000"),
            expectedYearlyReturn = 5.0
        )
        
        val duration = simulateWithdrawalDuration(verificationRequest)
        
        expect {
            that(duration.isInfinite).isFalse()
            that(duration.years >= 20.0).isTrue() // Should last at least 20 years
        }
    }

    @Test
    fun `Should simulate initial amount for short duration`() {
        val request = InitialAmountCalculationRequest(
            shouldLastForYears = 10.0,
            monthlyWithdraw = BigDecimal("1500"),
            expectedYearlyReturn = 2.0
        )

        val result = simulateInitialAmount(request)
        
        // Verify by simulating the duration with the resulting amount
        val verificationRequest = WithdrawalCalculationRequest(
            totalAmount = result.totalAmount,
            monthlyWithdraw = BigDecimal("1500"),
            expectedYearlyReturn = 2.0
        )
        
        val duration = simulateWithdrawalDuration(verificationRequest)
        
        expect {
            that(duration.isInfinite).isFalse()
            that(duration.years >= 10.0).isTrue() // Should last at least 10 years
        }
    }
} 