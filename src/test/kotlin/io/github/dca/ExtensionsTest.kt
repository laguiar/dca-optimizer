package io.github.dca

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Tests for the extension functions used in the application.
 */
@DisplayName("Extension Functions Tests")
class ExtensionsTest {

    @Test
    @DisplayName("andThen should apply the transformation function to the receiver")
    fun testAndThen() {
        val result = "test".andThen { it.uppercase() }
        result shouldBe "TEST"

        val numberResult = 5.andThen { it * 2 }
        numberResult shouldBe 10

        val complexResult = listOf(1, 2, 3).andThen { list ->
            list.sumOf { it * 2 }
        }
        complexResult shouldBe 12
    }
    
    @Test
    @DisplayName("andThen should work with chained calls")
    fun testAndThenChained() {
        val result = "test"
            .andThen { it.uppercase() }
            .andThen { it.replace("T", "X") }
        
        result shouldBe "XESX"
    }
    
    @Test
    @DisplayName("andThen should work with nullable types")
    fun testAndThenWithNullable() {
        val nullableString: String? = "test"
        val result = nullableString?.andThen { it.uppercase() }
        
        result shouldBe "TEST"

        val nullString: String? = null
        val nullResult = nullString?.andThen { it.uppercase() }
        
        nullResult shouldBe null
    }
}
