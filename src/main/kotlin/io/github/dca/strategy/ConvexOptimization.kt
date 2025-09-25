package io.github.dca.strategy

import io.github.dca.*
import io.github.dca.math.linear.*
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Implements portfolio optimization using convex optimization techniques.
 *
 * This strategy formulates the DCA distribution problem as a linear programming problem:
 *
 * minimize: penalty for deviating from target proportions
 * subject to: Σ(allocation_i) = total_amount
 *            allocation_i ≥ 0 (no short selling)
 *            ATH constraints (assets below ATH threshold get zero allocation)
 */
fun distributeByConvexOptimization(request: DcaRequest): Distribution =
    filterAssetsByConvexConstraints(request.assets, request.strategy.thresholds)
        .let { eligibleAssets ->
            when {
                eligibleAssets.isEmpty() -> emptyMap()
                else -> solveOptimizationProblem(request, eligibleAssets)
            }
        }

private fun filterAssetsByConvexConstraints(assets: List<Asset>, thresholds: Thresholds): List<Asset> =
    assets.filter { asset ->
        // Include assets that are below target weight
        asset.isWeightBellowTarget &&
                // Include assets that meet ATH threshold (if specified)
                (asset.fromAth == ZERO || asset.fromAth >= thresholds.fromAth)
    }

private fun solveOptimizationProblem(request: DcaRequest, eligibleAssets: List<Asset>): Distribution {
    val n = eligibleAssets.size
    val totalAmount = request.amount.toDouble()

    return try {
        // Use linear programming to minimize weighted deviation from targets
        // Objective: minimize sum of (target_weight * deviation_penalty)
        val objectiveCoefficients = eligibleAssets.map { asset ->
            // Higher penalty for assets that are further from their targets
            val targetWeight = asset.target
            val currentWeight = asset.weight
            val deviation = (targetWeight - currentWeight).coerceAtLeast(0.0)

            // Weight by deviation and inverse target to prioritize assets that are:
            // 1. Further below their target (higher deviation)
            // 2. Have higher target allocations
            val deviationWeight = if (deviation > 0) 1.0 / deviation else 1.0
            val targetPriority = if (targetWeight > 0) 1.0 / targetWeight else 1.0

            // Combine both factors (minimize means we want lower coefficients for higher priority)
            deviationWeight * targetPriority
        }.toDoubleArray()

        val objective = LinearObjectiveFunction(objectiveCoefficients, 0.0)

        // Constraints
        val constraints = mutableListOf<LinearConstraint>()

        // Equality constraint: sum of allocations = total amount
        val equalityCoefficients = DoubleArray(n) { 1.0 }
        constraints.add(LinearConstraint(equalityCoefficients, Relationship.EQ, totalAmount))

        // Non-negativity constraints: allocation_i >= 0
        for (i in 0 until n) {
            val coefficients = DoubleArray(n) { 0.0 }
            coefficients[i] = 1.0
            constraints.add(LinearConstraint(coefficients, Relationship.GEQ, 0.0))
        }

        // Solve the linear programming problem
        val solution = SimplexSolver(maxIterations = 1000).optimize(
            objective,
            LinearConstraintSet(constraints),
            GoalType.MINIMIZE
        )

        // Convert solution to distribution map
        eligibleAssets.mapIndexed { index, asset ->
            asset.ticker to BigDecimal(solution.point[index]).setScale(2, RoundingMode.HALF_UP)
        }.toMap()

    } catch (_: Exception) {
        // Fallback to simple proportional distribution if optimization fails
        fallbackDistribution(request, eligibleAssets)
    }
}


private fun fallbackDistribution(request: DcaRequest, eligibleAssets: List<Asset>): Distribution {
    // Simple proportional fallback based on target weights
    val totalTarget = eligibleAssets.sumOf { it.target }

    return if (totalTarget > 0) {
        eligibleAssets.associate { asset ->
            val proportion = asset.target / totalTarget
            asset.ticker to request.amount.multiply(BigDecimal(proportion)).setScale(2, RoundingMode.HALF_UP)
        }
    } else {
        // Equal distribution if no targets
        val equalAmount = request.amount.divide(BigDecimal(eligibleAssets.size), 2, RoundingMode.HALF_UP)
        eligibleAssets.associate { asset ->
            asset.ticker to equalAmount
        }
    }
}