package io.github.dca.strategy

import io.github.dca.*
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ConvexOptimizationTest {

    @Test
    fun `should return empty distribution when no eligible assets`() {
        val request = DcaRequest(
            amount = BigDecimal("1000.00"),
            strategy = DcaStrategy(
                type = StrategyType.CONVEX_OPTIMIZATION,
                thresholds = Thresholds(fromAth = 10.0, overTarget = 0.0)
            ),
            assets = listOf(
                Asset(ticker = "AAPL", weight = 30.0, target = 20.0, fromAth = 5.0), // Above target, below ATH threshold
                Asset(ticker = "GOOGL", weight = 25.0, target = 20.0, fromAth = 8.0) // Above target, below ATH threshold
            )
        )

        val result = distributeByConvexOptimization(request)

        result.shouldBeEmpty()
    }

    @Test
    fun `should distribute amount among eligible under-weighted assets`() {
        val request = DcaRequest(
            amount = BigDecimal("1000.00"),
            strategy = DcaStrategy(
                type = StrategyType.CONVEX_OPTIMIZATION,
                thresholds = Thresholds(fromAth = 10.0, overTarget = 0.0)
            ),
            assets = listOf(
                Asset(ticker = "AAPL", weight = 15.0, target = 20.0, fromAth = 15.0), // Under target, meets ATH threshold
                Asset(ticker = "GOOGL", weight = 10.0, target = 25.0, fromAth = 12.0), // Under target, meets ATH threshold
                Asset(ticker = "MSFT", weight = 25.0, target = 20.0, fromAth = 18.0) // Over target (should be excluded)
            )
        )

        val result = distributeByConvexOptimization(request)

        result.shouldNotBeEmpty()
        result.size shouldBeExactly 2
        result.shouldContainKey("AAPL")
        result.shouldContainKey("GOOGL")
        result.keys.shouldContainExactlyInAnyOrder(listOf("AAPL", "GOOGL"))

        // Total distribution should equal the investment amount
        val totalDistributed = result.values.sumOf { it }
        totalDistributed shouldBe BigDecimal("1000.00")
    }

    @Test
    fun `should handle assets with zero ATH threshold (always eligible)`() {
        val request = DcaRequest(
            amount = BigDecimal("500.00"),
            strategy = DcaStrategy(
                type = StrategyType.CONVEX_OPTIMIZATION,
                thresholds = Thresholds(fromAth = 10.0, overTarget = 0.0)
            ),
            assets = listOf(
                Asset(ticker = "BTC", weight = 5.0, target = 15.0, fromAth = 0.0), // Under target, zero ATH (always eligible)
                Asset(ticker = "ETH", weight = 8.0, target = 20.0, fromAth = 0.0)  // Under target, zero ATH (always eligible)
            )
        )

        val result = distributeByConvexOptimization(request)

        result.shouldNotBeEmpty()
        result.size shouldBeExactly 2
        result.shouldContainKey("BTC")
        result.shouldContainKey("ETH")

        val totalDistributed = result.values.sumOf { it }
        totalDistributed shouldBe BigDecimal("500.00")
    }

    @Test
    fun `should allocate more to assets with higher target weights`() {
        val request = DcaRequest(
            amount = BigDecimal("1000.00"),
            strategy = DcaStrategy(
                type = StrategyType.CONVEX_OPTIMIZATION,
                thresholds = Thresholds(fromAth = 5.0, overTarget = 0.0)
            ),
            assets = listOf(
                Asset(ticker = "SMALL", weight = 2.0, target = 10.0, fromAth = 15.0), // Small target
                Asset(ticker = "LARGE", weight = 5.0, target = 30.0, fromAth = 20.0)  // Large target
            )
        )

        val result = distributeByConvexOptimization(request)

        result.shouldNotBeEmpty()
        result.shouldContainKey("SMALL")
        result.shouldContainKey("LARGE")

        // Asset with larger target should get more allocation
        val smallAllocation = result["SMALL"]!!.toDouble()
        val largeAllocation = result["LARGE"]!!.toDouble()

        largeAllocation.shouldBeGreaterThan(smallAllocation)

        val totalDistributed = result.values.sumOf { it }
        totalDistributed shouldBe BigDecimal("1000.00")
    }

    @Test
    fun `should handle single eligible asset`() {
        val request = DcaRequest(
            amount = BigDecimal("750.00"),
            strategy = DcaStrategy(
                type = StrategyType.CONVEX_OPTIMIZATION,
                thresholds = Thresholds(fromAth = 10.0, overTarget = 0.0)
            ),
            assets = listOf(
                Asset(ticker = "ONLY", weight = 10.0, target = 25.0, fromAth = 15.0) // Only eligible asset
            )
        )

        val result = distributeByConvexOptimization(request)

        result.shouldNotBeEmpty()
        result.size shouldBeExactly 1
        result.shouldContainKey("ONLY")
        result["ONLY"] shouldBe BigDecimal("750.00")
    }

    @Test
    fun `should respect ATH threshold filtering`() {
        val request = DcaRequest(
            amount = BigDecimal("1000.00"),
            strategy = DcaStrategy(
                type = StrategyType.CONVEX_OPTIMIZATION,
                thresholds = Thresholds(fromAth = 15.0, overTarget = 0.0)
            ),
            assets = listOf(
                Asset(ticker = "HIGH_ATH", weight = 10.0, target = 20.0, fromAth = 5.0),  // Below ATH threshold (excluded)
                Asset(ticker = "LOW_ATH", weight = 12.0, target = 25.0, fromAth = 20.0)   // Above ATH threshold (included)
            )
        )

        val result = distributeByConvexOptimization(request)

        result.shouldNotBeEmpty()
        result.size shouldBeExactly 1
        result.shouldContainKey("LOW_ATH")
        result["LOW_ATH"] shouldBe BigDecimal("1000.00")
    }

    @Test
    fun `should prioritize assets with larger deviations from target`() {
        val request = DcaRequest(
            amount = BigDecimal("1000.00"),
            strategy = DcaStrategy(
                type = StrategyType.CONVEX_OPTIMIZATION,
                thresholds = Thresholds(fromAth = 5.0, overTarget = 0.0)
            ),
            assets = listOf(
                Asset(ticker = "HIGH_DEV", weight = 5.0, target = 30.0, fromAth = 15.0),  // 25% below target
                Asset(ticker = "LOW_DEV", weight = 18.0, target = 20.0, fromAth = 12.0)   // 2% below target
            )
        )

        val result = distributeByConvexOptimization(request)

        result.shouldNotBeEmpty()
        result.shouldContainKey("HIGH_DEV")
        result.shouldContainKey("LOW_DEV")

        // Total allocation should equal investment amount
        val totalDistributed = result.values.sumOf { it }
        totalDistributed shouldBe BigDecimal("1000.00")

        // Asset with larger deviation from target should receive more allocation
        val highDevAllocation = result["HIGH_DEV"]!!.toDouble()
        val lowDevAllocation = result["LOW_DEV"]!!.toDouble()

        highDevAllocation.shouldBeGreaterThan(lowDevAllocation)
    }
}