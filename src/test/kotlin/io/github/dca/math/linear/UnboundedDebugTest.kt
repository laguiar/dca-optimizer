package io.github.dca.math.linear

import org.junit.jupiter.api.Test

class UnboundedDebugTest {

    @Test
    fun `debug unbounded problem`() {
        // Maximize: x1 + x2 with no upper bounds
        // Subject to: x1 + x2 >= 1
        //            x1, x2 >= 0
        // This should be unbounded!

        val objective = LinearObjectiveFunction(doubleArrayOf(1.0, 1.0), 0.0)
        val constraints = listOf(
            LinearConstraint(doubleArrayOf(1.0, 1.0), Relationship.GEQ, 1.0)
        )

        println("=== DEBUGGING UNBOUNDED PROBLEM ===")
        println("Objective: Maximize x1 + x2")
        println("Constraints:")
        println("  x1 + x2 >= 1")
        println("  x1, x2 >= 0")
        println("Expected: Should throw UnboundedSolutionException")
        println()

        val solver = SimplexSolver()

        try {
            val result = solver.optimize(objective, LinearConstraintSet(constraints), GoalType.MAXIMIZE)
            println("ERROR: No exception thrown!")
            println("Result: Point [${result.point[0]}, ${result.point[1]}], Value: ${result.value}")
        } catch (e: UnboundedSolutionException) {
            println("SUCCESS: UnboundedSolutionException thrown as expected")
        } catch (e: Exception) {
            println("ERROR: Wrong exception type: ${e.javaClass.simpleName}: ${e.message}")
        }
    }
}