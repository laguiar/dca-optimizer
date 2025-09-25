package io.github.dca.math.linear

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class LinearObjectiveFunctionTest {

    @Test
    fun `should create objective function with correct properties`() {
        val coefficients = doubleArrayOf(1.0, 2.0, 3.0)
        val constantTerm = 5.0
        val objective = LinearObjectiveFunction(coefficients, constantTerm)

        objective.coefficients shouldBe coefficients
        objective.constantTerm shouldBe constantTerm
        objective.dimension shouldBe 3
    }

    @Test
    fun `should create objective function with default constant term`() {
        val coefficients = doubleArrayOf(1.0, 2.0)
        val objective = LinearObjectiveFunction(coefficients)

        objective.coefficients shouldBe coefficients
        objective.constantTerm shouldBe 0.0
        objective.dimension shouldBe 2
    }

    @Test
    fun `should evaluate objective function correctly`() {
        val objective = LinearObjectiveFunction(doubleArrayOf(2.0, 3.0), 1.0)
        val point = doubleArrayOf(1.0, 2.0)

        val result = objective.value(point)
        result shouldBe 9.0 // 2*1 + 3*2 + 1 = 9
    }

    @Test
    fun `should evaluate objective function with zero constant term`() {
        val objective = LinearObjectiveFunction(doubleArrayOf(2.0, 3.0))
        val point = doubleArrayOf(1.0, 2.0)

        val result = objective.value(point)
        result shouldBe 8.0 // 2*1 + 3*2 = 8
    }

    @Test
    fun `should throw exception for mismatched dimensions`() {
        val objective = LinearObjectiveFunction(doubleArrayOf(1.0, 2.0))
        val point = doubleArrayOf(1.0, 2.0, 3.0) // Wrong dimension

        shouldThrow<IllegalArgumentException> {
            objective.value(point)
        }
    }

    @Test
    fun `should handle single variable objective`() {
        val objective = LinearObjectiveFunction(doubleArrayOf(5.0), 2.0)
        val point = doubleArrayOf(3.0)

        val result = objective.value(point)
        result shouldBe 17.0 // 5*3 + 2 = 17
    }

    @Test
    fun `should generate meaningful string representation`() {
        val objective = LinearObjectiveFunction(doubleArrayOf(1.0, -2.0, 0.0, 3.0), 5.0)
        val str = objective.toString()

        str.contains("x1") shouldBe true
        str.contains("x4") shouldBe true
        str.contains("5.0") shouldBe true
        // Should not contain x3 since its coefficient is 0
        !str.contains("0.0x3") shouldBe true
    }

    @Test
    fun `should handle zero coefficients in string representation`() {
        val objective = LinearObjectiveFunction(doubleArrayOf(0.0, 1.0, 0.0))
        val str = objective.toString()

        str shouldBe "x2"
    }

    @Test
    fun `should handle all zero coefficients`() {
        val objective = LinearObjectiveFunction(doubleArrayOf(0.0, 0.0), 3.0)
        val str = objective.toString()

        str shouldBe "3.0"
    }

    @Test
    fun `should handle all zero coefficients and zero constant`() {
        val objective = LinearObjectiveFunction(doubleArrayOf(0.0, 0.0))
        val str = objective.toString()

        str shouldBe "0"
    }
}