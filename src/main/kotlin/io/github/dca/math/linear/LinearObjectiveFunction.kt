package io.github.dca.math.linear

/**
 * An objective function for a linear optimization problem.
 *
 * A linear objective function has a linear equation of the form:
 * c1*x1 + c2*x2 + ... + cn*xn + d = 0
 *
 * @param coefficients the coefficients of the linear objective function to be maximized or minimized
 * @param constantTerm the constant term of the linear objective function
 */
@JvmInline
value class LinearObjectiveFunction(
    private val data: Pair<DoubleArray, Double>
) {
    constructor(coefficients: DoubleArray, constantTerm: Double = 0.0) : this(
        Pair(coefficients.copyOf(), constantTerm)
    )

    val coefficients: DoubleArray get() = data.first
    val constantTerm: Double get() = data.second

    /**
     * Computes the value of the linear objective function at a given point.
     *
     * @param point point at which the linear objective function must be evaluated
     * @return value of the linear objective function at the given point
     */
    fun value(point: DoubleArray): Double {
        require(point.size == coefficients.size) {
            "Point dimension (${point.size}) does not match coefficients dimension (${coefficients.size})"
        }
        return coefficients.zip(point) { coeff, value -> coeff * value }.sum() + constantTerm
    }

    /**
     * Number of variables in the objective function.
     */
    val dimension: Int get() = coefficients.size

    override fun toString(): String {
        val terms = mutableListOf<String>()

        coefficients.forEachIndexed { i, coeff ->
            when {
                coeff == 0.0 -> return@forEachIndexed
                coeff == 1.0 -> terms.add("x${i + 1}")
                coeff == -1.0 -> terms.add("-x${i + 1}")
                coeff > 0 -> terms.add("${coeff}x${i + 1}")
                else -> terms.add("${coeff}x${i + 1}")
            }
        }

        if (constantTerm != 0.0) {
            terms.add(constantTerm.toString())
        }

        return if (terms.isEmpty()) "0" else terms.joinToString(" + ").replace("+ -", "- ")
    }
}