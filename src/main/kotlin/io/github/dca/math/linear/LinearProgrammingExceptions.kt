package io.github.dca.math.linear

/**
 * Base exception for linear programming problems.
 */
sealed class LinearProgrammingException(message: String) : Exception(message)

/**
 * Exception thrown when no feasible solution exists.
 * This occurs when the constraints are contradictory.
 */
class NoFeasibleSolutionException(message: String = "No feasible solution exists") :
    LinearProgrammingException(message)

/**
 * Exception thrown when the solution is unbounded.
 * This occurs when the objective function can be improved indefinitely.
 */
class UnboundedSolutionException(message: String = "The solution is unbounded") :
    LinearProgrammingException(message)

/**
 * Exception thrown when the algorithm doesn't converge within the maximum number of iterations.
 */
class TooManyIterationsException(
    message: String = "Too many iterations without convergence",
    val maxIterations: Int
) : LinearProgrammingException("$message (max iterations: $maxIterations)")

/**
 * Exception thrown for invalid linear programming problems.
 */
class InvalidLinearProgramException(message: String) : LinearProgrammingException(message)