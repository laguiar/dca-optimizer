package io.github.dca.strategy

import io.github.dca.*
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.math.abs

class ConvexOptimizationKotlinTest {

    private val epsilon = 1e-2 // Tolerance for BigDecimal comparisons

    @Test
    fun `should return empty distribution when no eligible assets using Kotlin implementation`() {
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
    fun `should distribute amount among eligible under-weighted assets using Kotlin implementation`() {
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

        // GOOGL should get more allocation since it has higher target weight and larger deviation
        val aaplAllocation = result["AAPL"]!!.toDouble()
        val googlAllocation = result["GOOGL"]!!.toDouble()
        googlAllocation.shouldBeGreaterThan(aaplAllocation)
    }

    @Test
    fun `should handle assets with zero ATH threshold using Kotlin implementation`() {
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

        // ETH should get more allocation due to higher target weight
        val btcAllocation = result["BTC"]!!.toDouble()
        val ethAllocation = result["ETH"]!!.toDouble()
        ethAllocation.shouldBeGreaterThan(btcAllocation)
    }

    @Test
    fun `should allocate more to assets with higher target weights using Kotlin implementation`() {
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
    fun `should handle single eligible asset using Kotlin implementation`() {
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
    fun `should respect ATH threshold filtering using Kotlin implementation`() {
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
    fun `should handle complex optimization scenario using Kotlin implementation`() {
        val request = DcaRequest(
            amount = BigDecimal("10000.00"),
            strategy = DcaStrategy(
                type = StrategyType.CONVEX_OPTIMIZATION,
                thresholds = Thresholds(fromAth = 5.0, overTarget = 0.0)
            ),
            assets = listOf(
                Asset(ticker = "TECH1", weight = 5.0, target = 15.0, fromAth = 10.0),   // Deviation: 10.0
                Asset(ticker = "TECH2", weight = 8.0, target = 12.0, fromAth = 8.0),    // Deviation: 4.0
                Asset(ticker = "BOND1", weight = 20.0, target = 25.0, fromAth = 15.0),  // Deviation: 5.0
                Asset(ticker = "BOND2", weight = 18.0, target = 20.0, fromAth = 12.0),  // Deviation: 2.0
                Asset(ticker = "OVER1", weight = 30.0, target = 15.0, fromAth = 20.0),  // Over target (excluded)
                Asset(ticker = "LOW_ATH", weight = 5.0, target = 10.0, fromAth = 3.0)   // Below ATH threshold (excluded)
            )
        )

        val result = distributeByConvexOptimization(request)

        result.shouldNotBeEmpty()
        result.size shouldBeExactly 4
        result.shouldContainKey("TECH1")
        result.shouldContainKey("TECH2")
        result.shouldContainKey("BOND1")
        result.shouldContainKey("BOND2")

        // Total distribution should equal the investment amount
        val totalDistributed = result.values.sumOf { it }
        totalDistributed shouldBe BigDecimal("10000.00")

        // TECH1 has the highest deviation (10.0) and should get significant allocation
        val tech1Allocation = result["TECH1"]!!.toDouble()
        val tech2Allocation = result["TECH2"]!!.toDouble()
        val bond1Allocation = result["BOND1"]!!.toDouble()
        val bond2Allocation = result["BOND2"]!!.toDouble()

        // Assets with higher target weights and deviations should get more allocation
        tech1Allocation.shouldBeGreaterThan(tech2Allocation) // TECH1 has higher target and deviation
        // Allow equality within a small tolerance for bonds to accommodate LP corner solutions
        (bond1Allocation - bond2Allocation).shouldBeGreaterThan(-epsilon)

        // All allocations should be non-negative (allowing zero due to LP sparsity)
        tech1Allocation.shouldBeGreaterThan(-epsilon)
        tech2Allocation.shouldBeGreaterThan(-epsilon)
        bond1Allocation.shouldBeGreaterThan(-epsilon)
        bond2Allocation.shouldBeGreaterThan(-epsilon)
    }

    @Test
    fun `should produce consistent results with deterministic inputs using Kotlin implementation`() {
        val request = DcaRequest(
            amount = BigDecimal("1000.00"),
            strategy = DcaStrategy(
                type = StrategyType.CONVEX_OPTIMIZATION,
                thresholds = Thresholds(fromAth = 10.0, overTarget = 0.0)
            ),
            assets = listOf(
                Asset(ticker = "A", weight = 10.0, target = 20.0, fromAth = 15.0),
                Asset(ticker = "B", weight = 15.0, target = 25.0, fromAth = 12.0)
            )
        )

        // Run the optimization multiple times - should produce identical results
        val result1 = distributeByConvexOptimization(request)
        val result2 = distributeByConvexOptimization(request)
        val result3 = distributeByConvexOptimization(request)

        result1 shouldBe result2
        result2 shouldBe result3
    }

    private fun assertDoublesEqual(expected: Double, actual: Double, tolerance: Double = epsilon) {
        abs(expected - actual) shouldBeLessThan tolerance
    }
}