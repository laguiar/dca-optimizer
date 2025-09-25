package io.github.dca.math.linear

/**
 * Solver for linear programming problems using the two-phase Simplex algorithm.
 *
 * The Simplex algorithm is used to find the optimal solution to linear programming problems
 * of the form:
 *
 * minimize (or maximize): c₁x₁ + c₂x₂ + ... + cₙxₙ
 * subject to: a₁₁x₁ + a₁₂x₂ + ... + a₁ₙxₙ {≤,=,≥} b₁
 *            a₂₁x₁ + a₂₂x₂ + ... + a₂ₙxₙ {≤,=,≥} b₂
 *            ...
 *            aₘ₁x₁ + aₘ₂x₂ + ... + aₘₙxₙ {≤,=,≥} bₘ
 *            x₁, x₂, ..., xₙ ≥ 0 (non-negativity constraints)
 */
class SimplexSolver(
    private val maxIterations: Int = 1000,
    private val epsilon: Double = 1e-6,
    private val pivotSelectionRule: PivotSelectionRule = PivotSelectionRule.DANTZIG
) {

    /**
     * Optimizes the given linear programming problem.
     *
     * @param objective the objective function to minimize or maximize
     * @param constraints the set of linear constraints
     * @param goalType whether to minimize or maximize the objective function
     * @param nonNegativeConstraint whether to enforce non-negativity constraints on all variables
     * @return the optimal solution as a PointValuePair
     * @throws NoFeasibleSolutionException if no feasible solution exists
     * @throws UnboundedSolutionException if the solution is unbounded
     * @throws TooManyIterationsException if the algorithm doesn't converge within maxIterations
     */
    fun optimize(
        objective: LinearObjectiveFunction,
        constraints: LinearConstraintSet,
        goalType: GoalType,
        nonNegativeConstraint: Boolean = true
    ): PointValuePair {
        validateProblem(objective, constraints)

        // Convert to standard form and create tableau
        val tableau = SimplexTableau(objective, constraints.constraints, goalType, epsilon)

        var iterations = 0

        // Phase I (if artificial variables are present)
        if (tableau.getCurrentPhase() == 1) {
            while (!tableau.isOptimal() && iterations < maxIterations) {
                val pivotColumn = tableau.getPivotColumn(pivotSelectionRule)

                if (pivotColumn < 0) break // Already optimal

                val pivotRow = tableau.getPivotRow(pivotColumn)

                if (pivotRow < 0) {
                    throw UnboundedSolutionException("Phase I problem is unbounded")
                }

                tableau.performPivot(pivotRow, pivotColumn)
                iterations++
            }

            if (iterations >= maxIterations) {
                throw TooManyIterationsException("Phase I did not converge", maxIterations)
            }

            // Check if Phase I solution is feasible
            if (!tableau.isPhase1Feasible()) {
                throw NoFeasibleSolutionException("No feasible solution found in Phase I")
            }

            // Transition to Phase II
            tableau.transitionToPhase2()
        }

        // Phase II
        while (!tableau.isOptimal() && iterations < maxIterations) {
            val pivotColumn = tableau.getPivotColumn(pivotSelectionRule)

            if (pivotColumn < 0) break // Already optimal

            if (tableau.isUnbounded(pivotColumn)) {
                throw UnboundedSolutionException("Solution is unbounded in Phase II")
            }

            val pivotRow = tableau.getPivotRow(pivotColumn)

            if (pivotRow < 0) {
                throw UnboundedSolutionException("No valid pivot row found")
            }

            tableau.performPivot(pivotRow, pivotColumn)
            iterations++
        }

        if (iterations >= maxIterations) {
            throw TooManyIterationsException("Phase II did not converge", maxIterations)
        }

        return tableau.getSolution()
    }

    private fun validateProblem(objective: LinearObjectiveFunction, constraints: LinearConstraintSet) {
        if (constraints.isEmpty) {
            throw InvalidLinearProgramException("No constraints provided")
        }

        val expectedDimension = objective.dimension

        for (constraint in constraints) {
            if (constraint.dimension != expectedDimension) {
                throw InvalidLinearProgramException(
                    "Constraint dimension (${constraint.dimension}) does not match objective dimension ($expectedDimension)"
                )
            }
        }

        // Check for invalid constraint values
        for (constraint in constraints) {
            if (!constraint.value.isFinite()) {
                throw InvalidLinearProgramException("Constraint contains non-finite value: ${constraint.value}")
            }

            for (coeff in constraint.coefficients) {
                if (!coeff.isFinite()) {
                    throw InvalidLinearProgramException("Constraint contains non-finite coefficient: $coeff")
                }
            }
        }

        // Check objective function
        for (coeff in objective.coefficients) {
            if (!coeff.isFinite()) {
                throw InvalidLinearProgramException("Objective function contains non-finite coefficient: $coeff")
            }
        }

        if (!objective.constantTerm.isFinite()) {
            throw InvalidLinearProgramException("Objective function contains non-finite constant term: ${objective.constantTerm}")
        }
    }
}