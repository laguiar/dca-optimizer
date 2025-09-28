package io.github.dca.plugins

import io.github.dca.*
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.maps.beEmpty
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.shouldNotBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Tests for the routing configuration in the application.
 * These tests verify that all API endpoints correctly handle requests and produce expected responses.
 */
@DisplayName("Routing Configuration Tests")
class RoutingTest {

    private val json = Json { 
        ignoreUnknownKeys = true 
        allowSpecialFloatingPointValues = true
    }

    @Nested
    @DisplayName("Optimize Endpoint Tests")
    inner class OptimizeEndpointTests {
        
        @Test
        @DisplayName("Should return 400 for invalid optimize payload (missing required fields)")
        fun testOptimizeWithInvalidPayloadMissingFields() = testApplication {
            application { module() }

            val response = client.post("/api/optimize") {
                contentType(ContentType.Application.Json)
                setBody("""{ }""")
            }

            response.status shouldBe HttpStatusCode.BadRequest
            response.bodyAsText().isNotBlank() shouldBe true
        }
        
        @Test
        @DisplayName("Should handle optimize request with TARGET strategy")
        fun testOptimizeWithTargetStrategy() = testApplication {
            application {
                module()
            }
            
            val request = DcaRequest(
                amount = BigDecimal("1000"),
                assets = listOf(
                    Asset(
                        ticker = "BTC",
                        target = 100.0
                    )
                ),
                strategy = DcaStrategy(type = StrategyType.TARGET)
            )
            
            val response = client.post("/api/optimize") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }
            
            response.status shouldBe HttpStatusCode.OK
            
            val responseBody = json.decodeFromString<DcaResponse>(response.bodyAsText())
            
            responseBody.distribution shouldNot beEmpty()
            responseBody.distribution shouldContainKey("BTC")
        }
        
        @Test
        @DisplayName("Should handle optimize request with WEIGHT strategy")
        fun testOptimizeWithWeightStrategy() = testApplication {
            application {
                module()
            }
            
            val request = DcaRequest(
                amount = BigDecimal("1000"),
                portfolioValue = BigDecimal("10000"),
                assets = listOf(
                    Asset(
                        ticker = "BTC",
                        weight = 50.0
                    ),
                    Asset(
                        ticker = "ETH",
                        weight = 50.0
                    )
                ),
                strategy = DcaStrategy(type = StrategyType.WEIGHT)
            )
            
            val response = client.post("/api/optimize") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }
            
            response.status shouldBe HttpStatusCode.OK
            
            val responseBody = json.decodeFromString<DcaResponse>(response.bodyAsText())

            responseBody.distribution shouldNot beEmpty()
            responseBody.distribution shouldContainKey("BTC")
            responseBody.distribution shouldContainKey("ETH")
        }
        
        @Test
        @DisplayName("Should handle optimize request with PORTFOLIO strategy")
        fun testOptimizeWithPortfolioStrategy() = testApplication {
            application {
                module()
            }
            
            val request = DcaRequest(
                amount = BigDecimal("1000"),
                portfolioValue = BigDecimal("10000"),
                assets = listOf(
                    Asset(
                        ticker = "BTC",
                        weight = 40.0,
                        target = 50.0
                    ),
                    Asset(
                        ticker = "ETH",
                        weight = 60.0,
                        target = 50.0
                    )
                ),
                strategy = DcaStrategy(type = StrategyType.PORTFOLIO)
            )
            
            val response = client.post("/api/optimize") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }
            
            response.status shouldBe HttpStatusCode.OK
            
            val responseBody = json.decodeFromString<DcaResponse>(response.bodyAsText())

            responseBody.distribution shouldNot beEmpty()
            responseBody.distribution shouldContainKey("BTC")
            responseBody.distribution shouldContainKey("ETH")
        }
        
        @Test
        @DisplayName("Should handle optimize request with RATING strategy")
        fun testOptimizeWithRatingStrategy() = testApplication {
            application {
                module()
            }
            
            val request = DcaRequest(
                amount = BigDecimal("1000"),
                assets = listOf(
                    Asset(
                        ticker = "BTC",
                        rating = 5
                    ),
                    Asset(
                        ticker = "ETH",
                        rating = 3
                    )
                ),
                strategy = DcaStrategy(type = StrategyType.RATING)
            )
            
            val response = client.post("/api/optimize") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }
            
            response.status shouldBe HttpStatusCode.OK
            
            val responseBody = json.decodeFromString<DcaResponse>(response.bodyAsText())

            responseBody.distribution shouldNot beEmpty()
            responseBody.distribution shouldContainKey("BTC")
            responseBody.distribution shouldContainKey("ETH")
        }
    }
    
    @Nested
    @DisplayName("Simulate Withdrawal Endpoint Tests")
    inner class SimulateWithdrawalEndpointTests {
        
        @Test
        @DisplayName("Should return 400 for invalid simulate withdrawal payload (missing required fields)")
        fun testSimulateWithdrawalWithInvalidPayloadMissingFields() = testApplication {
            application { module() }

            val response = client.post("/api/simulate-withdrawal") {
                contentType(ContentType.Application.Json)
                // Missing totalAmount
                setBody("""{
                  "monthlyWithdraw": "500",
                  "expectedYearlyReturn": 7.0
                }""")
            }

            response.status shouldBe HttpStatusCode.BadRequest
            response.bodyAsText().isNotBlank() shouldBe true
        }
        
        @Test
        @DisplayName("Should handle simulate withdrawal request")
        fun testSimulateWithdrawalEndpoint() = testApplication {
            application {
                module()
            }
            
            val request = WithdrawalCalculationRequest(
                totalAmount = BigDecimal("100000"),
                monthlyWithdraw = BigDecimal("500"),
                expectedYearlyReturn = 7.0
            )
            
            val response = client.post("/api/simulate-withdrawal") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }
            
            response.status shouldBe HttpStatusCode.OK
            
            val responseBody = json.decodeFromString<WithdrawalCalculationResponse>(response.bodyAsText())
            
            // Verify we get a reasonable response
            responseBody.years shouldBeGreaterThan 0.0
        }
        
        @Test
        @DisplayName("Should handle simulate withdrawal request with zero total amount")
        fun testSimulateWithdrawalWithZeroAmount() = testApplication {
            application {
                module()
            }
            
            val request = WithdrawalCalculationRequest(
                totalAmount = BigDecimal.ZERO,
                monthlyWithdraw = BigDecimal("500"),
                expectedYearlyReturn = 7.0
            )
            
            val response = client.post("/api/simulate-withdrawal") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }
            
            response.status shouldBe HttpStatusCode.OK
            
            val responseBody = json.decodeFromString<WithdrawalCalculationResponse>(response.bodyAsText())
            
            responseBody.years shouldBe 0.0
            responseBody.isInfinite shouldBe false
        }
        
        @Test
        @DisplayName("Should handle simulate withdrawal request with zero monthly withdraw")
        fun testSimulateWithdrawalWithZeroMonthlyWithdraw() = testApplication {
            application {
                module()
            }
            
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
            
            responseBody.isInfinite shouldBe true
        }
    }
    
    @Nested
    @DisplayName("Calculate Withdrawal Endpoint Tests")
    inner class CalculateWithdrawalEndpointTests {
        
        @Test
        @DisplayName("Should return 400 for invalid calculate withdrawal payload (missing required fields)")
        fun testCalculateWithdrawalWithInvalidPayloadMissingFields() = testApplication {
            application { module() }

            val response = client.post("/api/calculate-withdrawal") {
                contentType(ContentType.Application.Json)
                // Missing monthlyWithdraw
                setBody("""{
                  "totalAmount": "100000",
                  "expectedYearlyReturn": 7.0
                }""")
            }

            response.status shouldBe HttpStatusCode.BadRequest
            response.bodyAsText().isNotBlank() shouldBe true
        }
        
        @Test
        @DisplayName("Should handle calculate withdrawal request")
        fun testCalculateWithdrawalEndpoint() = testApplication {
            application {
                module()
            }
            
            val request = WithdrawalCalculationRequest(
                totalAmount = BigDecimal("100000"),
                monthlyWithdraw = BigDecimal("500"),
                expectedYearlyReturn = 7.0
            )
            
            val response = client.post("/api/calculate-withdrawal") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }
            
            response.status shouldBe HttpStatusCode.OK
            
            val responseBody = json.decodeFromString<WithdrawalCalculationResponse>(response.bodyAsText())
            
            // Verify we get a reasonable response
            responseBody.years shouldBeGreaterThan 0.0
        }
        
        @Test
        @DisplayName("Should handle calculate withdrawal request with high return rate")
        fun testCalculateWithdrawalWithHighReturn() = testApplication {
            application {
                module()
            }
            
            val request = WithdrawalCalculationRequest(
                totalAmount = BigDecimal("100000"),
                monthlyWithdraw = BigDecimal("500"),
                expectedYearlyReturn = 15.0
            )
            
            val response = client.post("/api/calculate-withdrawal") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }
            
            response.status shouldBe HttpStatusCode.OK
            
            val responseBody = json.decodeFromString<WithdrawalCalculationResponse>(response.bodyAsText())
            
            responseBody.isInfinite shouldBe true
        }
    }
    
    @Nested
    @DisplayName("Calculate Target Amount Endpoint Tests")
    inner class CalculateTargetAmountEndpointTests {
        
        @Test
        @DisplayName("Should return 400 for invalid calculate target amount payload (missing required fields)")
        fun testCalculateTargetAmountWithInvalidPayloadMissingFields() = testApplication {
            application { module() }

            val response = client.post("/api/calculate-target-amount") {
                contentType(ContentType.Application.Json)
                // Missing monthlyWithdraw
                setBody("""{
                  "shouldLastForYears": 30.0,
                  "expectedYearlyReturn": 7.0
                }""")
            }

            response.status shouldBe HttpStatusCode.BadRequest
            response.bodyAsText().isNotBlank() shouldBe true
        }
        
        @Test
        @DisplayName("Should handle calculate target amount request")
        fun testCalculateTargetAmountEndpoint() = testApplication {
            application {
                module()
            }
            
            val request = InitialAmountCalculationRequest(
                shouldLastForYears = 30.0,
                monthlyWithdraw = BigDecimal("1000"),
                expectedYearlyReturn = 7.0
            )
            
            val response = client.post("/api/calculate-target-amount") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }
            
            response.status shouldBe HttpStatusCode.OK
            
            val responseBody = json.decodeFromString<InitialAmountCalculationResponse>(response.bodyAsText())
            
            // Verify we get a reasonable response
            responseBody.totalAmount.compareTo(BigDecimal.ZERO) shouldBeGreaterThan 0
        }
        
        @Test
        @DisplayName("Should handle calculate target amount request with zero years")
        fun testCalculateTargetAmountWithZeroYears() = testApplication {
            application {
                module()
            }
            
            val request = InitialAmountCalculationRequest(
                shouldLastForYears = 0.0,
                monthlyWithdraw = BigDecimal("1000"),
                expectedYearlyReturn = 7.0
            )
            
            val response = client.post("/api/calculate-target-amount") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }
            
            response.status shouldBe HttpStatusCode.OK
            
            val responseBody = json.decodeFromString<InitialAmountCalculationResponse>(response.bodyAsText())
            
            responseBody.totalAmount shouldBe BigDecimal.ZERO
        }
    }

    @Nested
    @DisplayName("Calculate Advanced Withdrawal Endpoint Tests")
    inner class CalculateAdvancedWithdrawalEndpointTests {
        
        @Test
        @DisplayName("Should return 400 for invalid advanced withdrawal payload (missing required fields)")
        fun testCalculateAdvancedWithdrawalWithInvalidPayloadMissingFields() = testApplication {
            application { module() }

            val response = client.post("/api/calculate-advanced-withdrawal") {
                contentType(ContentType.Application.Json)
                // Missing monthlyWithdraw
                setBody("""{
                  "totalAmount": "100000",
                  "expectedYearlyReturn": 7.0
                }""")
            }

            response.status shouldBe HttpStatusCode.BadRequest
            response.bodyAsText().isNotBlank() shouldBe true
        }
        
        @Test
        @DisplayName("Should handle advanced withdrawal calculation request")
        fun testCalculateAdvancedWithdrawalEndpoint() = testApplication {
            application {
                module()
            }
            
            val request = AdvancedWithdrawalCalculationRequest(
                totalAmount = BigDecimal("100000"),
                monthlyWithdraw = BigDecimal("500"),
                expectedYearlyReturn = 7.0,
                yearlyInflationRate = 2.0,
                yearlyTaxAllowance = BigDecimal("12000"),
                averageTaxRate = 20.0
            )
            
            val response = client.post("/api/calculate-advanced-withdrawal") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }
            
            response.status shouldBe HttpStatusCode.OK
            
            val responseBody = json.decodeFromString<AdvancedWithdrawalCalculationResponse>(response.bodyAsText())
            
            // Verify we get a reasonable response
            responseBody.years shouldBeGreaterThan 0.0
            responseBody.realReturn shouldBe 5.0 // 7% return - 2% inflation
            responseBody.yearlyBreakdown shouldNotBe null
            responseBody.yearlyBreakdown!! shouldNot io.kotest.matchers.collections.beEmpty()
        }
        
        @Test
        @DisplayName("Should handle advanced withdrawal calculation with high inflation")
        fun testCalculateAdvancedWithdrawalWithHighInflation() = testApplication {
            application {
                module()
            }
            
            val request = AdvancedWithdrawalCalculationRequest(
                totalAmount = BigDecimal("100000"),
                monthlyWithdraw = BigDecimal("500"),
                expectedYearlyReturn = 7.0,
                yearlyInflationRate = 5.0,
                yearlyTaxAllowance = BigDecimal("12000"),
                averageTaxRate = 20.0
            )
            
            val response = client.post("/api/calculate-advanced-withdrawal") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }
            
            response.status shouldBe HttpStatusCode.OK
            
            val responseBody = json.decodeFromString<AdvancedWithdrawalCalculationResponse>(response.bodyAsText())
            
            // With high inflation, real return is lower
            responseBody.realReturn shouldBe 2.0 // 7% return - 5% inflation
            // Final withdrawal amount should be higher due to inflation
            responseBody.inflationAdjustedWithdrawal.compareTo(request.monthlyWithdraw) shouldBeGreaterThan 0
        }
    }
} 
