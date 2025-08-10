package io.github.dca.plugins

import io.github.dca.WithdrawalCalculationRequest
import io.github.dca.WithdrawalCalculationResponse
import io.github.dca.module
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Tests for the serialization configuration in the application.
 * These tests verify that the application correctly handles JSON serialization and deserialization.
 */
@DisplayName("Serialization Configuration Tests")
class SerializationTest {

    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
        allowSpecialFloatingPointValues = true
    }

    @Test
    @DisplayName("Should handle JSON serialization with special floating point values")
    fun testJsonSerializationWithSpecialValues() = testApplication {
        application {
            module()
        }
        
        // Test with a withdrawal calculation that returns Infinity
        val request = WithdrawalCalculationRequest(
            totalAmount = BigDecimal("100000"),
            monthlyWithdraw = BigDecimal.ZERO,
            expectedYearlyReturn = 7.0
        )
        
        val response = client.post("/api/simulate-withdrawal") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
        }
        
        response.status shouldBe HttpStatusCode.OK
        
        val responseBody = json.decodeFromString<WithdrawalCalculationResponse>(response.bodyAsText())
        
        // Verify that the response contains Infinity
        responseBody.isInfinite shouldBe true
    }
    
    @Test
    @DisplayName("Should handle JSON serialization with unknown properties")
    fun testJsonSerializationWithUnknownProperties() = testApplication {
        application {
            module()
        }
        
        // Create a JSON string with an unknown property
        val jsonWithUnknownProperty = """
            {
                "amount": 1000,
                "assets": [
                    {
                        "ticker": "BTC",
                        "target": 100.0,
                        "unknownProperty": "value"
                    }
                ],
                "strategy": {
                    "type": "TARGET"
                }
            }
        """.trimIndent()
        
        // The server should ignore the unknown property
        val response = client.post("/api/optimize") {
            contentType(ContentType.Application.Json)
            setBody(jsonWithUnknownProperty)
        }
        
        response.status shouldBe HttpStatusCode.OK
    }
    
    @Test
    @DisplayName("Should handle lenient JSON parsing")
    fun testLenientJsonParsing() = testApplication {
        application {
            module()
        }
        
        // Create a JSON string with unquoted keys (which is not valid JSON but allowed in lenient mode)
        val lenientJson = """
            {
                amount: 1000,
                assets: [
                    {
                        ticker: "BTC",
                        target: 100.0
                    }
                ],
                strategy: {
                    type: "TARGET"
                }
            }
        """.trimIndent()
        
        // The server should parse the lenient JSON
        val response = client.post("/api/optimize") {
            contentType(ContentType.Application.Json)
            setBody(lenientJson)
        }
        
        response.status shouldBe HttpStatusCode.OK
    }
} 