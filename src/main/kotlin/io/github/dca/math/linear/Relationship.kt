package io.github.dca.math.linear

/**
 * Types of relationships between the left and right hand sides of linear constraints.
 */
enum class Relationship(val symbol: String) {
    /** Equal to relationship: = */
    EQ("="),
    /** Less than or equal to relationship: <= */
    LEQ("<="),
    /** Greater than or equal to relationship: >= */
    GEQ(">=");

    /**
     * Get the opposite relationship.
     */
    fun opposite(): Relationship = when (this) {
        EQ -> EQ
        LEQ -> GEQ
        GEQ -> LEQ
    }

    override fun toString(): String = symbol
}