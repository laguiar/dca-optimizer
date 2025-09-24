package io.github.dca.strategy

import io.github.dca.*

fun distributeByPortfolio(request: DcaRequest): Distribution =
    calculateTargetByPortfolio(request.assets)
        .entries
        .associate { (ticker, adjustedTarget) ->
            ticker to request.amount.calculateDistribution(adjustedTarget)
        }

fun calculateTargetByPortfolio(assets: List<Asset>): Map<String, Double> {
    // sum the total percentage of over-weighted assets and divide it by the number of under-weighted assets
    val targetFactor = assets.sumOf { asset ->
        if (asset.isWeightBellowTarget.not()) asset.weight - asset.target else ZERO
    }.let { sum ->
        val count = assets.count { it.isWeightBellowTarget }
        sum / if (count > 0) count else 1
    }
    return assets.associate { asset ->
        val adjustedTarget = when {
            // increase the asset target by the targetFactor
            asset.isWeightBellowTarget -> asset.target + targetFactor
            // reduce the over target assets by its over target amount
            else -> asset.target - (asset.weight - asset.target)
        }.toDecimalRepresentation()

        asset.ticker to adjustedTarget
    }
}
