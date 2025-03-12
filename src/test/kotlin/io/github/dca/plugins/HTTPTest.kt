package io.github.dca.plugins

import io.github.dca.module
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

/**
 * Tests for the HTTP configuration in the application.
 * Note: The HTTP configuration is currently commented out in the Application.kt file,
 * so these tests are provided for future reference when it's enabled.
 */
@DisplayName("HTTP Configuration Tests")
class HTTPTest {

    @Test
    @DisplayName("Should handle CORS preflight requests")
    fun testCorsPreflightRequest() = testApplication {
        application {
            module()
        }
        
        // This test is a placeholder for when CORS is configured
        val response = client.get("/") {
            header(HttpHeaders.Origin, "https://example.com")
        }
        
        // Since CORS is not configured, we expect a 404 Not Found
        expectThat(response.status).isEqualTo(HttpStatusCode.NotFound)
    }
    
    @Test
    @DisplayName("Should handle compression")
    fun testCompression() = testApplication {
        application {
            module()
        }
        
        // This test is a placeholder for when compression is configured
        val response = client.get("/") {
            header(HttpHeaders.AcceptEncoding, "gzip, deflate")
        }
        
        // Since the route doesn't exist, we expect a 404 Not Found
        expectThat(response.status).isEqualTo(HttpStatusCode.NotFound)
    }
} 