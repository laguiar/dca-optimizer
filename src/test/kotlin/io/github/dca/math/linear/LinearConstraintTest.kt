package io.github.dca.math.linear

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class LinearConstraintTest {

    @Test
    fun `should create constraint with correct properties`() {
        val coefficients = doubleArrayOf(1.0, 2.0, 3.0)
        val constraint = LinearConstraint(coefficients, Relationship.LEQ, 5.0)

        constraint.coefficients shouldBe coefficients
        constraint.relationship shouldBe Relationship.LEQ
        constraint.value shouldBe 5.0
        constraint.dimension shouldBe 3
    }

    @Test
    fun `should evaluate left hand side correctly`() {
        val constraint = LinearConstraint(doubleArrayOf(2.0, 3.0), Relationship.LEQ, 10.0)
        val point = doubleArrayOf(1.0, 2.0)

        val result = constraint.evaluateLeftHandSide(point)
        result shouldBe 8.0 // 2*1 + 3*2 = 8
    }

    @Test
    fun `should check constraint satisfaction for LEQ`() {
        val constraint = LinearConstraint(doubleArrayOf(1.0, 1.0), Relationship.LEQ, 5.0)

        constraint.isSatisfiedBy(doubleArrayOf(2.0, 2.0)) shouldBe true  // 4 <= 5
        constraint.isSatisfiedBy(doubleArrayOf(2.0, 3.0)) shouldBe true  // 5 <= 5
        constraint.isSatisfiedBy(doubleArrayOf(3.0, 3.0)) shouldBe false // 6 > 5
    }

    @Test
    fun `should check constraint satisfaction for GEQ`() {
        val constraint = LinearConstraint(doubleArrayOf(1.0, 1.0), Relationship.GEQ, 5.0)

        constraint.isSatisfiedBy(doubleArrayOf(3.0, 3.0)) shouldBe true  // 6 >= 5
        constraint.isSatisfiedBy(doubleArrayOf(2.0, 3.0)) shouldBe true  // 5 >= 5
        constraint.isSatisfiedBy(doubleArrayOf(1.0, 2.0)) shouldBe false // 3 < 5
    }

    @Test
    fun `should check constraint satisfaction for EQ`() {
        val constraint = LinearConstraint(doubleArrayOf(1.0, 1.0), Relationship.EQ, 5.0)

        constraint.isSatisfiedBy(doubleArrayOf(2.0, 3.0)) shouldBe true  // 5 == 5
        constraint.isSatisfiedBy(doubleArrayOf(2.0, 2.0)) shouldBe false // 4 != 5
        constraint.isSatisfiedBy(doubleArrayOf(3.0, 3.0)) shouldBe false // 6 != 5
    }

    @Test
    fun `should handle equality comparison correctly`() {
        val constraint1 = LinearConstraint(doubleArrayOf(1.0, 2.0), Relationship.LEQ, 5.0)
        val constraint2 = LinearConstraint(doubleArrayOf(1.0, 2.0), Relationship.LEQ, 5.0)
        val constraint3 = LinearConstraint(doubleArrayOf(1.0, 3.0), Relationship.LEQ, 5.0)

        (constraint1 == constraint2) shouldBe true
        (constraint1 == constraint3) shouldBe false
    }

    @Test
    fun `should generate meaningful string representation`() {
        val constraint = LinearConstraint(doubleArrayOf(2.0, -1.0, 3.0), Relationship.LEQ, 10.0)
        val str = constraint.toString()

        str.contains("x1") shouldBe true
        str.contains("x2") shouldBe true
        str.contains("x3") shouldBe true
        str.contains("<=") shouldBe true
        str.contains("10.0") shouldBe true
    }
}