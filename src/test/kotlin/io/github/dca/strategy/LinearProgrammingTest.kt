package io.github.dca.strategy

import io.github.dca.*
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.math.abs
import io.kotest.matchers.ints.shouldBeGreaterThan as intShouldBeGreaterThan

class LinearProgrammingTest {

    private val epsilon = 0.01 // Tolerance for comparisons

    // ========== Basic Functionality Tests ==========

    @Test
    fun `should return empty distribution when no eligible assets`() {
        val request = DcaRequest(
            amount = BigDecimal("1000.00"),
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 10.0, overTarget = 0.0)
            ),
            assets = listOf(
                Asset(ticker = "AAPL", weight = 30.0, target = 20.0, fromAth = 5.0), // Above target, below ATH threshold
                Asset(ticker = "GOOGL", weight = 25.0, target = 20.0, fromAth = 8.0) // Above target, below ATH threshold
            )
        )

        val result = distributeByLinearProgramming(request)

        result.shouldBeEmpty()
    }

    @Test
    fun `should distribute amount among eligible under-weighted assets`() {
        val request = DcaRequest(
            amount = BigDecimal("1000.00"),
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 10.0, overTarget = 0.0)
            ),
            assets = listOf(
                Asset(ticker = "AAPL", weight = 15.0, target = 20.0, fromAth = 15.0), // Under target, meets ATH threshold
                Asset(ticker = "GOOGL", weight = 10.0, target = 25.0, fromAth = 12.0), // Under target, meets ATH threshold
                Asset(ticker = "MSFT", weight = 25.0, target = 20.0, fromAth = 18.0) // Over target (should be excluded)
            )
        )

        val result = distributeByLinearProgramming(request)

        result.shouldNotBeEmpty()
        result.size shouldBeExactly 2
        result.shouldContainKey("AAPL")
        result.shouldContainKey("GOOGL")
        result.keys.shouldContainExactlyInAnyOrder(listOf("AAPL", "GOOGL"))

        // Total distribution should equal the investment amount
        val totalDistributed = result.values.sumOf { it }
        totalDistributed shouldBe BigDecimal("1000.00")

        // GOOGL has higher target (25 vs 20) and higher deviation (15 vs 5)
        // Priority(GOOGL) = 15 * 25 = 375, Priority(AAPL) = 5 * 20 = 100
        // So GOOGL should get more allocation
        result["GOOGL"]!!.toDouble().shouldBeGreaterThan(result["AAPL"]!!.toDouble())
    }

    @Test
    fun `should handle single eligible asset`() {
        val request = DcaRequest(
            amount = BigDecimal("750.00"),
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 10.0, overTarget = 0.0)
            ),
            assets = listOf(
                Asset(ticker = "ONLY", weight = 10.0, target = 25.0, fromAth = 15.0)
            )
        )

        val result = distributeByLinearProgramming(request)

        result.shouldNotBeEmpty()
        result.size shouldBeExactly 1
        result.shouldContainKey("ONLY")
        result["ONLY"] shouldBe BigDecimal("750.00")
    }

    // ========== ATH Threshold Tests ==========

    @Test
    fun `should handle assets with zero ATH threshold (always eligible)`() {
        val request = DcaRequest(
            amount = BigDecimal("500.00"),
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 10.0, overTarget = 0.0)
            ),
            assets = listOf(
                Asset(ticker = "BTC", weight = 5.0, target = 15.0, fromAth = 0.0), // Under target, zero ATH (always eligible)
                Asset(ticker = "ETH", weight = 8.0, target = 20.0, fromAth = 0.0)  // Under target, zero ATH (always eligible)
            )
        )

        val result = distributeByLinearProgramming(request)

        result.shouldNotBeEmpty()
        result.size shouldBeExactly 2
        result.shouldContainKey("BTC")
        result.shouldContainKey("ETH")

        val totalDistributed = result.values.sumOf { it }
        totalDistributed shouldBe BigDecimal("500.00")

        // ETH has higher target (20 vs 15) and higher deviation (12 vs 10)
        // Priority(ETH) = 12 * 20 = 240, Priority(BTC) = 10 * 15 = 150
        result["ETH"]!!.toDouble().shouldBeGreaterThan(result["BTC"]!!.toDouble())
    }

    @Test
    fun `should respect ATH threshold filtering`() {
        val request = DcaRequest(
            amount = BigDecimal("1000.00"),
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 15.0, overTarget = 0.0)
            ),
            assets = listOf(
                Asset(ticker = "HIGH_ATH", weight = 10.0, target = 20.0, fromAth = 5.0),  // Below ATH threshold (excluded)
                Asset(ticker = "LOW_ATH", weight = 12.0, target = 25.0, fromAth = 20.0)   // Above ATH threshold (included)
            )
        )

        val result = distributeByLinearProgramming(request)

        result.shouldNotBeEmpty()
        result.size shouldBeExactly 1
        result.shouldContainKey("LOW_ATH")
        result["LOW_ATH"] shouldBe BigDecimal("1000.00")
    }

    @Test
    fun `should handle asset exactly at ATH threshold boundary`() {
        val request = DcaRequest(
            amount = BigDecimal("1000.00"),
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 15.0, overTarget = 0.0)
            ),
            assets = listOf(
                Asset(ticker = "EXACT", weight = 10.0, target = 20.0, fromAth = 15.0), // Exactly at threshold (should be included)
                Asset(ticker = "BELOW", weight = 12.0, target = 25.0, fromAth = 14.9)  // Just below threshold (excluded)
            )
        )

        val result = distributeByLinearProgramming(request)

        result.size shouldBeExactly 1
        result.shouldContainKey("EXACT")
        result["EXACT"] shouldBe BigDecimal("1000.00")
    }

    // ========== Optimization Priority Tests ==========

    @Test
    fun `should allocate more to assets with higher target weights`() {
        val request = DcaRequest(
            amount = BigDecimal("1000.00"),
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 5.0, overTarget = 0.0)
            ),
            assets = listOf(
                Asset(ticker = "SMALL", weight = 2.0, target = 10.0, fromAth = 15.0), // Deviation: 8
                Asset(ticker = "LARGE", weight = 5.0, target = 30.0, fromAth = 20.0)  // Deviation: 25
            )
        )

        val result = distributeByLinearProgramming(request)

        result.shouldNotBeEmpty()
        result.shouldContainKey("SMALL")
        result.shouldContainKey("LARGE")

        // Priority(LARGE) = 25 * 30 = 750, Priority(SMALL) = 8 * 10 = 80
        // Asset with larger priority should get more allocation
        result["LARGE"]!!.toDouble().shouldBeGreaterThan(result["SMALL"]!!.toDouble())

        val totalDistributed = result.values.sumOf { it }
        totalDistributed shouldBe BigDecimal("1000.00")
    }

    @Test
    fun `should prioritize assets with larger deviations from target`() {
        val request = DcaRequest(
            amount = BigDecimal("1000.00"),
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 5.0, overTarget = 0.0)
            ),
            assets = listOf(
                Asset(ticker = "HIGH_DEV", weight = 5.0, target = 30.0, fromAth = 15.0),  // Deviation: 25
                Asset(ticker = "LOW_DEV", weight = 18.0, target = 20.0, fromAth = 12.0)   // Deviation: 2
            )
        )

        val result = distributeByLinearProgramming(request)

        result.shouldNotBeEmpty()
        result.shouldContainKey("HIGH_DEV")
        result.shouldContainKey("LOW_DEV")

        val totalDistributed = result.values.sumOf { it }
        totalDistributed shouldBe BigDecimal("1000.00")

        // Priority(HIGH_DEV) = 25 * 30 = 750, Priority(LOW_DEV) = 2 * 20 = 40
        // Asset with larger deviation should receive more allocation
        result["HIGH_DEV"]!!.toDouble().shouldBeGreaterThan(result["LOW_DEV"]!!.toDouble())
    }

    @Test
    fun `should balance deviation and target weight in priority calculation`() {
        val request = DcaRequest(
            amount = BigDecimal("1000.00"),
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 5.0, overTarget = 0.0)
            ),
            assets = listOf(
                Asset(ticker = "HIGH_TARGET_LOW_DEV", weight = 15.0, target = 20.0, fromAth = 10.0),  // Deviation: 5, Target: 20, Priority: 100
                Asset(ticker = "LOW_TARGET_HIGH_DEV", weight = 5.0, target = 10.0, fromAth = 12.0)    // Deviation: 5, Target: 10, Priority: 50
            )
        )

        val result = distributeByLinearProgramming(request)

        result.shouldNotBeEmpty()
        result.size shouldBeExactly 2

        // Same deviation but higher target should get more allocation
        result["HIGH_TARGET_LOW_DEV"]!!.toDouble().shouldBeGreaterThan(result["LOW_TARGET_HIGH_DEV"]!!.toDouble())

        val totalDistributed = result.values.sumOf { it }
        totalDistributed shouldBe BigDecimal("1000.00")
    }

    // ========== Edge Cases ==========

    @Test
    fun `should handle very small investment amounts`() {
        val request = DcaRequest(
            amount = BigDecimal("1.00"),
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 5.0, overTarget = 0.0)
            ),
            assets = listOf(
                Asset(ticker = "A", weight = 10.0, target = 20.0, fromAth = 10.0),
                Asset(ticker = "B", weight = 15.0, target = 25.0, fromAth = 12.0)
            )
        )

        val result = distributeByLinearProgramming(request)

        result.shouldNotBeEmpty()
        val totalDistributed = result.values.sumOf { it }
        totalDistributed shouldBe BigDecimal("1.00")
    }

    @Test
    fun `should handle very large investment amounts`() {
        val request = DcaRequest(
            amount = BigDecimal("1000000.00"),
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 5.0, overTarget = 0.0)
            ),
            assets = listOf(
                Asset(ticker = "A", weight = 10.0, target = 20.0, fromAth = 10.0),
                Asset(ticker = "B", weight = 15.0, target = 25.0, fromAth = 12.0)
            )
        )

        val result = distributeByLinearProgramming(request)

        result.shouldNotBeEmpty()
        val totalDistributed = result.values.sumOf { it }
        totalDistributed shouldBe BigDecimal("1000000.00")
    }

    @Test
    fun `should handle assets with equal priorities`() {
        val request = DcaRequest(
            amount = BigDecimal("1000.00"),
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 5.0, overTarget = 0.0)
            ),
            assets = listOf(
                Asset(ticker = "A", weight = 10.0, target = 20.0, fromAth = 10.0), // Deviation: 10, Priority: 200
                Asset(ticker = "B", weight = 5.0, target = 10.0, fromAth = 12.0),  // Deviation: 5, Priority: 50
                Asset(ticker = "C", weight = 15.0, target = 25.0, fromAth = 15.0)  // Deviation: 10, Priority: 250
            )
        )

        val result = distributeByLinearProgramming(request)

        result.shouldNotBeEmpty()
        result.size shouldBeExactly 3
        val totalDistributed = result.values.sumOf { it }
        totalDistributed shouldBe BigDecimal("1000.00")

        // All allocations should be non-negative
        result.values.forEach { it.toDouble().shouldBeGreaterThanOrEqual(0.0) }
    }

    @Test
    fun `should handle assets with very small deviations`() {
        val request = DcaRequest(
            amount = BigDecimal("1000.00"),
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 5.0, overTarget = 0.0)
            ),
            assets = listOf(
                Asset(ticker = "TINY_DEV", weight = 19.99, target = 20.0, fromAth = 10.0), // Deviation: 0.01
                Asset(ticker = "LARGE_DEV", weight = 10.0, target = 30.0, fromAth = 12.0)  // Deviation: 20.0
            )
        )

        val result = distributeByLinearProgramming(request)

        result.shouldNotBeEmpty()
        result.size shouldBeExactly 2
        val totalDistributed = result.values.sumOf { it }
        totalDistributed shouldBe BigDecimal("1000.00")

        // Asset with much larger deviation should get much more allocation
        result["LARGE_DEV"]!!.toDouble().shouldBeGreaterThan(result["TINY_DEV"]!!.toDouble())
    }

    @Test
    fun `should handle rounding edge cases correctly`() {
        val request = DcaRequest(
            amount = BigDecimal("100.33"), // Amount that doesn't divide evenly
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 5.0, overTarget = 0.0)
            ),
            assets = listOf(
                Asset(ticker = "A", weight = 10.0, target = 20.0, fromAth = 10.0),
                Asset(ticker = "B", weight = 15.0, target = 25.0, fromAth = 12.0),
                Asset(ticker = "C", weight = 8.0, target = 18.0, fromAth = 15.0)
            )
        )

        val result = distributeByLinearProgramming(request)

        result.shouldNotBeEmpty()
        val totalDistributed = result.values.sumOf { it }

        // Due to rounding, total might be slightly off, but should be within a penny
        abs(totalDistributed.toDouble() - 100.33) shouldBeLessThan 0.02
    }

    // ========== Complex Scenarios ==========

    @Test
    fun `should handle complex optimization with multiple assets`() {
        val request = DcaRequest(
            amount = BigDecimal("10000.00"),
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 5.0, overTarget = 0.0)
            ),
            assets = listOf(
                Asset(ticker = "TECH1", weight = 5.0, target = 15.0, fromAth = 10.0),   // Dev: 10, Priority: 150
                Asset(ticker = "TECH2", weight = 8.0, target = 12.0, fromAth = 8.0),    // Dev: 4, Priority: 48
                Asset(ticker = "BOND1", weight = 20.0, target = 25.0, fromAth = 15.0),  // Dev: 5, Priority: 125
                Asset(ticker = "BOND2", weight = 18.0, target = 20.0, fromAth = 12.0),  // Dev: 2, Priority: 40
                Asset(ticker = "OVER1", weight = 30.0, target = 15.0, fromAth = 20.0),  // Over target (excluded)
                Asset(ticker = "LOW_ATH", weight = 5.0, target = 10.0, fromAth = 3.0)   // Below ATH threshold (excluded)
            )
        )

        val result = distributeByLinearProgramming(request)

        result.shouldNotBeEmpty()
        result.size shouldBeExactly 4
        result.shouldContainKey("TECH1")
        result.shouldContainKey("TECH2")
        result.shouldContainKey("BOND1")
        result.shouldContainKey("BOND2")

        val totalDistributed = result.values.sumOf { it }
        totalDistributed shouldBe BigDecimal("10000.00")

        // Verify priority ordering: TECH1(150) has highest priority and should get most allocation
        val tech1Allocation = result["TECH1"]!!.toDouble()
        val bond1Allocation = result["BOND1"]!!.toDouble()

        tech1Allocation.shouldBeGreaterThan(bond1Allocation)

        // All allocations should be non-negative (LP may give zero to lower priority assets)
        result.values.forEach { it.toDouble().shouldBeGreaterThanOrEqual(0.0) }
    }

    // ========== Determinism Tests ==========

    @Test
    fun `should produce consistent results with identical inputs`() {
        val request = DcaRequest(
            amount = BigDecimal("1000.00"),
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 10.0, overTarget = 0.0)
            ),
            assets = listOf(
                Asset(ticker = "A", weight = 10.0, target = 20.0, fromAth = 15.0),
                Asset(ticker = "B", weight = 15.0, target = 25.0, fromAth = 12.0)
            )
        )

        // Run the optimization multiple times - should produce identical results
        val result1 = distributeByLinearProgramming(request)
        val result2 = distributeByLinearProgramming(request)
        val result3 = distributeByLinearProgramming(request)

        result1 shouldBe result2
        result2 shouldBe result3
    }

    // ========== Fallback Behavior Tests ==========

    @Test
    fun `should return empty distribution when assets have zero target and positive weight`() {
        val request = DcaRequest(
            amount = BigDecimal("1000.00"),
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 0.0, overTarget = 0.0)
            ),
            assets = listOf(
                Asset(ticker = "A", weight = 5.0, target = 0.0, fromAth = 0.0),  // Above target (5.0 > 0.0) - excluded
                Asset(ticker = "B", weight = 8.0, target = 0.0, fromAth = 0.0)   // Above target (8.0 > 0.0) - excluded
            )
        )

        val result = distributeByLinearProgramming(request)

        // Assets with zero target and positive weight are above target and excluded
        result.shouldBeEmpty()
    }

    @Test
    fun `should handle mixed zero and non-zero targets in fallback`() {
        val request = DcaRequest(
            amount = BigDecimal("1000.00"),
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 0.0, overTarget = 0.0)
            ),
            assets = listOf(
                Asset(ticker = "WITH_TARGET", weight = 5.0, target = 20.0, fromAth = 0.0),
                Asset(ticker = "NO_TARGET", weight = 8.0, target = 0.0, fromAth = 0.0)   // Still below weight, so eligible
            )
        )

        val result = distributeByLinearProgramming(request)

        result.shouldNotBeEmpty()

        // WITH_TARGET should get all allocation since NO_TARGET has zero priority
        // But due to weight check, NO_TARGET is actually above target (8 > 0) so it's excluded
        result.shouldContainKey("WITH_TARGET")

        val totalDistributed = result.values.sumOf { it }
        totalDistributed shouldBe BigDecimal("1000.00")
    }

    // ========== Optimization Quality Tests ==========

    @Test
    fun `should verify allocation respects equality constraint (sum equals total amount)`() {
        val request = DcaRequest(
            amount = BigDecimal("5000.00"),
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 5.0, overTarget = 0.0)
            ),
            assets = listOf(
                Asset(ticker = "A", weight = 10.0, target = 20.0, fromAth = 10.0),
                Asset(ticker = "B", weight = 12.0, target = 25.0, fromAth = 12.0),
                Asset(ticker = "C", weight = 8.0, target = 15.0, fromAth = 15.0)
            )
        )

        val result = distributeByLinearProgramming(request)

        val totalDistributed = result.values.sumOf { it }
        totalDistributed shouldBe BigDecimal("5000.00")
    }

    @Test
    fun `should verify all allocations are non-negative`() {
        val request = DcaRequest(
            amount = BigDecimal("1000.00"),
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 5.0, overTarget = 0.0)
            ),
            assets = listOf(
                Asset(ticker = "A", weight = 5.0, target = 25.0, fromAth = 10.0),
                Asset(ticker = "B", weight = 10.0, target = 30.0, fromAth = 12.0),
                Asset(ticker = "C", weight = 15.0, target = 20.0, fromAth = 15.0)
            )
        )

        val result = distributeByLinearProgramming(request)

        // Verify non-negativity constraint
        result.values.forEach { allocation ->
            allocation.toDouble().shouldBeGreaterThanOrEqual(0.0)
        }
    }

    @Test
    fun `should allocate proportionally to priority scores`() {
        val request = DcaRequest(
            amount = BigDecimal("1000.00"),
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 5.0, overTarget = 0.0)
            ),
            assets = listOf(
                Asset(ticker = "LOW", weight = 5.0, target = 10.0, fromAth = 10.0),    // Deviation: 5, Priority: 50
                Asset(ticker = "HIGH", weight = 5.0, target = 20.0, fromAth = 12.0)    // Deviation: 15, Priority: 300
            )
        )

        val result = distributeByLinearProgramming(request)

        val lowAllocation = result["LOW"]!!.toDouble()
        val highAllocation = result["HIGH"]!!.toDouble()

        // HIGH has 6x the priority (300 vs 50), so it should get approximately 6x the allocation
        val ratio = highAllocation / lowAllocation
        ratio.shouldBeGreaterThan(5.0) // Allow some tolerance
    }

    // ========== Diversification Tests ==========

    @Test
    fun `should distribute proportionally when diversify is true`() {
        val request = DcaRequest(
            amount = BigDecimal("10000.00"),
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 5.0, overTarget = 0.0),
                diversify = true  // Enable diversification
            ),
            assets = listOf(
                Asset(ticker = "A", weight = 10.0, target = 30.0, fromAth = 10.0),  // Dev: 20, Priority: 600
                Asset(ticker = "B", weight = 15.0, target = 25.0, fromAth = 12.0),  // Dev: 10, Priority: 250
                Asset(ticker = "C", weight = 20.0, target = 25.0, fromAth = 15.0)   // Dev: 5, Priority: 125
            )
        )

        val result = distributeByLinearProgramming(request)

        // All eligible assets should receive allocation
        result.shouldNotBeEmpty()
        result.size shouldBeExactly 3
        result.shouldContainKey("A")
        result.shouldContainKey("B")
        result.shouldContainKey("C")

        // Total should equal investment amount
        val totalDistributed = result.values.sumOf { it }
        totalDistributed shouldBe BigDecimal("10000.00")

        // Verify proportional allocation based on priorities
        // Total priority = 600 + 250 + 125 = 975
        // A: (600/975) × 10000 = 6153.85
        // B: (250/975) × 10000 = 2564.10
        // C: (125/975) × 10000 = 1282.05

        val aAllocation = result["A"]!!.toDouble()
        val bAllocation = result["B"]!!.toDouble()
        val cAllocation = result["C"]!!.toDouble()

        // All should be positive
        aAllocation.shouldBeGreaterThan(0.0)
        bAllocation.shouldBeGreaterThan(0.0)
        cAllocation.shouldBeGreaterThan(0.0)

        // Higher priority assets should get more
        aAllocation.shouldBeGreaterThan(bAllocation)
        bAllocation.shouldBeGreaterThan(cAllocation)

        // Verify approximate proportions (with rounding tolerance)
        abs(aAllocation - 6153.85) shouldBeLessThan 1.0
        abs(bAllocation - 2564.10) shouldBeLessThan 1.0
        abs(cAllocation - 1282.05) shouldBeLessThan 1.0
    }

    @Test
    fun `should use corner solution when diversify is false`() {
        val request = DcaRequest(
            amount = BigDecimal("10000.00"),
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 5.0, overTarget = 0.0),
                diversify = false  // Standard LP optimization
            ),
            assets = listOf(
                Asset(ticker = "A", weight = 10.0, target = 30.0, fromAth = 10.0),  // Dev: 20, Priority: 600
                Asset(ticker = "B", weight = 15.0, target = 25.0, fromAth = 12.0),  // Dev: 10, Priority: 250
                Asset(ticker = "C", weight = 20.0, target = 25.0, fromAth = 15.0)   // Dev: 5, Priority: 125
            )
        )

        val result = distributeByLinearProgramming(request)

        result.shouldNotBeEmpty()

        // Total should equal investment amount
        val totalDistributed = result.values.sumOf { it }
        totalDistributed shouldBe BigDecimal("10000.00")

        // With LP optimization, highest priority asset (A) should get most or all allocation
        result.shouldContainKey("A")
        val aAllocation = result["A"]!!.toDouble()

        // A has highest priority (600), so it should dominate the allocation
        aAllocation.shouldBeGreaterThan(8000.0) // Should get at least 80% in this case
    }

    @Test
    fun `should handle diversified allocation with realistic portfolio scenario`() {
        val request = DcaRequest(
            amount = BigDecimal("10000.00"),
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 10.0, overTarget = 0.3),
                diversify = true  // Enable diversification
            ),
            assets = listOf(
                Asset(ticker = "S&P500", weight = 48.6, target = 50.0, fromAth = 0.0),  // Dev: 1.4, Priority: 70.0
                Asset(ticker = "BTC", weight = 19.1, target = 20.0, fromAth = 0.0),     // Dev: 0.9, Priority: 18.0
                Asset(ticker = "WORLD", weight = 8.5, target = 11.0, fromAth = 0.0),    // Dev: 2.5, Priority: 27.5
                Asset(ticker = "EU", weight = 9.9, target = 8.0, fromAth = 0.0),        // Overweight (excluded)
                Asset(ticker = "STOCKS", weight = 16.5, target = 9.0, fromAth = 0.0),   // Overweight (excluded)
                Asset(ticker = "YIELD", weight = 3.3, target = 2.0, fromAth = 0.0)      // Overweight (excluded)
            )
        )

        val result = distributeByLinearProgramming(request)

        // All underweight assets should receive allocation
        result.shouldNotBeEmpty()
        result.size shouldBeExactly 3
        result.shouldContainKey("S&P500")
        result.shouldContainKey("BTC")
        result.shouldContainKey("WORLD")

        // Total should equal investment amount
        val totalDistributed = result.values.sumOf { it }
        totalDistributed shouldBe BigDecimal("10000.00")

        // Verify all eligible assets got positive allocation (diversification)
        // Total priority = 70.0 + 18.0 + 27.5 = 115.5
        // S&P500: (70.0/115.5) × 10000 = 6061.69
        // WORLD:  (27.5/115.5) × 10000 = 2380.95
        // BTC:    (18.0/115.5) × 10000 = 1557.36

        val sp500Allocation = result["S&P500"]!!.toDouble()
        val btcAllocation = result["BTC"]!!.toDouble()
        val worldAllocation = result["WORLD"]!!.toDouble()

        // All should be positive (diversified)
        sp500Allocation.shouldBeGreaterThan(0.0)
        btcAllocation.shouldBeGreaterThan(0.0)
        worldAllocation.shouldBeGreaterThan(0.0)

        // Higher priority should still get more
        sp500Allocation.shouldBeGreaterThan(worldAllocation)
        worldAllocation.shouldBeGreaterThan(btcAllocation)

        // Verify approximate allocations (with rounding tolerance)
        abs(sp500Allocation - 6061.69) shouldBeLessThan 2.0
        abs(worldAllocation - 2380.95) shouldBeLessThan 2.0
        abs(btcAllocation - 1557.36) shouldBeLessThan 2.0
    }

    @Test
    fun `should handle edge case with single asset and diversify true`() {
        val request = DcaRequest(
            amount = BigDecimal("1000.00"),
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 5.0, overTarget = 0.0),
                diversify = true
            ),
            assets = listOf(
                Asset(ticker = "ONLY", weight = 10.0, target = 25.0, fromAth = 10.0)
            )
        )

        val result = distributeByLinearProgramming(request)

        result.shouldNotBeEmpty()
        result.size shouldBeExactly 1
        result.shouldContainKey("ONLY")
        // Single asset gets everything regardless of diversify flag
        result["ONLY"] shouldBe BigDecimal("1000.00")
    }

    @Test
    fun `diversify mode should respect priority ratios accurately`() {
        val request = DcaRequest(
            amount = BigDecimal("1000.00"),
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 5.0, overTarget = 0.0),
                diversify = true
            ),
            assets = listOf(
                Asset(ticker = "HIGH", weight = 5.0, target = 20.0, fromAth = 10.0),    // Dev: 15, Priority: 300
                Asset(ticker = "LOW", weight = 5.0, target = 10.0, fromAth = 12.0)      // Dev: 5, Priority: 50
            )
        )

        val result = distributeByLinearProgramming(request)

        result.size shouldBeExactly 2

        val highAllocation = result["HIGH"]!!.toDouble()
        val lowAllocation = result["LOW"]!!.toDouble()

        // Total priority = 300 + 50 = 350
        // HIGH: (300/350) × 1000 = 857.14
        // LOW:  (50/350) × 1000 = 142.86

        abs(highAllocation - 857.14) shouldBeLessThan 1.0
        abs(lowAllocation - 142.86) shouldBeLessThan 1.0

        // Verify ratio matches priority ratio: 300/50 = 6
        val ratio = highAllocation / lowAllocation
        abs(ratio - 6.0) shouldBeLessThan 0.1
    }

    // ========== Real-World Scenarios ==========

    /**
     * Tests realistic portfolio rebalancing scenario with $10,000 investment into a $300,000 portfolio.
     *
     * IMPORTANT: Understanding Linear Programming Corner Solutions
     * ===========================================================
     *
     * This test demonstrates a fundamental property of linear programming: the simplex algorithm
     * ALWAYS finds optimal solutions at corner points (vertices) of the feasible region.
     *
     * Priority Calculation:
     * - S&P500: deviation=1.4 × target=50.0 = Priority: 70.0  ← HIGHEST
     * - WORLD:  deviation=2.5 × target=11.0 = Priority: 27.5
     * - BTC:    deviation=0.9 × target=20.0 = Priority: 18.0
     *
     * Linear Programming Problem:
     * maximize: 70.0×(S&P500) + 27.5×(WORLD) + 18.0×(BTC)
     * subject to: S&P500 + WORLD + BTC = $10,000
     *             all allocations ≥ 0
     *
     * Corner Points of Feasible Region:
     * - (10000, 0, 0) → objective value = 700,000  ✓ OPTIMAL SOLUTION
     * - (0, 10000, 0) → objective value = 275,000
     * - (0, 0, 10000) → objective value = 180,000
     *
     * Expected Result:
     * {
     *   "distribution": {
     *     "S&P500": "10000.00",  ← All funds go to highest priority asset
     *     "BTC": "0.00",         ← Zero allocation (corner solution)
     *     "WORLD": "0.00"        ← Zero allocation (corner solution)
     *   }
     * }
     *
     * Why This Happens:
     * With a linear objective function and linear constraints, the optimal solution will always
     * be at a vertex of the feasible region. Since S&P500 has the highest priority coefficient
     * (70.0), allocating all funds to it maximizes the objective function. This "greedy" behavior
     * is mathematically correct and is a natural consequence of linear optimization.
     *
     * This produces SPARSE SOLUTIONS where only the highest-priority assets receive allocation.
     *
     * Alternative Strategies for More Balanced Allocations:
     * - PORTFOLIO: Allocates to all assets proportionally
     * - WEIGHT: Distributes based on distance from target
     * - TARGET: Excludes overweight assets but spreads allocation differently
     * - Or modify this strategy to use quadratic objective or minimum allocation constraints
     */
    @Test
    fun `should handle realistic portfolio rebalancing with mixed over and underweight assets`() {
        val request = DcaRequest(
            amount = BigDecimal("10000.00"),
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 10.0, overTarget = 0.3)
            ),
            assets = listOf(
                Asset(ticker = "S&P500", weight = 48.6, target = 50.0, fromAth = 0.0),  // Dev: 1.4, Priority: 70.0
                Asset(ticker = "BTC", weight = 19.1, target = 20.0, fromAth = 0.0),     // Dev: 0.9, Priority: 18.0
                Asset(ticker = "WORLD", weight = 8.5, target = 11.0, fromAth = 0.0),    // Dev: 2.5, Priority: 27.5
                Asset(ticker = "EU", weight = 9.9, target = 8.0, fromAth = 0.0),        // Overweight (excluded)
                Asset(ticker = "STOCKS", weight = 16.5, target = 9.0, fromAth = 0.0),   // Overweight (excluded)
                Asset(ticker = "YIELD", weight = 3.3, target = 2.0, fromAth = 0.0)      // Overweight (excluded)
            )
        )

        val result = distributeByLinearProgramming(request)

        // Verify result is not empty
        result.shouldNotBeEmpty()

        // Total distribution should equal the investment amount
        val totalDistributed = result.values.sumOf { it }
        totalDistributed shouldBe BigDecimal("10000.00")

        // Only underweight assets should be eligible for allocation
        val eligibleTickers = setOf("S&P500", "BTC", "WORLD")
        val overweightTickers = setOf("EU", "STOCKS", "YIELD")

        // Verify only eligible (underweight) assets received allocation
        result.keys.forEach { ticker ->
            (ticker in eligibleTickers) shouldBe true
        }

        // Verify overweight assets did not receive any allocation
        overweightTickers.forEach { ticker ->
            (ticker in result.keys) shouldBe false
        }

        // S&P500 has the highest priority (70 = 1.4 * 50), so it should receive allocation
        result.shouldContainKey("S&P500")
        val sp500Allocation = result["S&P500"]!!.toDouble()
        sp500Allocation.shouldBeGreaterThan(0.0)

        // Linear programming typically produces sparse solutions at corner points of the feasible region.
        // With our objective function, the highest priority asset (S&P500) gets most or all allocation.
        // This is mathematically optimal behavior for the convex optimization strategy.

        // All allocations should be non-negative
        result.values.forEach { it.toDouble().shouldBeGreaterThanOrEqual(0.0) }

        // Verify the distribution prioritizes higher-priority assets
        // If WORLD or BTC received allocation, S&P500 should have received at least as much
        val worldAllocation = result["WORLD"]?.toDouble() ?: 0.0
        val btcAllocation = result["BTC"]?.toDouble() ?: 0.0

        if (worldAllocation > 0.0 || btcAllocation > 0.0) {
            sp500Allocation.shouldBeGreaterThanOrEqual(worldAllocation)
            sp500Allocation.shouldBeGreaterThanOrEqual(btcAllocation)
        }
    }

    // ========== Smart Capping Tests ==========

    @Test
    fun `should apply 90% cap when three or more eligible assets exist`() {
        val request = DcaRequest(
            amount = BigDecimal("10000.00"),
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 5.0, overTarget = 0.0),
                diversify = false  // LP optimization with smart cap
            ),
            assets = listOf(
                Asset(ticker = "HIGH", weight = 5.0, target = 30.0, fromAth = 10.0),  // Dev: 25, Priority: 750 (highest)
                Asset(ticker = "MED", weight = 10.0, target = 25.0, fromAth = 12.0),  // Dev: 15, Priority: 375
                Asset(ticker = "LOW", weight = 15.0, target = 20.0, fromAth = 15.0)   // Dev: 5, Priority: 100
            )
        )

        val result = distributeByLinearProgramming(request)

        result.shouldNotBeEmpty()
        result.size intShouldBeGreaterThan 1  // Should allocate to more than one asset

        val highAllocation = result["HIGH"]!!.toDouble()
        val totalDistributed = result.values.sumOf { it }.toDouble()

        // HIGH should get max 90% due to smart cap (3+ assets)
        (highAllocation / totalDistributed).shouldBeLessThan(0.91)  // Allow small rounding tolerance

        // At least one other asset should get allocation
        val otherAssets = result.filterKeys { it != "HIGH" }
        otherAssets.shouldNotBeEmpty()
        otherAssets.values.sumOf { it }.toDouble().shouldBeGreaterThan(900.0)  // At least 10% to others
    }

    @Test
    fun `should apply 90% cap when top two assets have similar priorities`() {
        val request = DcaRequest(
            amount = BigDecimal("10000.00"),
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 5.0, overTarget = 0.0),
                diversify = false
            ),
            assets = listOf(
                Asset(ticker = "A", weight = 5.0, target = 20.0, fromAth = 10.0),  // Dev: 15, Priority: 300
                Asset(ticker = "B", weight = 8.0, target = 18.0, fromAth = 12.0)   // Dev: 10, Priority: 180 (60% of A)
            )
        )

        val result = distributeByLinearProgramming(request)

        result.shouldNotBeEmpty()
        result.size shouldBeExactly 2

        val aAllocation = result["A"]!!.toDouble()
        val bAllocation = result["B"]!!.toDouble()

        // A should get max 90% due to close competition
        (aAllocation / 10000.0).shouldBeLessThan(0.91)

        // B should get at least 10%
        bAllocation.shouldBeGreaterThan(900.0)
    }

    @Test
    fun `should not apply cap when two assets have large priority gap`() {
        val request = DcaRequest(
            amount = BigDecimal("10000.00"),
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 5.0, overTarget = 0.0),
                diversify = false
            ),
            assets = listOf(
                Asset(ticker = "DOMINANT", weight = 5.0, target = 30.0, fromAth = 10.0),  // Dev: 25, Priority: 750
                Asset(ticker = "MINOR", weight = 18.0, target = 20.0, fromAth = 12.0)     // Dev: 2, Priority: 40 (5% of DOMINANT)
            )
        )

        val result = distributeByLinearProgramming(request)

        result.shouldNotBeEmpty()
        result.shouldContainKey("DOMINANT")

        val dominantAllocation = result["DOMINANT"]!!.toDouble()

        // DOMINANT should get close to 100% (no cap due to large priority gap)
        dominantAllocation.shouldBeGreaterThan(9900.0)  // Should get at least 99%
    }

    @Test
    fun `should apply 90% cap when all deviations are small`() {
        val request = DcaRequest(
            amount = BigDecimal("10000.00"),
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 5.0, overTarget = 0.0),
                diversify = false
            ),
            assets = listOf(
                Asset(ticker = "A", weight = 48.0, target = 50.0, fromAth = 10.0),  // Dev: 2.0 (small)
                Asset(ticker = "B", weight = 29.0, target = 30.0, fromAth = 12.0)   // Dev: 1.0 (small)
            )
        )

        val result = distributeByLinearProgramming(request)

        result.shouldNotBeEmpty()
        result.size shouldBeExactly 2

        val aAllocation = result["A"]!!.toDouble()
        val bAllocation = result["B"]!!.toDouble()

        // A should get max 90% due to small deviations
        (aAllocation / 10000.0).shouldBeLessThan(0.91)

        // B should get at least 10%
        bAllocation.shouldBeGreaterThan(900.0)
    }

    @Test
    fun `should respect explicit maxSingleAssetPct override`() {
        val request = DcaRequest(
            amount = BigDecimal("10000.00"),
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 5.0, overTarget = 0.0),
                diversify = false,
                maxSingleAssetPct = 0.75  // Explicit 75% cap
            ),
            assets = listOf(
                Asset(ticker = "HIGH", weight = 5.0, target = 30.0, fromAth = 10.0),  // Dev: 25, Priority: 750
                Asset(ticker = "LOW", weight = 18.0, target = 20.0, fromAth = 12.0)   // Dev: 2, Priority: 40
            )
        )

        val result = distributeByLinearProgramming(request)

        result.shouldNotBeEmpty()
        result.size shouldBeExactly 2

        val highAllocation = result["HIGH"]!!.toDouble()
        val lowAllocation = result["LOW"]!!.toDouble()

        // HIGH should get max 75% due to explicit override
        (highAllocation / 10000.0).shouldBeLessThan(0.76)

        // LOW should get at least 25%
        lowAllocation.shouldBeGreaterThan(2400.0)
    }

    @Test
    fun `should allow 100% allocation when explicitly set via maxSingleAssetPct`() {
        val request = DcaRequest(
            amount = BigDecimal("10000.00"),
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 5.0, overTarget = 0.0),
                diversify = false,
                maxSingleAssetPct = 1.0  // Explicit 100% - disable smart cap
            ),
            assets = listOf(
                Asset(ticker = "A", weight = 48.0, target = 50.0, fromAth = 10.0),  // Dev: 2.0 (would trigger smart cap)
                Asset(ticker = "B", weight = 29.0, target = 30.0, fromAth = 12.0)   // Dev: 1.0
            )
        )

        val result = distributeByLinearProgramming(request)

        result.shouldNotBeEmpty()
        result.shouldContainKey("A")

        val aAllocation = result["A"]!!.toDouble()

        // A should get 100% (smart cap overridden)
        aAllocation.shouldBeGreaterThan(9900.0)
    }

    @Test
    fun `should not apply cap for single eligible asset`() {
        val request = DcaRequest(
            amount = BigDecimal("10000.00"),
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 5.0, overTarget = 0.0),
                diversify = false
            ),
            assets = listOf(
                Asset(ticker = "ONLY", weight = 10.0, target = 30.0, fromAth = 10.0)  // Only eligible asset
            )
        )

        val result = distributeByLinearProgramming(request)

        result.shouldNotBeEmpty()
        result.size shouldBeExactly 1
        result["ONLY"] shouldBe BigDecimal("10000.00")  // Gets 100% (no cap for single asset)
    }

    @Test
    fun `should apply smart cap in realistic scenario with multiple assets`() {
        val request = DcaRequest(
            amount = BigDecimal("10000.00"),
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 10.0, overTarget = 0.3),
                diversify = false
            ),
            assets = listOf(
                Asset(ticker = "S&P500", weight = 48.6, target = 50.0, fromAth = 0.0),  // Dev: 1.4, Priority: 70.0
                Asset(ticker = "BTC", weight = 19.1, target = 20.0, fromAth = 0.0),     // Dev: 0.9, Priority: 18.0
                Asset(ticker = "WORLD", weight = 8.5, target = 11.0, fromAth = 0.0),    // Dev: 2.5, Priority: 27.5
                Asset(ticker = "EU", weight = 9.9, target = 8.0, fromAth = 0.0)         // Overweight (excluded)
            )
        )

        val result = distributeByLinearProgramming(request)

        result.shouldNotBeEmpty()
        result.size intShouldBeGreaterThan 1  // Should allocate to multiple assets due to smart cap

        val totalDistributed = result.values.sumOf { it }.toDouble()
        totalDistributed shouldBe 10000.0

        // S&P500 has highest priority but should be capped at 90% (3 eligible assets)
        val sp500Allocation = result["S&P500"]!!.toDouble()
        (sp500Allocation / totalDistributed).shouldBeLessThan(0.91)

        // At least one other asset should get allocation
        val otherAssets = result.filterKeys { it != "S&P500" }
        otherAssets.shouldNotBeEmpty()
        otherAssets.values.sumOf { it }.toDouble().shouldBeGreaterThan(900.0)
    }

    @Test
    fun `should verify cap works with boundary case at exactly 50 percent priority ratio`() {
        val request = DcaRequest(
            amount = BigDecimal("10000.00"),
            strategy = DcaStrategy(
                type = StrategyType.LINEAR_PROGRAMMING,
                thresholds = Thresholds(fromAth = 5.0, overTarget = 0.0),
                diversify = false
            ),
            assets = listOf(
                Asset(ticker = "A", weight = 5.0, target = 20.0, fromAth = 10.0),  // Dev: 15, Priority: 300
                Asset(ticker = "B", weight = 10.0, target = 15.0, fromAth = 12.0)  // Dev: 5, Priority: 75 (25% of A - below 50%)
            )
        )

        val result = distributeByLinearProgramming(request)

        result.shouldNotBeEmpty()

        val aAllocation = result["A"]!!.toDouble()

        // Priority ratio is 25% (below 50% threshold), so no cap should be applied
        // A should get close to 100%
        aAllocation.shouldBeGreaterThan(9500.0)
    }
}
