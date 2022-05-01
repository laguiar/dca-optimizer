package io.github.dca

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import javax.enterprise.context.ApplicationScoped

typealias Distribution = Map<String, BigDecimal>

private val mathContext = MathContext.DECIMAL32

@ApplicationScoped
class DcaHandler {

    fun optimize(request: DcaRequest): OptimizerResponse =
        (request.strategy ?: DcaStrategy.default()).let { strategy ->
            // validar target sum and weight sum
            request.assets
                .filter { weightSmallerThanTarget(it, strategy) }
                .filter { assetBellowAthThreshold(it, strategy) }
                .let { filteredAssets ->
                    val targetLeft = 100.0 - filteredAssets.sumOf { it.target }
                    val targetFactor = targetLeft / (100 - targetLeft)
                    val totalWeightGap = filteredAssets.sumOf { it.target - it.weight }

                    filteredAssets.associate { asset ->
                        val strategyFactor = when (strategy.calculationFactor) {
                            CalculationFactor.TARGET -> (asset.target + (asset.target * targetFactor)).toDecimalRepresentation()
                            CalculationFactor.WEIGHT -> (asset.target - asset.weight) / totalWeightGap
                        }

                        // map each asset to its final distribution amount
                        asset.ticker to request.amount.calculateDistribution(strategyFactor)

                    }.let(::OptimizerResponse)
                }
        }

    private fun weightSmallerThanTarget(asset: Asset, strategy: DcaStrategy) =
        asset.weight < (asset.target + strategy.overTargetThreshold)

    private fun assetBellowAthThreshold(asset: Asset, strategy: DcaStrategy) =
        // if num, the asset is included anyway
        when (asset.belowAth) {
            null -> true
            else -> asset.belowAth >= strategy.athThreshold
        }

}

private fun BigDecimal.calculateDistribution(percentage: Double): BigDecimal =
    this.multiply(BigDecimal(percentage.toString()), mathContext)
        .setScale(2, RoundingMode.HALF_DOWN)

private fun Double.toDecimalRepresentation(): Double = this / 100