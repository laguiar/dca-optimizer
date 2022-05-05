package io.github.dca

import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.containsKey
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import java.math.BigDecimal
import java.math.MathContext

internal class DcaHandlerTest {

    private val handler = DcaHandler()

    // ATH default threshold value: 5.0
    private val assets = listOf(
        Asset(ticker = "A", weight = 25.0, target = 20.0, fromAth = 20.0), // over-target
        Asset(ticker = "B", weight = 15.0, target = 20.0, fromAth = 4.0), // under-target but too close to ATH
        Asset(ticker = "C", weight = 15.0, target = 25.0, fromAth = 15.0), // under-target
        Asset(ticker = "D", weight = 10.0, target = 25.0, fromAth = 15.0), // under-target
        Asset(ticker = "E", weight = 5.0, target = 10.0, fromAth = 22.0)   // under-target
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
                calculationFactor = CalculationFactor.WEIGHT,
                thresholds = Thresholds(
                    fromAth = 10.0,
                    overTarget = 0.1
                )
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
            strategy = DcaStrategy.default(),
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

    @Test
    fun `Should optimize a request with multiple assets and PORTFOLIO strategy`() {
        // 9.0% of over-target will be divided by 3 assets
        val assets = listOf(
            Asset(ticker = "A", weight = 25.0, target = 20.0), // target will be 15.0
            Asset(ticker = "B", weight = 14.0, target = 10.0), // target will be 6.0
            Asset(ticker = "C", weight = 15.0, target = 30.0), // target will be 33.0
            Asset(ticker = "D", weight = 10.0, target = 30.0), // target will be 33.0
            Asset(ticker = "E", weight = 5.0, target = 10.0) //  target will be 13.00
        )

        val request = DcaRequest(
            amount = amountToInvest,
            strategy = DcaStrategy(
                calculationFactor = CalculationFactor.PORTFOLIO,
                thresholds = Thresholds(
                    overTarget = 0.0
                )
            ),
            assets = assets
        )

        handler.optimize(request).let { response ->
            expect {
                that(response.distribution).hasSize(5)
                that(response.distribution.keys).containsExactly("A", "B","C", "D", "E")
                that(response.distribution["A"]).isEqualTo(BigDecimal("150.00"))
                that(response.distribution["B"]).isEqualTo(BigDecimal("60.00"))
                that(response.distribution["C"]).isEqualTo(BigDecimal("330.00"))
                that(response.distribution["D"]).isEqualTo(BigDecimal("330.00"))
                that(response.distribution["E"]).isEqualTo(BigDecimal("130.00"))
            }
        }
    }
}
