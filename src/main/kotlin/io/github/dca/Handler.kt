package io.github.dca

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import javax.enterprise.context.ApplicationScoped

typealias Distribution = Map<String, BigDecimal>

private val mathContext = MathContext.DECIMAL32
private const val HUNDRED_PERCENT = 100.0
private const val ZERO = 0.0

@ApplicationScoped
class DcaHandler {

    fun optimize(request: DcaRequest): DcaResponse =
        when (request.strategy.type) {
            StrategyType.TARGET -> calculateDistribution(request, request.strategy, ::distributeByTarget)
            StrategyType.WEIGHT -> calculateDistribution(request, request.strategy, ::distributeByWeight)
            StrategyType.PORTFOLIO -> calculateDistribution(request, request.strategy, ::distributeByPortfolio)
            StrategyType.RATING -> calculateDistribution(request, request.strategy, ::distributeByRating)
            else -> TODO()
        }

    private fun calculateDistribution(
        request: DcaRequest,
        strategy: DcaStrategy,
        applyStrategy: (DcaRequest, DcaStrategy) -> Distribution
    ): DcaResponse =
        applyStrategy(request, strategy)
            .let(::DcaResponse)
}

private fun distributeByTarget(request: DcaRequest, strategy: DcaStrategy): Distribution =
    filterAssetsByWeightAndAth(request.assets, strategy.thresholds)
        .let { filteredAssets ->
            val targetLeft = HUNDRED_PERCENT - filteredAssets.sumOf { it.target }
            val targetFactor = targetLeft / (HUNDRED_PERCENT - targetLeft)

            filteredAssets.associate { asset ->
                val strategyFactor = (asset.target + (asset.target * targetFactor)).toDecimalRepresentation()
                asset.ticker to request.amount.calculateDistribution(strategyFactor)
            }
        }

private fun distributeByWeight(request: DcaRequest, strategy: DcaStrategy): Distribution =
    filterAssetsByWeightAndAth(request.assets, strategy.thresholds)
        .let { filteredAssets ->
            val totalWeightGap = filteredAssets.sumOf { it.target - it.weight }

            filteredAssets.associate { asset ->
                val strategyFactor = (asset.target - asset.weight) / totalWeightGap
                asset.ticker to request.amount.calculateDistribution(strategyFactor)
            }
        }

private fun distributeByPortfolio(request: DcaRequest, strategy: DcaStrategy): Distribution {
    // sum the total percentage of over target assets and divide it by the number of under target assets
    val targetFactor = request.assets.sumOf { asset ->
        when {
            !isWeightBellowTarget(asset, strategy.thresholds) -> asset.weight - asset.target
            else -> ZERO
        }
    }.div(
        request.assets.count { isWeightBellowTarget(it, strategy.thresholds) }
    )

    return request.assets.associate { asset ->
        val adjustedTarget = when {
            // increase the asset target by the targetFactor
            isWeightBellowTarget(asset, strategy.thresholds) -> asset.target + targetFactor
            // reduce the over target assets by its over target amount
            else -> asset.target - (asset.weight - asset.target)
        }.toDecimalRepresentation()

        asset.ticker to request.amount.calculateDistribution(adjustedTarget)
    }
}

private fun distributeByRating(request: DcaRequest, strategy: DcaStrategy): Distribution =
    request.assets.sumOf { it.rating.orZero().toDouble() }
        .let { sum ->
            request.assets.associate { asset ->
                val weight = asset.rating.orZero().div(sum)
                asset.ticker to request.amount.calculateDistribution(weight)
            }
        }

private fun filterAssetsByWeightAndAth(assets: List<Asset>, thresholds: Thresholds): List<Asset> =
    assets
        .filter { isWeightBellowTarget(it, thresholds) }
        .filter { isBellowAthThreshold(it, thresholds) }

private fun isWeightBellowTarget(asset: Asset, thresholds: Thresholds) =
    asset.weight <= (asset.target + thresholds.overTarget)

private fun isBellowAthThreshold(asset: Asset, thresholds: Thresholds) =
    // if not set, the asset is included anyway
    when (asset.fromAth) {
        ZERO -> true // default strategy value or just in ath by luck
        else -> asset.fromAth >= thresholds.fromAth
    }

private fun BigDecimal.calculateDistribution(percentage: Double): BigDecimal =
    this.multiply(BigDecimal(percentage.toString()), mathContext)
        .setScale(2, RoundingMode.HALF_DOWN)

private fun Double.toDecimalRepresentation(): Double = this / HUNDRED_PERCENT

fun Int?.orZero(): Int = this ?: 0
