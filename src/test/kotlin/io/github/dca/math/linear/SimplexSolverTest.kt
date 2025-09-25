package io.github.dca.math.linear

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import kotlin.math.abs

class SimplexSolverTest {

    private val epsilon = 1e-6

    @Test
    fun `should solve simple maximization problem`() {
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

        val solver = SimplexSolver()
        val result = solver.optimize(objective, LinearConstraintSet(constraints), GoalType.MAXIMIZE)

        result.point.size shouldBe 2
        abs(result.point[0] - 2.0) shouldBeLessThan epsilon
        abs(result.point[1] - 2.0) shouldBeLessThan epsilon
        abs(result.value - 10.0) shouldBeLessThan epsilon
    }

    @Test
    fun `should solve simple minimization problem`() {
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

        val solver = SimplexSolver()
        val result = solver.optimize(objective, LinearConstraintSet(constraints), GoalType.MINIMIZE)

        result.point.size shouldBe 2
        abs(result.point[0] - 1.0) shouldBeLessThan epsilon
        abs(result.point[1] - 1.0) shouldBeLessThan epsilon
        abs(result.value - 5.0) shouldBeLessThan epsilon
    }

    @Test
    fun `should solve problem with equality constraints`() {
        // Minimize: x1 + 2x2
        // Subject to: x1 + x2 = 3
        //            x1, x2 >= 0
        // Expected solution: x1 = 3, x2 = 0, value = 3

        val objective = LinearObjectiveFunction(doubleArrayOf(1.0, 2.0), 0.0)
        val constraints = listOf(
            LinearConstraint(doubleArrayOf(1.0, 1.0), Relationship.EQ, 3.0)
        )

        val solver = SimplexSolver()
        val result = solver.optimize(objective, LinearConstraintSet(constraints), GoalType.MINIMIZE)

        result.point.size shouldBe 2
        abs(result.point[0] - 3.0) shouldBeLessThan epsilon
        abs(result.point[1] - 0.0) shouldBeLessThan epsilon
        abs(result.value - 3.0) shouldBeLessThan epsilon
    }

    @Test
    fun `should handle mixed constraint types`() {
        // Minimize: x1 + x2
        // Subject to: x1 + x2 >= 2
        //            x1 - x2 <= 1
        //            x1 + 2x2 = 3
        //            x1, x2 >= 0

        val objective = LinearObjectiveFunction(doubleArrayOf(1.0, 1.0), 0.0)
        val constraints = listOf(
            LinearConstraint(doubleArrayOf(1.0, 1.0), Relationship.GEQ, 2.0),
            LinearConstraint(doubleArrayOf(1.0, -1.0), Relationship.LEQ, 1.0),
            LinearConstraint(doubleArrayOf(1.0, 2.0), Relationship.EQ, 3.0)
        )

        val solver = SimplexSolver()
        val result = solver.optimize(objective, LinearConstraintSet(constraints), GoalType.MINIMIZE)

        result.point.size shouldBe 2
        // Verify the solution satisfies all constraints
        val point = result.point

        // Check constraint satisfaction
        abs(point[0] + point[1] - 2.0) shouldBeGreaterThan -epsilon
        (point[0] - point[1]) shouldBeLessThanOrEqual 1.0 + epsilon
        abs(point[0] + 2.0 * point[1] - 3.0) shouldBeLessThan epsilon

        point[0] shouldBeGreaterThan -epsilon
        point[1] shouldBeGreaterThan -epsilon
    }

    @Test
    fun `should throw exception for infeasible problem`() {
        // Impossible constraints: x1 + x2 <= 1 and x1 + x2 >= 2
        val objective = LinearObjectiveFunction(doubleArrayOf(1.0, 1.0), 0.0)
        val constraints = listOf(
            LinearConstraint(doubleArrayOf(1.0, 1.0), Relationship.LEQ, 1.0),
            LinearConstraint(doubleArrayOf(1.0, 1.0), Relationship.GEQ, 2.0)
        )

        val solver = SimplexSolver()
        shouldThrow<NoFeasibleSolutionException> {
            solver.optimize(objective, LinearConstraintSet(constraints), GoalType.MINIMIZE)
        }
    }

    @Test
    fun `should throw exception for unbounded problem`() {
        // Maximize: x1 + x2 with no upper bounds
        // Subject to: x1 + x2 >= 1
        //            x1, x2 >= 0
        val objective = LinearObjectiveFunction(doubleArrayOf(1.0, 1.0), 0.0)
        val constraints = listOf(
            LinearConstraint(doubleArrayOf(1.0, 1.0), Relationship.GEQ, 1.0)
        )

        val solver = SimplexSolver()
        shouldThrow<UnboundedSolutionException> {
            solver.optimize(objective, LinearConstraintSet(constraints), GoalType.MAXIMIZE)
        }
    }

    @Test
    fun `should handle single variable problem`() {
        // Minimize: 2x
        // Subject to: x >= 3
        val objective = LinearObjectiveFunction(doubleArrayOf(2.0), 0.0)
        val constraints = listOf(
            LinearConstraint(doubleArrayOf(1.0), Relationship.GEQ, 3.0)
        )

        val solver = SimplexSolver()
        val result = solver.optimize(objective, LinearConstraintSet(constraints), GoalType.MINIMIZE)

        result.point.size shouldBe 1
        abs(result.point[0] - 3.0) shouldBeLessThan epsilon
        abs(result.value - 6.0) shouldBeLessThan epsilon
    }

    @Test
    fun `should handle problem with constant term in objective`() {
        // Minimize: x1 + x2 + 5
        // Subject to: x1 + x2 >= 2
        //            x1, x2 >= 0
        val objective = LinearObjectiveFunction(doubleArrayOf(1.0, 1.0), 5.0)
        val constraints = listOf(
            LinearConstraint(doubleArrayOf(1.0, 1.0), Relationship.GEQ, 2.0)
        )

        val solver = SimplexSolver()
        val result = solver.optimize(objective, LinearConstraintSet(constraints), GoalType.MINIMIZE)

        result.point.size shouldBe 2
        abs(result.point[0] + result.point[1] - 2.0) shouldBeLessThan epsilon
        abs(result.value - 7.0) shouldBeLessThan epsilon // 2 + 5
    }

    @Test
    fun `should validate problem dimensions`() {
        val objective = LinearObjectiveFunction(doubleArrayOf(1.0, 2.0), 0.0)
        val constraints = listOf(
            LinearConstraint(doubleArrayOf(1.0, 2.0, 3.0), Relationship.LEQ, 4.0) // Wrong dimension
        )

        val solver = SimplexSolver()
        shouldThrow<InvalidLinearProgramException> {
            solver.optimize(objective, LinearConstraintSet(constraints), GoalType.MINIMIZE)
        }
    }

    @Test
    fun `should validate empty constraints`() {
        val objective = LinearObjectiveFunction(doubleArrayOf(1.0, 2.0), 0.0)
        val constraints = emptyList<LinearConstraint>()

        val solver = SimplexSolver()
        shouldThrow<InvalidLinearProgramException> {
            solver.optimize(objective, LinearConstraintSet(constraints), GoalType.MINIMIZE)
        }
    }

    @Test
    fun `should use different pivot selection rules`() {
        val objective = LinearObjectiveFunction(doubleArrayOf(3.0, 2.0), 0.0)
        val constraints = listOf(
            LinearConstraint(doubleArrayOf(1.0, 1.0), Relationship.LEQ, 4.0),
            LinearConstraint(doubleArrayOf(2.0, 1.0), Relationship.LEQ, 6.0)
        )

        val solverDantzig = SimplexSolver(pivotSelectionRule = PivotSelectionRule.DANTZIG)
        val solverBland = SimplexSolver(pivotSelectionRule = PivotSelectionRule.BLAND)

        val resultDantzig = solverDantzig.optimize(objective, LinearConstraintSet(constraints), GoalType.MAXIMIZE)
        val resultBland = solverBland.optimize(objective, LinearConstraintSet(constraints), GoalType.MAXIMIZE)

        // Both should find the same optimal solution
        abs(resultDantzig.value - resultBland.value) shouldBeLessThan epsilon
        abs(resultDantzig.point[0] - resultBland.point[0]) shouldBeLessThan epsilon
        abs(resultDantzig.point[1] - resultBland.point[1]) shouldBeLessThan epsilon
    }
}