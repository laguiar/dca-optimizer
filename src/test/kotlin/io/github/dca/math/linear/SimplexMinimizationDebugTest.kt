package io.github.dca.math.linear

import org.junit.jupiter.api.Test

class SimplexMinimizationDebugTest {

    @Test
    fun `debug minimization problem step by step`() {
        // Minimize: 2x1 + 3x2
        // Subject to: x1 + 2x2 >= 3
        //            2x1 + x2 >= 3
        //            x1, x2 >= 0
        // Expected solution: x1 = 1, x2 = 1, value = 5

        val objective = LinearObjectiveFunction(doubleArrayOf(2.0, 3.0), 0.0)
        val constraints = listOf(
            LinearConstraint(doubleArrayOf(1.0, 2.0), Relationship.GEQ, 3.0),
            LinearConstraint(doubleArrayOf(2.0, 1.0), Relationship.GEQ, 3.0)
        )

        println("=== DEBUGGING MINIMIZATION PROBLEM ===")
        println("Objective: Minimize 2x1 + 3x2")
        println("Constraints:")
        println("  x1 + 2x2 >= 3")
        println("  2x1 + x2 >= 3")
        println("  x1, x2 >= 0")
        println("Expected: x1 = 1, x2 = 1, value = 5")
        println()

        val solver = SimplexSolver()

        try {
            val result = solver.optimize(objective, LinearConstraintSet(constraints), GoalType.MINIMIZE)

            println("ACTUAL RESULT:")
            println("Point: [${result.point[0]}, ${result.point[1]}]")
            println("Value: ${result.value}")

            // Debug tableau
            val tableau = SimplexTableau(objective, constraints, GoalType.MINIMIZE, 1e-6)
            val solver2 = SimplexSolver()
            solver2.optimize(objective, LinearConstraintSet(constraints), GoalType.MINIMIZE)
            println("Raw tableau RHS: ${tableau.getTableauRHS()}")
            println()

            // Manual verification
            val x1 = result.point[0]
            val x2 = result.point[1]
            val manualValue = 2.0 * x1 + 3.0 * x2
            println("Manual calculation: 2 * $x1 + 3 * $x2 = $manualValue")

            // Check constraints
            val constraint1 = x1 + 2.0 * x2
            val constraint2 = 2.0 * x1 + x2
            println("Constraint 1: x1 + 2x2 = $constraint1 (should be >= 3)")
            println("Constraint 2: 2x1 + x2 = $constraint2 (should be >= 3)")

        } catch (e: Exception) {
            println("ERROR: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
        }
    }

    @Test
    fun `debug constant term problem`() {
        // Minimize: x1 + x2 + 5
        // Subject to: x1 + x2 >= 2
        //            x1, x2 >= 0
        // Expected: x1 + x2 = 2, value = 2 + 5 = 7

        val objective = LinearObjectiveFunction(doubleArrayOf(1.0, 1.0), 5.0)
        val constraints = listOf(
            LinearConstraint(doubleArrayOf(1.0, 1.0), Relationship.GEQ, 2.0)
        )

        println("=== DEBUGGING CONSTANT TERM PROBLEM ===")
        println("Objective: Minimize x1 + x2 + 5")
        println("Constraints:")
        println("  x1 + x2 >= 2")
        println("  x1, x2 >= 0")
        println("Expected: x1 + x2 = 2, value = 7")
        println()

        val solver = SimplexSolver()

        try {
            val result = solver.optimize(objective, LinearConstraintSet(constraints), GoalType.MINIMIZE)

            println("ACTUAL RESULT:")
            println("Point: [${result.point[0]}, ${result.point[1]}]")
            println("Value: ${result.value}")

            // Debug tableau step by step
            val tableau = SimplexTableau(objective, constraints, GoalType.MINIMIZE, 1e-6)
            println("Initial tableau RHS: ${tableau.getTableauRHS()}")
            println("Initial tableau:")
            tableau.printTableau()

            // Let's run the solver to see the final tableau
            val solver3 = SimplexSolver()
            solver3.optimize(objective, LinearConstraintSet(constraints), GoalType.MINIMIZE)
            println("Final tableau RHS: ${tableau.getTableauRHS()}")
            println("Final tableau:")
            tableau.printTableau()

            // Manual verification
            val x1 = result.point[0]
            val x2 = result.point[1]
            val manualValue = 1.0 * x1 + 1.0 * x2 + 5.0
            println("Manual calculation: 1 * $x1 + 1 * $x2 + 5 = $manualValue")

            // Check constraints
            val constraint1 = x1 + x2
            println("Constraint: x1 + x2 = $constraint1 (should be >= 2)")

        } catch (e: Exception) {
            println("ERROR: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
        }
    }
}