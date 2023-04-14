package io.github.dca

import io.github.dca.strategy.filterAssetsByWeightAndAth

fun distributeByTarget(request: DcaRequest): Distribution =
    filterAssetsByWeightAndAth(request.assets, request.strategy.thresholds)
        .let { filteredAssets ->
            val targetLeft = HUNDRED_PERCENT - filteredAssets.sumOf { it.target }
            val targetFactor = targetLeft / (HUNDRED_PERCENT - targetLeft)

            filteredAssets.associate { asset ->
                val strategyFactor = (asset.target + (asset.target * targetFactor)).toDecimalRepresentation()
                asset.ticker to request.amount.calculateDistribution(strategyFactor)
            }
        }

fun distributeByRating(request: DcaRequest): Distribution =
    request.assets.sumOf { it.rating.orZero().toDouble() }
        .let { sum ->
            request.assets.associate { asset ->
                val weight = asset.rating.orZero().div(sum)
                asset.ticker to request.amount.calculateDistribution(weight)
            }
        }
