package io.github.dca

import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.math.MathContext.DECIMAL32
import java.math.RoundingMode.HALF_EVEN
import kotlin.Double.Companion.POSITIVE_INFINITY
import kotlin.math.exp
import kotlin.math.ln

private const val MAX_MONTHS = 3600 // 300 years as a reasonable upper limit
private const val SAFETY_MARGIN = 0.01 // 1% of total amount as safety margin
private const val MONTHS = 12.0
private val context = DECIMAL32

// Define a class for the search result
private data class SearchBounds(val low: BigDecimal, val high: BigDecimal, val isValid: Boolean = true)

/**
 * Calculates how many years a total wealth amount will last given monthly withdrawals and expected returns.
 * 
 * @param request The withdrawal calculation parameters
 * @return A response containing the number of years the money will last and whether it lasts indefinitely
 */
fun calculateWithdrawalDuration(request: WithdrawalCalculationRequest): WithdrawalCalculationResponse {
    val totalAmount = request.totalAmount
    val monthlyWithdraw = request.monthlyWithdraw
    val yearlyReturn = request.expectedYearlyReturn
    
    // Handle edge cases
    if (totalAmount <= ZERO) {
        return WithdrawalCalculationResponse(
            years = 0.0,
            isInfinite = false
        )
    }
    
    if (monthlyWithdraw <= ZERO) {
        return WithdrawalCalculationResponse(
            years = POSITIVE_INFINITY,
            isInfinite = true
        )
    }
    
    // Convert yearly return to monthly
    val monthlyReturn = yearlyReturn / MONTHS / 100.0
    
    // If annual returns exceed annual withdrawals by at least the safety margin, money lasts forever
    val annualWithdrawalDouble = monthlyWithdraw.multiply(BigDecimal("12")).toDouble()
    val annualReturnDouble = totalAmount.toDouble() * (yearlyReturn / 100.0)
    val safetyMarginDouble = totalAmount.toDouble() * SAFETY_MARGIN
    
    // Check if returns exceed withdrawals with safety margin
    if (annualReturnDouble - annualWithdrawalDouble >= safetyMarginDouble) {
        return WithdrawalCalculationResponse(
            years = POSITIVE_INFINITY,
            isInfinite = true
        )
    }
    
    // Special case for zero return
    if (yearlyReturn == 0.0) {
        val months = totalAmount.divide(monthlyWithdraw, context)
        return WithdrawalCalculationResponse(
            years = months.divide(BigDecimal(MONTHS), context).toDouble(),
            isInfinite = false
        )
    }
    
    // Calculate how many months the money will last
    // Formula: n = -ln(1 - r*P/W) / ln(1 + r)
    // Where:
    // n = number of months
    // r = monthly interest rate (as a decimal)
    // P = principal (total amount)
    // W = monthly withdrawal amount
    
    val rP = monthlyReturn * totalAmount.toDouble()
    val numerator = -ln(1.0 - (rP / monthlyWithdraw.toDouble()))
    val denominator = ln(1.0 + monthlyReturn)
    
    val months = numerator / denominator
    
    // Convert months to years with 2 decimal precision
    val years = BigDecimal(months / MONTHS)
        .setScale(context.precision, context.roundingMode)
        .toDouble()
        
    return WithdrawalCalculationResponse(
        years = years,
        isInfinite = false
    )
}

/**
 * Alternative calculation that accounts for the exact monthly compounding.
 * This simulates the withdrawal process month by month, for a slightly more accurate result.
 */
fun simulateWithdrawalDuration(request: WithdrawalCalculationRequest): WithdrawalCalculationResponse {
    val totalAmount = request.totalAmount
    val monthlyWithdraw = request.monthlyWithdraw
    val yearlyReturn = request.expectedYearlyReturn
    
    // Handle edge cases
    if (totalAmount <= ZERO) {
        return WithdrawalCalculationResponse(
            years = 0.0,
            isInfinite = false
        )
    }
    
    if (monthlyWithdraw <= ZERO) {
        return WithdrawalCalculationResponse(
            years = POSITIVE_INFINITY,
            isInfinite = true
        )
    }
    
    val monthlyReturn = yearlyReturn / MONTHS / 100.0
    
    // If annual returns exceed annual withdrawals by at least the safety margin, money lasts forever
    val annualWithdrawalDouble = monthlyWithdraw.multiply(BigDecimal("12")).toDouble()
    val annualReturnDouble = totalAmount.toDouble() * (yearlyReturn / 100.0)
    val safetyMarginDouble = totalAmount.toDouble() * SAFETY_MARGIN
    
    // Check if returns exceed withdrawals with safety margin
    if (annualReturnDouble - annualWithdrawalDouble >= safetyMarginDouble) {
        return WithdrawalCalculationResponse(
            years = POSITIVE_INFINITY,
            isInfinite = true
        )
    }
    
    // Special case for zero return
    if (yearlyReturn == 0.0) {
        val months = totalAmount.divide(monthlyWithdraw, context).toDouble()
        return WithdrawalCalculationResponse(
            years = BigDecimal(months / MONTHS)
                .setScale(3, context.roundingMode)
                .toDouble(),
            isInfinite = false
        )
    }

    // Define a data class for the simulation state
    data class SimulationState(val amount: BigDecimal, val month: Int)
    
    // Define a function to calculate the next state
    fun nextState(state: SimulationState): SimulationState? {
        if (state.amount <= ZERO || state.month >= MAX_MONTHS) return null
        
        val newAmount = state.amount
            .multiply(BigDecimal(1.0 + monthlyReturn), context)
            .subtract(monthlyWithdraw)
            
        return SimulationState(newAmount, state.month + 1)
    }
    
    // Run the simulation using a sequence
    val finalState = generateSequence(SimulationState(totalAmount, 0), ::nextState).last()
    
    // If we reached the maximum, it effectively lasts forever
    if (finalState.month >= MAX_MONTHS) {
        return WithdrawalCalculationResponse(
            years = POSITIVE_INFINITY,
            isInfinite = true
        )
    }
    
    // Convert months to years with 2 decimal precision
    val years = BigDecimal(finalState.month / MONTHS)
        .setScale(3, context.roundingMode)
        .toDouble()
        
    return WithdrawalCalculationResponse(
        years = years,
        isInfinite = false
    )
}

/**
 * Calculates the initial amount needed for a specific withdrawal duration.
 * This is the inverse of the withdrawal duration calculation.
 * 
 * @param request The initial amount calculation parameters
 * @return The total amount needed to meet the specified duration
 */
fun calculateInitialAmount(request: InitialAmountCalculationRequest): InitialAmountCalculationResponse {
    val years = request.shouldLastForYears
    val monthlyWithdraw = request.monthlyWithdraw
    val yearlyReturn = request.expectedYearlyReturn
    
    // Handle edge cases
    if (years <= 0.0) {
        return InitialAmountCalculationResponse(totalAmount = ZERO)
    }
    
    if (monthlyWithdraw <= ZERO) {
        return InitialAmountCalculationResponse(totalAmount = ZERO)
    }
    
    // Special case for zero return
    if (yearlyReturn == 0.0) {
        // If no return, just multiply monthly withdrawal by number of months
        val totalMonths = years * MONTHS
        val totalAmount = monthlyWithdraw.multiply(BigDecimal(totalMonths))
        return InitialAmountCalculationResponse(totalAmount = totalAmount.setScale(2, HALF_EVEN))
    }
    
    // Convert yearly return to monthly
    val monthlyReturn = yearlyReturn / MONTHS / 100.0
    
    // Calculate the initial amount needed
    // Rearranging the formula: n = -ln(1 - r*P/W) / ln(1 + r)
    // To solve for P: P = W * (1 - (1 + r)^(-n)) / r
    // Where:
    // n = number of months
    // r = monthly interest rate (as a decimal)
    // P = principal (total amount)
    // W = monthly withdrawal amount
    
    val months = years * MONTHS
    val factor = 1.0 - exp(-months * ln(1.0 + monthlyReturn))
    val totalAmount = monthlyWithdraw.toDouble() * factor / monthlyReturn
    
    return InitialAmountCalculationResponse(
        totalAmount = BigDecimal(totalAmount).setScale(context.precision, context.roundingMode)
    )
}

// Define a data class to represent the search state
private data class SearchState(val low: BigDecimal, val high: BigDecimal, val attempt: Int = 0)

/**
 * Alternative calculation that uses binary search to find the initial amount needed.
 * This is more accurate for edge cases but computationally more expensive.
 */
fun simulateInitialAmount(request: InitialAmountCalculationRequest): InitialAmountCalculationResponse {
    val years = request.shouldLastForYears
    val monthlyWithdraw = request.monthlyWithdraw
    val yearlyReturn = request.expectedYearlyReturn
    
    // Handle edge cases
    if (years <= 0.0) {
        return InitialAmountCalculationResponse(totalAmount = ZERO)
    }
    
    if (monthlyWithdraw <= ZERO) {
        return InitialAmountCalculationResponse(totalAmount = ZERO)
    }
    
    // Special case for zero return
    if (yearlyReturn == 0.0) {
        // If no return, just multiply monthly withdrawal by number of months
        val totalMonths = years * MONTHS
        val totalAmount = monthlyWithdraw.multiply(BigDecimal(totalMonths))
        return InitialAmountCalculationResponse(totalAmount = totalAmount.setScale(2, HALF_EVEN))
    }
    
    // For medium to high returns, use the formula calculation as it's more reliable
    if (yearlyReturn >= 4.0) {
        return calculateInitialAmount(request)
    }

    // Function to test if an amount is sufficient
    fun testAmount(amount: BigDecimal): Boolean {
        return try {
            val testRequest = WithdrawalCalculationRequest(
                totalAmount = amount,
                monthlyWithdraw = monthlyWithdraw,
                expectedYearlyReturn = yearlyReturn
            )
            val result = calculateWithdrawalDuration(testRequest)
            result.isInfinite || result.years >= years
        } catch (e: Exception) {
            false
        }
    }
    
    // Find initial upper bound using a functional approach
    val initialHigh = monthlyWithdraw.multiply(BigDecimal(years * MONTHS * 2))
    val maxAttempts = 10
    
    // Function to find a suitable upper bound
    fun findUpperBound(): SearchBounds {
        // Generate a sequence of search states, doubling the high value each time
        val finalState = generateSequence(SearchState(ZERO, initialHigh)) { state ->
            if (state.attempt < maxAttempts && !testAmount(state.high)) {
                SearchState(
                    low = state.high,
                    high = state.high.multiply(BigDecimal("2"), context),
                    attempt = state.attempt + 1
                )
            } else {
                null // Stop the sequence
            }
        }.last()

        // If we couldn't find a suitable upper bound, return an invalid result
        if (finalState.attempt >= maxAttempts && !testAmount(finalState.high)) {
            return SearchBounds(finalState.low, finalState.high, isValid = false)
        }
        
        return SearchBounds(finalState.low, finalState.high)
    }
    
    // Function to perform binary search
    fun binarySearch(initialLow: BigDecimal, initialHigh: BigDecimal): BigDecimal {
        val tolerance = 0.01 // 1% tolerance
        
        // Generate a sequence of search states for binary search
        val binarySearchSequence = generateSequence(
            SearchState(initialLow, initialHigh)
        ) { state ->
            val relativeDifference = state.high.subtract(state.low)
                .divide(state.high, context)
                .toDouble()

            if (state.attempt < maxAttempts && relativeDifference > tolerance) {
                val mid = state.low.add(state.high).divide(BigDecimal("2"), context)

                when {
                    testAmount(mid) -> SearchState(state.low, mid, state.attempt + 1)
                    else -> SearchState(mid, state.high, state.attempt + 1)
                }
            } else {
                null // Stop the sequence
            }
        }
        
        return binarySearchSequence.last().high
    }
    
    // Execute the search process
    return try {
        val bounds = findUpperBound()
        
        if (bounds.isValid) {
            val finalAmount = binarySearch(bounds.low, bounds.high)
            InitialAmountCalculationResponse(totalAmount = finalAmount.setScale(2, HALF_EVEN))
        } else {
            // Fall back to formula calculation
            calculateInitialAmount(request)
        }
    } catch (e: Exception) {
        // Fall back to formula calculation if any errors occur
        calculateInitialAmount(request)
    }
} 