package io.github.dca.math.linear

import org.junit.jupiter.api.Test

class SimplexDebugTest {

    @Test
    fun `debug simple maximization problem`() {
        // Maximize: 3x1 + 2x2
        // Subject to: x1 + x2 <= 4
        //            2x1 + x2 <= 6
        //            x1, x2 >= 0
        // Expected solution: x1 = 2, x2 = 2, value = 10

        val objective = LinearObjectiveFunction(doubleArrayOf(3.0, 2.0), 0.0)
        val constraints = listOf(
            LinearConstraint(doubleArrayOf(1.0, 1.0), Relationship.LEQ, 4.0),
            LinearConstraint(doubleArrayOf(2.0, 1.0), Relationship.LEQ, 6.0)
        )

        val tableau = SimplexTableau(objective, constraints, GoalType.MAXIMIZE, 1e-6)

        println("Initial tableau:")
        tableau.printTableau()

        var iteration = 0
        while (!tableau.isOptimal() && iteration < 10) {
            println("Iteration ${iteration + 1}:")

            val pivotColumn = tableau.getPivotColumn(PivotSelectionRule.DANTZIG)
            println("Pivot column: $pivotColumn")

            if (pivotColumn < 0) break

            val pivotRow = tableau.getPivotRow(pivotColumn)
            println("Pivot row: $pivotRow")

            if (pivotRow < 0) {
                println("Unbounded solution detected")
                break
            }

            tableau.performPivot(pivotRow, pivotColumn)
            tableau.printTableau()

            iteration++
        }

        println("Final solution:")
        val solution = tableau.getSolution()
        println("Point: ${solution.point.contentToString()}")
        println("Value: ${solution.value}")
    }
}