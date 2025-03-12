package io.github.dca

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

/**
 * Tests for the extension functions used in the application.
 */
@DisplayName("Extension Functions Tests")
class ExtensionsTest {

    @Test
    @DisplayName("andThen should apply the transformation function to the receiver")
    fun testAndThen() {
        val result = "test".andThen { it.uppercase() }
        expectThat(result).isEqualTo("TEST")
        
        val numberResult = 5.andThen { it * 2 }
        expectThat(numberResult).isEqualTo(10)
        
        val complexResult = listOf(1, 2, 3).andThen { list ->
            list.map { it * 2 }.sum()
        }
        expectThat(complexResult).isEqualTo(12)
    }
    
    @Test
    @DisplayName("andThen should work with chained calls")
    fun testAndThenChained() {
        val result = "test"
            .andThen { it.uppercase() }
            .andThen { it.replace("T", "X") }
        
        expectThat(result).isEqualTo("XESX")
    }
    
    @Test
    @DisplayName("andThen should work with nullable types")
    fun testAndThenWithNullable() {
        val nullableString: String? = "test"
        val result = nullableString?.andThen { it.uppercase() }
        
        expectThat(result).isEqualTo("TEST")
        
        val nullString: String? = null
        val nullResult = nullString?.andThen { it.uppercase() }
        
        expectThat(nullResult).isEqualTo(null)
    }
} 