package io.github.dca.math.linear

/**
 * This class holds a point and its corresponding value.
 * This is used to return the result of optimization problems.
 *
 * @param point the point coordinates
 * @param value the value of the objective function at the point
 */
data class PointValuePair(
    val point: DoubleArray,
    val value: Double
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PointValuePair) return false
        return point.contentEquals(other.point) && value == other.value
    }

    override fun hashCode(): Int {
        var result = point.contentHashCode()
        result = 31 * result + value.hashCode()
        return result
    }

    override fun toString(): String = "PointValuePair(point=${point.contentToString()}, value=$value)"
}
