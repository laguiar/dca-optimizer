package io.github.dca.withdrawal

import io.github.dca.InitialAmountCalculationRequest
import io.github.dca.WithdrawalCalculationRequest
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
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
        result.totalAmount shouldBe BigDecimal("120000.00")
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
        
        duration.isInfinite shouldBe false
        (abs(duration.years - 30.0) < 0.1) shouldBe true // Within 0.1 years
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
        
        duration.isInfinite shouldBe false
        (abs(duration.years - 5.0) < 0.1) shouldBe true // Within 0.1 years
    }

    @Test
    fun `Should handle edge case with zero years`() {
        val request = InitialAmountCalculationRequest(
            shouldLastForYears = 0.0,
            monthlyWithdraw = BigDecimal("1000"),
            expectedYearlyReturn = 4.0
        )

        val result = calculateInitialAmount(request)
        
        result.totalAmount shouldBe BigDecimal.ZERO
    }

    @Test
    fun `Should handle edge case with zero monthly withdraw`() {
        val request = InitialAmountCalculationRequest(
            shouldLastForYears = 10.0,
            monthlyWithdraw = BigDecimal.ZERO,
            expectedYearlyReturn = 4.0
        )

        val result = calculateInitialAmount(request)
        
        result.totalAmount shouldBe BigDecimal.ZERO
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
        
        duration.isInfinite shouldBe false
        duration.years shouldBeGreaterThanOrEqual 30.0 // Should last at least 30 years
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
        
        duration.isInfinite shouldBe false
        duration.years shouldBeGreaterThanOrEqual 20.0 // Should last at least 20 years
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
        
        duration.isInfinite shouldBe false
        duration.years shouldBeGreaterThanOrEqual 10.0 // Should last at least 10 years
    }
} 