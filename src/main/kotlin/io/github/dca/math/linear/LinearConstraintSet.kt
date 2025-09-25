package io.github.dca.math.linear

/**
 * A set of linear constraints for a linear optimization problem.
 * This class provides an immutable view of a collection of linear constraints.
 *
 * @param constraints the collection of linear constraints
 */
class LinearConstraintSet(constraints: Collection<LinearConstraint>) : Iterable<LinearConstraint> {
    private val constraintSet = LinkedHashSet(constraints)

    /**
     * Gets the constraints as an unmodifiable collection.
     */
    val constraints: Set<LinearConstraint> get() = constraintSet.toSet()

    /**
     * Checks if the constraint set is empty.
     */
    val isEmpty: Boolean get() = constraintSet.isEmpty()

    /**
     * Returns an iterator over the constraints.
     */
    override fun iterator(): Iterator<LinearConstraint> = constraintSet.iterator()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LinearConstraintSet) return false
        return constraintSet == other.constraintSet
    }

    override fun hashCode(): Int = constraintSet.hashCode()

    override fun toString(): String = constraintSet.joinToString("\n") { it.toString() }
}