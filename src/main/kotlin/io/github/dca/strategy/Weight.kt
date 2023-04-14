package io.github.dca.strategy

import io.github.dca.Asset
import io.github.dca.DcaRequest
import io.github.dca.Distribution
import io.github.dca.Thresholds
import io.github.dca.ZERO
import io.github.dca.calculateAdjustedAmountToInvest
import io.github.dca.calculateAdjustedWeight
import io.github.dca.calculateDistribution
import io.github.dca.extractCalculationBasis
import java.math.BigDecimal

fun distributeByWeight(request: DcaRequest): Distribution =
    filterAssetsByWeightAndAth(request.assets, request.strategy.thresholds)
        .let { filteredAssets ->
            val (totalNeedForRebalance, underWeightAmounts) = extractCalculationBasis(request, filteredAssets)

            when (request.amount > totalNeedForRebalance) {
                true -> rebalancePortfolioByWeight(request, totalNeedForRebalance, underWeightAmounts)
                false -> rebalanceUnderWeightedAssets(filteredAssets, request)
            }
        }

private fun rebalancePortfolioByWeight(
    request: DcaRequest,
    totalNeedForRebalance: BigDecimal,
    underWeightAmounts: Map<String, BigDecimal>
): Distribution {
    val amountGap = request.amount - totalNeedForRebalance
    // define amount to invest on under-weighted assets from totalNeedForRebalance
    return request.assets.map { asset ->
        val amountToInvest = underWeightAmounts[asset.ticker] ?: BigDecimal.ZERO
        Asset(
            ticker = asset.ticker,
            weight = calculateAdjustedWeight(request.portfolioValueOrZero(), amountToInvest, asset.weight),
            target = asset.target
        )
    }.let { updatedAssets ->
        calculateTargetByPortfolio(updatedAssets)
            .entries.associate { (ticker, adjustedTarget) ->
                ticker to calculateAdjustedAmountToInvest(
                    ticker,
                    amountGap,
                    adjustedTarget,
                    underWeightAmounts
                )
            }
    }
}

private fun rebalanceUnderWeightedAssets(
    filteredAssets: List<Asset>,
    request: DcaRequest
): Distribution {
    // distribute the amount to invest among the under-weighted assets
    val totalWeightGap = filteredAssets.sumOf { it.target - it.weight }
    return filteredAssets.associate { asset ->
        val strategyFactor = (asset.target - asset.weight) / totalWeightGap
        asset.ticker to request.amount.calculateDistribution(strategyFactor)
    }
}

fun filterAssetsByWeightAndAth(assets: List<Asset>, thresholds: Thresholds): List<Asset> =
    assets
        .filter { it.isWeightBellowTarget }
        .filter { isBellowAthThreshold(it, thresholds) }

private fun isBellowAthThreshold(asset: Asset, thresholds: Thresholds) =
    // if not set, the asset is included anyway
    when (asset.fromAth) {
        ZERO -> true // default strategy value or just in ath by luck
        else -> asset.fromAth >= thresholds.fromAth
    }
