package io.github.dca

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

private val mathContext = MathContext.DECIMAL32
const val HUNDRED_PERCENT = 100.0
const val ZERO = 0.0

fun extractCalculationBasis(
    request: DcaRequest,
    filteredAssets: List<Asset>
): Pair<BigDecimal, Map<String, BigDecimal>> {
    val underWeightAmounts = filteredAssets.associate {
        it.ticker to request.portfolioValueOrZero()
            .multiply((it.target - it.weight)
                .toDecimalRepresentation()
                .toBigDecimal())
    }
    val totalNeedForRebalancing = underWeightAmounts.values.sumOf { it }
    return Pair(totalNeedForRebalancing, underWeightAmounts)
}

fun BigDecimal.calculateDistribution(percentage: Double): BigDecimal =
    this.multiply(BigDecimal(percentage.toString()), mathContext)
        .setScale(2, RoundingMode.HALF_DOWN)

// extract current invested value from updated weight and portfolio value and extract new updated weight
fun calculateAdjustedWeight(portfolioValue: BigDecimal, amountToInvest: BigDecimal, weight: Double): Double =
    portfolioValue.multiply(weight.toDecimalRepresentation())
        .add(amountToInvest)
        .divide(portfolioValue)
        .toDouble() * 100

// calculate the amount to invest from the amountGap and adjusted target
fun calculateAdjustedAmountToInvest(
    ticker: String,
    amountGap: BigDecimal,
    adjustedTarget: Double,
    underWeightAmounts: Map<String, BigDecimal>
): BigDecimal =
    (amountGap * adjustedTarget.toBigDecimal() + (underWeightAmounts[ticker] ?: BigDecimal.ZERO))
        .setScale(2, RoundingMode.HALF_DOWN)

fun Double.toDecimalRepresentation(): Double = this / HUNDRED_PERCENT

fun Int?.orZero(): Int = this ?: 0

fun BigDecimal.multiply(value: Double): BigDecimal = this.multiply(value.toBigDecimal(mathContext), mathContext)

fun BigDecimal.divide(value: Double): BigDecimal = this.divide(value.toBigDecimal(mathContext), mathContext)
