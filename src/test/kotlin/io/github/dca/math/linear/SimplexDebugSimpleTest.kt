package io.github.dca.math.linear

import org.junit.jupiter.api.Test

class SimplexDebugSimpleTest {

    @Test
    fun `debug maximization problem step by step`() {
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

        println("=== DEBUGGING SIMPLE MAXIMIZATION PROBLEM ===")
        println("Objective: Maximize 3x1 + 2x2")
        println("Constraints:")
        println("  x1 + x2 <= 4")
        println("  2x1 + x2 <= 6")
        println("  x1, x2 >= 0")
        println("Expected: x1 = 2, x2 = 2, value = 10")
        println()

        val solver = SimplexSolver()
        val result = solver.optimize(objective, LinearConstraintSet(constraints), GoalType.MAXIMIZE)

        println("ACTUAL RESULT:")
        println("Point: [${result.point[0]}, ${result.point[1]}]")
        println("Value: ${result.value}")
        println()

        // Manual verification
        val x1 = result.point[0]
        val x2 = result.point[1]
        val manualValue = 3.0 * x1 + 2.0 * x2
        println("Manual calculation: 3 * $x1 + 2 * $x2 = $manualValue")

        // Check constraints
        val constraint1 = x1 + x2
        val constraint2 = 2.0 * x1 + x2
        println("Constraint 1: x1 + x2 = $constraint1 (should be <= 4)")
        println("Constraint 2: 2x1 + x2 = $constraint2 (should be <= 6)")
    }
}