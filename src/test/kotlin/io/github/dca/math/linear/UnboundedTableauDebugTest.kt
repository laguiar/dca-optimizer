package io.github.dca.math.linear

import org.junit.jupiter.api.Test

class UnboundedTableauDebugTest {

    @Test
    fun `debug unbounded tableau step by step`() {
        // Maximize: x1 + x2
        // Subject to: x1 + x2 >= 1
        //            x1, x2 >= 0
        // This should be unbounded

        val objective = LinearObjectiveFunction(doubleArrayOf(1.0, 1.0), 0.0)
        val constraints = listOf(
            LinearConstraint(doubleArrayOf(1.0, 1.0), Relationship.GEQ, 1.0)
        )

        val tableau = SimplexTableau(objective, constraints, GoalType.MAXIMIZE, 1e-6)

        println("=== UNBOUNDED TABLEAU DEBUGGING ===")
        println("Variables: numVars=${objective.dimension}, numSlackVars=?, numArtificialVars=?")
        println("Initial tableau:")
        tableau.printTableau()

        var iteration = 0
        while (!tableau.isOptimal() && iteration < 10) {
            iteration++
            println("=== ITERATION $iteration ===")

            val pivotCol = tableau.getPivotColumn(PivotSelectionRule.DANTZIG)
            println("Pivot column: $pivotCol")

            if (pivotCol < 0) {
                println("No pivot column found - optimal!")
                break
            }

            // Check unboundedness
            val isUnbounded = tableau.isUnbounded(pivotCol)
            println("Is unbounded: $isUnbounded")

            if (isUnbounded) {
                println("UNBOUNDED DETECTED!")
                break
            }

            val pivotRow = tableau.getPivotRow(pivotCol)
            println("Pivot row: $pivotRow")

            if (pivotRow < 0) {
                println("No valid pivot row - should be unbounded!")
                break
            }

            tableau.performPivot(pivotRow, pivotCol)
            println("After pivot:")
            tableau.printTableau()
        }

        println("=== PHASE TRANSITION ===")
        println("Current phase: ${tableau.getCurrentPhase()}")
        println("Is optimal: ${tableau.isOptimal()}")
        println("Is Phase I feasible: ${tableau.isPhase1Feasible()}")

        if (tableau.getCurrentPhase() == 1 && tableau.isPhase1Feasible()) {
            println("Transitioning to Phase II...")
            tableau.transitionToPhase2()
            println("After transition:")
            println("Current phase: ${tableau.getCurrentPhase()}")
            println("Is optimal: ${tableau.isOptimal()}")
            tableau.printTableau()

            // Additional debug: check objective coefficients
            println("OBJECTIVE ANALYSIS:")
            val pivotCol = tableau.getPivotColumn(PivotSelectionRule.DANTZIG)
            println("Pivot column from DANTZIG: $pivotCol")
            if (pivotCol >= 0) {
                println("Checking unboundedness for column $pivotCol: ${tableau.isUnbounded(pivotCol)}")
            }

            // Now try to continue Phase II
            println("=== PHASE II ITERATIONS ===")
            var phase2Iteration = 0
            while (!tableau.isOptimal() && phase2Iteration < 5) {
                phase2Iteration++
                println("Phase II Iteration $phase2Iteration")

                val pivotCol = tableau.getPivotColumn(PivotSelectionRule.DANTZIG)
                println("Pivot column: $pivotCol")

                if (pivotCol < 0) {
                    println("No pivot column - optimal!")
                    break
                }

                val isUnbounded = tableau.isUnbounded(pivotCol)
                println("Is unbounded: $isUnbounded")

                if (isUnbounded) {
                    println("UNBOUNDED DETECTED IN PHASE II!")
                    break
                }

                val pivotRow = tableau.getPivotRow(pivotCol)
                println("Pivot row: $pivotRow")

                if (pivotRow < 0) {
                    println("No valid pivot row - UNBOUNDED!")
                    break
                }

                tableau.performPivot(pivotRow, pivotCol)
                tableau.printTableau()
            }
        }

        val solution = tableau.getSolution()
        println("Final solution:")
        println("Point: ${solution.point.contentToString()}")
        println("Value: ${solution.value}")
    }
}