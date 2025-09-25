package io.github.dca.math.linear

/**
 * A linear constraint for a linear optimization problem.
 *
 * A linear constraint has a linear equation of the form:
 * c1*x1 + c2*x2 + ... + cn*xn {=,<=,>=} v
 *
 * @param coefficients the coefficients of the constraint (left hand side)
 * @param relationship the type of constraint (=, <=, or >=)
 * @param value the value of the constraint (right hand side)
 */
data class LinearConstraint(
    val coefficients: DoubleArray,
    val relationship: Relationship,
    val value: Double
) {
    init {
        require(coefficients.isNotEmpty()) { "Constraint coefficients cannot be empty" }
    }

    /**
     * Number of variables in the constraint.
     */
    val dimension: Int get() = coefficients.size

    /**
     * Evaluates the left-hand side of the constraint at a given point.
     *
     * @param point the point at which to evaluate the constraint
     * @return the value of the left-hand side of the constraint
     */
    fun evaluateLeftHandSide(point: DoubleArray): Double {
        require(point.size == coefficients.size) {
            "Point dimension (${point.size}) does not match constraint dimension (${coefficients.size})"
        }
        return coefficients.zip(point) { coeff, value -> coeff * value }.sum()
    }

    /**
     * Checks if a given point satisfies this constraint.
     *
     * @param point the point to check
     * @param epsilon tolerance for floating point comparisons
     * @return true if the constraint is satisfied, false otherwise
     */
    fun isSatisfiedBy(point: DoubleArray, epsilon: Double = 1e-6): Boolean {
        val leftValue = evaluateLeftHandSide(point)
        return when (relationship) {
            Relationship.EQ -> kotlin.math.abs(leftValue - value) <= epsilon
            Relationship.LEQ -> leftValue <= value + epsilon
            Relationship.GEQ -> leftValue >= value - epsilon
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LinearConstraint) return false
        return coefficients.contentEquals(other.coefficients) &&
                relationship == other.relationship &&
                value == other.value
    }

    override fun hashCode(): Int {
        var result = coefficients.contentHashCode()
        result = 31 * result + relationship.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }

    override fun toString(): String {
        val terms = mutableListOf<String>()

        coefficients.forEachIndexed { i, coeff ->
            when {
                coeff == 0.0 -> return@forEachIndexed
                coeff == 1.0 -> terms.add("x${i + 1}")
                coeff == -1.0 -> terms.add("-x${i + 1}")
                coeff > 0 && terms.isNotEmpty() -> terms.add(" + ${coeff}x${i + 1}")
                coeff < 0 && terms.isNotEmpty() -> terms.add(" - ${-coeff}x${i + 1}")
                else -> terms.add("${coeff}x${i + 1}")
            }
        }

        val leftSide = if (terms.isEmpty()) "0" else terms.joinToString("")
        return "$leftSide ${relationship.symbol} $value"
    }
}