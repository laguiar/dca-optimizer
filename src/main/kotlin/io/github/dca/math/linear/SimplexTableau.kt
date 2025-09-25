package io.github.dca.math.linear

import kotlin.math.abs

/**
 * A tableau representation of a linear programming problem.
 * The tableau is used by the Simplex algorithm to solve linear programming problems.
 */
internal class SimplexTableau(
    private val objective: LinearObjectiveFunction,
    private val constraints: Collection<LinearConstraint>,
    private val goalType: GoalType,
    private val epsilon: Double = 1e-6
) {
    private val numVars: Int = objective.dimension
    private val numConstraints: Int = constraints.size
    private val numSlackVars: Int = constraints.count { it.relationship == Relationship.LEQ || it.relationship == Relationship.GEQ }
    private val numArtificialVars: Int = constraints.count { it.relationship != Relationship.LEQ }

    private val tableauWidth: Int = numVars + numSlackVars + numArtificialVars + 1 // +1 for RHS
    private val tableauHeight: Int = numConstraints + 1 // +1 for objective row

    private val tableau: Array<DoubleArray> = Array(tableauHeight) { DoubleArray(tableauWidth) }
    private val basicVariables: IntArray = IntArray(numConstraints)

    private var phase: Int = if (numArtificialVars > 0) 1 else 2

    init {
        initializeTableau()
    }

    private fun initializeTableau() {
        // Initialize with zeros
        for (i in tableau.indices) {
            tableau[i].fill(0.0)
        }

        // Set up objective row (last row)
        val objectiveRow = tableau[tableauHeight - 1]
        for (i in 0 until numVars) {
            objectiveRow[i] = if (goalType == GoalType.MAXIMIZE) -objective.coefficients[i] else objective.coefficients[i]
        }
        objectiveRow[tableauWidth - 1] = if (goalType == GoalType.MAXIMIZE) -objective.constantTerm else objective.constantTerm

        // Set up constraint rows
        var slackVarIndex = numVars
        var artificialVarIndex = numVars + numSlackVars
        var basicVarIndex = 0

        constraints.forEachIndexed { constraintIndex, constraint ->
            val row = tableau[constraintIndex]

            // Copy constraint coefficients
            for (i in 0 until numVars) {
                row[i] = constraint.coefficients[i]
            }

            // Set RHS value
            row[tableauWidth - 1] = constraint.value

            when (constraint.relationship) {
                Relationship.LEQ -> {
                    // Add slack variable
                    row[slackVarIndex] = 1.0
                    basicVariables[constraintIndex] = slackVarIndex
                    slackVarIndex++
                }
                Relationship.GEQ -> {
                    // Subtract slack variable and add artificial variable
                    row[slackVarIndex] = -1.0
                    row[artificialVarIndex] = 1.0
                    basicVariables[constraintIndex] = artificialVarIndex
                    slackVarIndex++
                    artificialVarIndex++
                }
                Relationship.EQ -> {
                    // Add artificial variable only
                    row[artificialVarIndex] = 1.0
                    basicVariables[constraintIndex] = artificialVarIndex
                    artificialVarIndex++
                }
            }
        }

        // If we have artificial variables, set up Phase I objective
        if (numArtificialVars > 0) {
            setupPhase1Objective()
        }
    }

    private fun setupPhase1Objective() {
        // Phase I: minimize sum of artificial variables
        val objectiveRow = tableau[tableauHeight - 1]
        objectiveRow.fill(0.0)

        // Set coefficients for artificial variables to 1
        var artificialVarIndex = numVars + numSlackVars
        repeat(numArtificialVars) {
            objectiveRow[artificialVarIndex] = 1.0
            artificialVarIndex++
        }

        // Eliminate artificial variables from objective row
        for (i in 0 until numConstraints) {
            val basicVar = basicVariables[i]
            if (basicVar >= numVars + numSlackVars) { // It's an artificial variable
                subtractRow(tableauHeight - 1, i, 1.0)
            }
        }
    }

    private fun setupPhase2Objective() {
        // Phase II: use original objective
        val objectiveRow = tableau[tableauHeight - 1]

        // Clear the objective row
        objectiveRow.fill(0.0)

        // Set original objective coefficients
        for (i in 0 until numVars) {
            objectiveRow[i] = if (goalType == GoalType.MAXIMIZE) -objective.coefficients[i] else objective.coefficients[i]
        }
        objectiveRow[tableauWidth - 1] = if (goalType == GoalType.MAXIMIZE) -objective.constantTerm else objective.constantTerm

        // Eliminate basic variables from objective row
        for (i in 0 until numConstraints) {
            val basicVar = basicVariables[i]
            if (basicVar < numVars && abs(objectiveRow[basicVar]) > epsilon) {
                val coefficient = objectiveRow[basicVar]
                // Only eliminate the basic variable's coefficient, preserving the tableau structure
                // by subtracting coefficient * (constraint row) to make basic variable coefficient zero
                subtractRow(tableauHeight - 1, i, coefficient)
            }
        }
    }

    private fun subtractRow(targetRow: Int, sourceRow: Int, multiplier: Double) {
        for (j in tableau[targetRow].indices) {
            tableau[targetRow][j] -= multiplier * tableau[sourceRow][j]
        }
    }

    /**
     * Finds the pivot column using the specified pivot selection rule.
     */
    fun getPivotColumn(pivotSelectionRule: PivotSelectionRule): Int {
        val objectiveRow = tableau[tableauHeight - 1]

        return when (pivotSelectionRule) {
            PivotSelectionRule.DANTZIG -> {
                var pivotCol = -1
                var mostNegative = 0.0
                for (i in 0 until tableauWidth - 1) {
                    if (objectiveRow[i] < mostNegative) {
                        mostNegative = objectiveRow[i]
                        pivotCol = i
                    }
                }
                pivotCol
            }
            PivotSelectionRule.BLAND -> {
                var pivotCol = -1
                for (i in 0 until tableauWidth - 1) {
                    if (objectiveRow[i] < -epsilon) {
                        pivotCol = i
                        break
                    }
                }
                pivotCol
            }
        }
    }

    /**
     * Finds the pivot row for the given pivot column using the minimum ratio test.
     */
    fun getPivotRow(pivotColumn: Int): Int {
        if (pivotColumn < 0) return -1

        var pivotRow = -1
        var minRatio = Double.POSITIVE_INFINITY

        for (i in 0 until numConstraints) {
            val pivotElement = tableau[i][pivotColumn]
            val rhs = tableau[i][tableauWidth - 1]

            if (pivotElement > epsilon) {
                val ratio = rhs / pivotElement
                if (ratio >= 0 && ratio < minRatio) {
                    minRatio = ratio
                    pivotRow = i
                }
            }
        }

        return pivotRow
    }

    /**
     * Performs a pivot operation on the tableau.
     */
    fun performPivot(pivotRow: Int, pivotColumn: Int) {
        val pivotElement = tableau[pivotRow][pivotColumn]

        if (abs(pivotElement) <= epsilon) {
            throw InvalidLinearProgramException("Pivot element is too small: $pivotElement")
        }

        // Update basic variable
        basicVariables[pivotRow] = pivotColumn

        // Scale pivot row
        for (j in tableau[pivotRow].indices) {
            tableau[pivotRow][j] /= pivotElement
        }

        // Eliminate pivot column in other rows
        for (i in tableau.indices) {
            if (i != pivotRow) {
                val multiplier = tableau[i][pivotColumn]
                if (abs(multiplier) > epsilon) {
                    for (j in tableau[i].indices) {
                        tableau[i][j] -= multiplier * tableau[pivotRow][j]
                    }
                }
            }
        }
    }

    /**
     * Checks if the current solution is optimal.
     */
    fun isOptimal(): Boolean {
        val objectiveRow = tableau[tableauHeight - 1]

        // Standard optimality check: all coefficients >= 0 (for minimization form)
        val standardOptimal = (0 until tableauWidth - 1).all { objectiveRow[it] >= -epsilon }

        // Additional check for potential unboundedness in Phase II
        if (standardOptimal && phase == 2 && goalType == GoalType.MAXIMIZE) {
            // For maximization problems in Phase II, if all objective coefficients are zero
            // (not just non-negative), this might indicate an issue with the tableau setup
            val allZero = (0 until numVars).all { kotlin.math.abs(objectiveRow[it]) <= epsilon }
            if (allZero) {
                // Check if we can still improve by looking for non-basic variables
                // that appear in constraints with positive coefficients
                for (j in 0 until numVars) {
                    val isBasic = basicVariables.any { it == j }
                    if (!isBasic) {
                        // Check if this non-basic variable has positive coefficients in constraints
                        val hasPositiveConstraints = (0 until numConstraints).any {
                            tableau[it][j] > epsilon
                        }
                        if (hasPositiveConstraints) {
                            // This suggests the problem might be unbounded
                            // Let the algorithm continue to proper unboundedness detection
                            return false
                        }
                    }
                }
            }
        }

        return standardOptimal
    }

    /**
     * Checks if the problem is unbounded.
     */
    fun isUnbounded(pivotColumn: Int): Boolean {
        if (pivotColumn < 0) return false
        return (0 until numConstraints).all { tableau[it][pivotColumn] <= epsilon }
    }

    /**
     * Checks if Phase I solution is feasible (all artificial variables are zero).
     */
    fun isPhase1Feasible(): Boolean {
        val objectiveValue = tableau[tableauHeight - 1][tableauWidth - 1]
        return abs(objectiveValue) <= epsilon
    }

    /**
     * Transitions from Phase I to Phase II.
     */
    fun transitionToPhase2() {
        if (phase != 1) return

        phase = 2
        removeArtificialVariables()
        setupPhase2Objective()
    }

    private fun removeArtificialVariables() {
        // Remove artificial variable columns from tableau
        val newTableauWidth = numVars + numSlackVars + 1
        val newTableau = Array(tableauHeight) { DoubleArray(newTableauWidth) }

        for (i in tableau.indices) {
            // Copy original variables and slack variables
            System.arraycopy(tableau[i], 0, newTableau[i], 0, numVars + numSlackVars)
            // Copy RHS
            newTableau[i][newTableauWidth - 1] = tableau[i][tableauWidth - 1]
        }

        // Update tableau reference (in a real implementation, we'd need to handle this differently)
        // For now, we'll keep the original tableau size but zero out artificial variable columns
        for (i in tableau.indices) {
            for (j in numVars + numSlackVars until tableauWidth - 1) {
                tableau[i][j] = 0.0
            }
        }
    }

    /**
     * Extracts the current solution from the tableau.
     */
    fun getSolution(): PointValuePair {
        val solution = DoubleArray(numVars)

        // Initialize all variables to zero (non-basic variables)
        solution.fill(0.0)

        // Set basic variables to their values
        for (i in 0 until numConstraints) {
            val basicVar = basicVariables[i]
            if (basicVar < numVars) {
                solution[basicVar] = tableau[i][tableauWidth - 1]
            }
        }

        // Calculate objective value directly from the solution and original objective
        var objectiveValue = objective.constantTerm
        for (i in 0 until numVars) {
            objectiveValue += objective.coefficients[i] * solution[i]
        }

        return PointValuePair(solution, objectiveValue)
    }

    /**
     * Gets the current phase (1 or 2).
     */
    fun getCurrentPhase(): Int = phase

    /**
     * Debug method to get the tableau RHS value.
     */
    fun getTableauRHS(): Double = tableau[tableauHeight - 1][tableauWidth - 1]

    /**
     * Debug method to print the current tableau state.
     */
    fun printTableau() {
        println("Phase $phase Tableau:")
        println("Dimensions: numVars=$numVars, numSlackVars=$numSlackVars, numArtificialVars=$numArtificialVars")
        println("Basic variables: ${basicVariables.contentToString()}")
        for (i in tableau.indices) {
            println(tableau[i].contentToString())
        }
        println()
    }
}