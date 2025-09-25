package io.github.dca.math.linear

/**
 * Types of relationships between the left and right hand sides of linear constraints.
 */
enum class Relationship(val symbol: String) {
    EQ("="),
    LEQ("<="),
    GEQ(">=");

    override fun toString(): String = symbol
}