package io.github.dca.math.linear

import org.junit.jupiter.api.Test

class SimplexTableauDebugTest {

    @Test
    fun `debug tableau step by step`() {
        // Maximize: 3x1 + 2x2
        // Subject to: x1 + x2 <= 4
        //            2x1 + x2 <= 6
        //            x1, x2 >= 0

        val objective = LinearObjectiveFunction(doubleArrayOf(3.0, 2.0), 0.0)
        val constraints = listOf(
            LinearConstraint(doubleArrayOf(1.0, 1.0), Relationship.LEQ, 4.0),
            LinearConstraint(doubleArrayOf(2.0, 1.0), Relationship.LEQ, 6.0)
        )

        val tableau = SimplexTableau(objective, constraints, GoalType.MAXIMIZE, 1e-6)

        println("=== TABLEAU DEBUGGING ===")
        println("Initial tableau:")
        tableau.printTableau()

        // Manual iteration
        var iteration = 0
        while (!tableau.isOptimal() && iteration < 5) {
            iteration++
            println("=== ITERATION $iteration ===")

            val pivotCol = tableau.getPivotColumn(PivotSelectionRule.DANTZIG)
            println("Pivot column: $pivotCol")

            if (pivotCol < 0) {
                println("No pivot column found - optimal!")
                break
            }

            val pivotRow = tableau.getPivotRow(pivotCol)
            println("Pivot row: $pivotRow")

            if (pivotRow < 0) {
                println("Unbounded solution detected")
                break
            }

            tableau.performPivot(pivotRow, pivotCol)
            println("After pivot:")
            tableau.printTableau()
        }

        println("=== FINAL SOLUTION ===")
        val solution = tableau.getSolution()
        println("Point: ${solution.point.contentToString()}")
        println("Value: ${solution.value}")

        // Debug: Print the raw tableau RHS value
        println("Raw tableau RHS: ${tableau.getTableauRHS()}")
    }
}