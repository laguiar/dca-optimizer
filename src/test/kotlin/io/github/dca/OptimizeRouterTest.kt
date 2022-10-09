package io.github.dca

import io.quarkus.test.junit.QuarkusIntegrationTest
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.Matchers
import org.hamcrest.Matchers.hasKey
import org.junit.jupiter.api.Test

@QuarkusIntegrationTest
class OptimizeRouterTest {

    @Test
    fun testOptimizeRouter() {
        Given {
            contentType("application/json")
            body("""
                {
                    "amount": "1000.00",
                    "assets": [
                        {
                            "ticker": "BTC",
                            "weight": "50.0",
                            "target": "70.0"
                        }
                    ]
                }
            """.trimIndent()
            )
        } When {
            post("/api/optimize")
        } Then {
            statusCode(200)
            body("distribution", hasKey("BTC"))
        }
    }
}
