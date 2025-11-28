package io.github.dca.strategy

import io.github.dca.*
import io.github.dca.math.linear.*
import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP

private const val MAX_SIMPLEX_ITERATIONS = 1000
private const val ALLOCATION_SCALE = 2

/**
 * Implements portfolio optimization using linear programming (LP) techniques.
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
fun distributeByLinearProgramming(request: DcaRequest): Distribution =
    filterAssetsByLinearConstraints(request.assets, request.strategy.thresholds)
        .let { eligibleAssets ->
            when {
                eligibleAssets.isEmpty() -> emptyMap()
                request.strategy.diversify -> distributeDiversified(request, eligibleAssets)
                else -> solveOptimizationProblem(request, eligibleAssets)
            }
        }

private fun filterAssetsByLinearConstraints(assets: List<Asset>, thresholds: Thresholds): List<Asset> =
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

/**
 * Determines if a concentration cap should be applied to prevent extreme single-asset allocation.
 *
 * Smart capping logic applies a 90% maximum allocation when:
 * 1. Three or more eligible assets exist (multiple opportunities to diversify)
 * 2. Top two assets have similar priorities (within 50% - competition is close)
 * 3. All deviations are small (< 5% from target - minor rebalancing only)
 *
 * @param assets List of eligible assets to analyze
 * @param priorities Calculated priority scores for each asset
 * @return true if a 90% cap should be applied to prevent concentration
 */
private fun shouldApplyConcentrationCap(assets: List<Asset>, priorities: List<Double>): Boolean {
    val n = assets.size

    // Always cap when 3+ assets available (multiple opportunities)
    if (n >= 3) return true

    // Never cap with only 1 asset (no choice)
    if (n == 1) return false

    // For 2 assets, cap if priorities are close (competitive choice)
    if (n == 2) {
        val sortedPriorities = priorities.sortedDescending()
        val topPriority = sortedPriorities[0]
        val secondPriority = sortedPriorities[1]

        // If second is within 50% of top, they're competitive - apply cap
        val ratio = secondPriority / topPriority
        if (ratio >= 0.50) return true
    }

    // Cap if all deviations are small (minor rebalancing - < 5% from target)
    val maxDeviation = assets.maxOf { (it.target - it.weight).coerceAtLeast(0.0) }
    if (maxDeviation < 5.0) return true

    return false
}

private fun solveOptimizationProblem(request: DcaRequest, eligibleAssets: List<Asset>): Distribution {
    val n = eligibleAssets.size
    val totalAmount = request.amount.toDouble()

    return try {
        // Calculate priorities first (needed for both objective function and smart cap logic)
        val priorities = eligibleAssets.map { asset ->
            val deviation = (asset.target - asset.weight).coerceAtLeast(0.0)
            (deviation * asset.target).coerceAtLeast(1e-10)
        }

        // Determine if concentration cap should be applied
        val smartCapSuggested = shouldApplyConcentrationCap(eligibleAssets, priorities)
        val maxAllocationPct = when {
            // Explicit user override takes precedence
            request.strategy.maxSingleAssetPct != null -> request.strategy.maxSingleAssetPct
            // Apply smart cap (90%) if suggested
            smartCapSuggested -> 0.90
            // Otherwise no cap (100%)
            else -> 1.0
        }

        // Use linear programming to maximize priority-weighted allocation
        // Priority is based on how far below target the asset is and its target weight
        // Since we minimize, negate the priority to maximize allocation to high-priority assets
        val objectiveCoefficients = priorities.map { -it }.toDoubleArray()

        val objective = LinearObjectiveFunction(objectiveCoefficients, 0.0)

        // Constraints
        val constraints = mutableListOf<LinearConstraint>()

        // Equality constraint: sum of allocations = total amount
        val equalityCoefficients = DoubleArray(n) { 1.0 }
        constraints.add(LinearConstraint(equalityCoefficients, Relationship.EQ, totalAmount))

        // Non-negativity and optional max allocation constraints
        for (i in 0 until n) {
            val coefficients = DoubleArray(n) { 0.0 }
            coefficients[i] = 1.0

            // Min: allocation_i >= 0
            constraints.add(LinearConstraint(coefficients, Relationship.GEQ, 0.0))

            // Max: allocation_i <= maxAllocationPct * total (if capped)
            if (maxAllocationPct < 1.0) {
                constraints.add(
                    LinearConstraint(coefficients, Relationship.LEQ, totalAmount * maxAllocationPct)
                )
            }
        }

        // Solve the linear programming problem
        val solution = SimplexSolver(maxIterations = MAX_SIMPLEX_ITERATIONS)
            .optimize(
                objective = objective,
                constraints = LinearConstraintSet(constraints),
                goalType = GoalType.MINIMIZE
            )

        // Convert solution to distribution map
        eligibleAssets.mapIndexed { index, asset ->
            asset.ticker to BigDecimal(solution.point[index]).setScale(ALLOCATION_SCALE, HALF_UP)
        }.toMap()

    } catch (_: Exception) {
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