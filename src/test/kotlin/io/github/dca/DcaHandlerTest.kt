package io.github.dca

import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.containsKey
import strikt.assertions.containsKeys
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.withValue
import java.math.BigDecimal
import java.math.MathContext

internal class DcaHandlerTest {

    private val handler = DcaHandler()

    // ATH max value: 0.10
    private val assets = listOf(
        Asset(ticker = "A", weight = 25.0, target = 20.0, belowAth = 20.0), // over-target
        Asset(ticker = "B", weight = 15.0, target = 20.0, belowAth = 8.0), // under-target but too close to ATH
        Asset(ticker = "C", weight = 15.0, target = 25.0, belowAth = 15.0), // under-target
        Asset(ticker = "D", weight = 10.0, target = 25.0, belowAth = 15.0), // under-target
        Asset(ticker = "E", weight = 5.0, target = 10.0, belowAth = 22.0)   // under-target
    )

    private val amountToInvest = BigDecimal("1000", MathContext.DECIMAL32)

    @Test
    fun `Should optimize a request with minimal data`() {
        val request = DcaRequest(
            amount = amountToInvest,
            assets = listOf(
                Asset(
                    ticker = "BTC",
                    weight = 20.0,
                    target = 50.0
                )
            )
        )

        handler.optimize(request).let { response ->
            expectThat(response.distribution)
                .hasSize(1)
                .containsKey("BTC")
        }
    }

    @Test
    fun `Should optimize a request with multiple assets and WEIGHT strategy`() {
        val request = DcaRequest(
            amount = amountToInvest,
            strategy = DcaStrategy(
                calculationFactor = CalculationFactor.WEIGHT
            ),
            assets = assets
        )

        handler.optimize(request).let { response ->
            expect {
                that(response.distribution).hasSize(3)
                that(response.distribution.keys).containsExactly("C", "D", "E")
                that(response.distribution["C"]).isEqualTo(BigDecimal("333.33"))
                that(response.distribution["D"]).isEqualTo(BigDecimal("500.00"))
                that(response.distribution["E"]).isEqualTo(BigDecimal("166.67"))
            }
        }
    }

    @Test
    fun `Should optimize a request with multiple assets and TARGET strategy`() {
        val request = DcaRequest(
            amount = amountToInvest,
            strategy = DcaStrategy(
                calculationFactor = CalculationFactor.TARGET
            ),
            assets = assets
        )

        handler.optimize(request).let { response ->
            expect {
                that(response.distribution).hasSize(3)
                that(response.distribution.keys).containsExactly("C", "D", "E")
                that(response.distribution["C"]).isEqualTo(BigDecimal("416.67"))
                that(response.distribution["D"]).isEqualTo(BigDecimal("416.67"))
                that(response.distribution["E"]).isEqualTo(BigDecimal("166.67"))
            }
        }
    }
}
