package io.github.dca.strategy

import io.github.dca.*
import io.github.dca.math.linear.*
import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP

private const val MAX_SIMPLEX_ITERATIONS = 1000
private const val ALLOCATION_SCALE = 2

/**
 * Implements portfolio optimization using convex optimization techniques.
 *
 * This strategy formulates the DCA distribution problem as a linear programming problem:
 *
 * maximize: priority-weighted allocation where priority = deviation * target_weight
 * subject to: Σ(allocation_i) = total_amount
 *            allocation_i ≥ 0 (no short selling)
 *            ATH constraints (assets below ATH threshold get zero allocation)
 *
 * The objective prioritizes assets that are:
 * 1. Further below their target weight (higher deviation)
 * 2. Have higher target allocations
 *
 * IMPORTANT: Linear Programming Corner Solutions
 * ==============================================
 * Linear programming with a linear objective function produces SPARSE SOLUTIONS at corner points
 * of the feasible region. This means the optimizer will typically allocate all (or most) funds to
 * the asset(s) with the highest priority score.
 *
 * Example (diversify=false):
 * - Asset A: priority = 70.0 (deviation=1.4 × target=50.0)
 * - Asset B: priority = 27.5 (deviation=2.5 × target=11.0)
 * - Asset C: priority = 18.0 (deviation=0.9 × target=20.0)
 *
 * Result: Asset A receives 100% of allocation (mathematically optimal corner solution)
 *
 * Diversification Mode (diversify=true):
 * ======================================
 * When diversify=true, allocations are distributed proportionally based on priority scores
 * to encourage spreading investments across multiple assets.
 *
 * Example (diversify=true, same assets as above):
 * - Asset A: (70.0 / 115.5) × $10,000 = $6,061.69
 * - Asset B: (27.5 / 115.5) × $10,000 = $2,380.95
 * - Asset C: (18.0 / 115.5) × $10,000 = $1,557.36
 *
 * This provides a balance between priority-based optimization and portfolio diversification.
 */
fun distributeByConvexOptimization(request: DcaRequest): Distribution =
    filterAssetsByConvexConstraints(request.assets, request.strategy.thresholds)
        .let { eligibleAssets ->
            when {
                eligibleAssets.isEmpty() -> emptyMap()
                request.strategy.diversify -> distributeDiversified(request, eligibleAssets)
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

/**
 * Distributes funds proportionally based on priority scores to encourage diversification.
 *
 * Instead of solving the LP problem (which produces corner solutions), this allocates
 * proportionally: allocation_i = (priority_i / total_priority) × total_amount
 *
 * This spreads investments across all eligible assets while still respecting priorities.
 */
private fun distributeDiversified(request: DcaRequest, eligibleAssets: List<Asset>): Distribution {
    // Calculate priority for each asset
    val priorities = eligibleAssets.map { asset ->
        val deviation = (asset.target - asset.weight).coerceAtLeast(0.0)
        val priority = deviation * asset.target
        priority.coerceAtLeast(1e-10) // Prevent division by zero
    }

    val totalPriority = priorities.sum()

    if (totalPriority <= 0.0) {
        // Fallback to equal distribution if all priorities are zero
        return fallbackDistribution(request, eligibleAssets)
    }

    // Distribute proportionally based on priority scores
    return eligibleAssets.mapIndexed { index, asset ->
        val proportion = priorities[index] / totalPriority
        asset.ticker to request.amount.multiply(proportion.toBigDecimal())
            .setScale(ALLOCATION_SCALE, HALF_UP)
    }.toMap()
}

private fun solveOptimizationProblem(request: DcaRequest, eligibleAssets: List<Asset>): Distribution {
    val n = eligibleAssets.size
    val totalAmount = request.amount.toDouble()

    return try {
        // Use linear programming to maximize priority-weighted allocation
        // Priority is based on how far below target the asset is and its target weight
        val objectiveCoefficients = eligibleAssets.map { asset ->
            val targetWeight = asset.target
            val currentWeight = asset.weight
            val deviation = (targetWeight - currentWeight).coerceAtLeast(0.0)

            // Priority score: allocate more to assets with:
            // 1. Higher deviation from target (further below target)
            // 2. Higher target weight (more important in portfolio)
            // Since we minimize, negate the priority to maximize allocation to high-priority assets
            val priority = deviation * targetWeight
            -priority.coerceAtLeast(1e-10) // Prevent division by zero, ensure negative for maximization
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
        val solution = SimplexSolver(maxIterations = MAX_SIMPLEX_ITERATIONS).optimize(
            objective,
            LinearConstraintSet(constraints),
            GoalType.MINIMIZE
        )

        // Convert solution to distribution map
        eligibleAssets.mapIndexed { index, asset ->
            asset.ticker to BigDecimal(solution.point[index]).setScale(ALLOCATION_SCALE, HALF_UP)
        }.toMap()

    } catch (e: Exception) {
        // Fallback to simple proportional distribution if optimization fails
        // This can happen if the problem is infeasible or the solver encounters numerical issues
        fallbackDistribution(request, eligibleAssets)
    }
}


/**
 * Fallback distribution when optimization fails.
 * Distributes proportionally based on target weights, or equally if no targets exist.
 */
private fun fallbackDistribution(request: DcaRequest, eligibleAssets: List<Asset>): Distribution {
    val totalTarget = eligibleAssets.sumOf { it.target }

    return if (totalTarget > 0) {
        eligibleAssets.associate { asset ->
            val proportion = asset.target / totalTarget
            asset.ticker to request.amount.multiply(BigDecimal(proportion)).setScale(ALLOCATION_SCALE, HALF_UP)
        }
    } else {
        // Equal distribution if no targets
        val equalAmount = request.amount.divide(BigDecimal(eligibleAssets.size), ALLOCATION_SCALE, HALF_UP)
        eligibleAssets.associate { asset ->
            asset.ticker to equalAmount
        }
    }
}