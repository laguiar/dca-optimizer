package io.github.dca

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.hasEntry
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo

class OptimizeRouterTest {

    @Test
    fun testOptimizeRouter() = testApplication {
        val client = createClient {
            install(ContentNegotiation) {
                json()
            }
        }

        val response = client.post("/api/optimize") {
            contentType(Json)
            this.setBody("""
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
        }

        expectThat(response.status).isEqualTo(HttpStatusCode.OK)
        expectThat(response.contentType()).isEqualTo(Json.withParameter("charset", "UTF-8"))
        expectThat(response.body<DcaResponse>().distribution).hasSize(1)
        expectThat(response.body<DcaResponse>().distribution).hasEntry("BTC", BigDecimalNumber("1000.00"))
    }
}
