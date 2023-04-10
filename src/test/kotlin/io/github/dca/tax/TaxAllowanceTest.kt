package io.github.dca.tax

import io.github.dca.quotes.MarketClient
import io.github.dca.tax.Direction.BUY
import io.github.dca.tax.Direction.SELL
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEmpty
import java.math.BigDecimal
import java.time.LocalDate

class TaxAllowanceTest {

    private val marketClient = mockk<MarketClient>()

    @Test
    fun `should return empty list when there are no transactions`() {
        every { marketClient.getQuotes(any()) } returns emptyMap()

        val result = findProfitableTransactionsForTaxAllowance(emptyList(), marketClient)
        expectThat(result).isEmpty()
    }

    @Test
    fun `should return empty list when there are no profitable transactions`() {
        val transactions = listOf(
            Transaction("AAPL", 100.0, BUY, BigDecimal("100.0"), LocalDate.now().minusDays(1)),
            Transaction("GOOG", 50.0, BUY, BigDecimal("200.0"), LocalDate.now().minusDays(1)),
            Transaction("AAPL", 50.0, SELL, BigDecimal("120.0"), LocalDate.now()),
            Transaction("GOOG", 50.0, SELL, BigDecimal("190.0"), LocalDate.now())
        )
        every { marketClient.getQuotes(any()) } returns mapOf(
            "AAPL" to BigDecimal("99.0"),
            "GOOG" to BigDecimal("190.0")
        )

        val result = findProfitableTransactionsForTaxAllowance(transactions, marketClient)
        expectThat(result).isEmpty()
    }

    @Test
    fun `should return list of profitable shares to be sold for tax allowance usage`() {
        val transactions = listOf(
            Transaction("AAPL", 100.0, BUY, BigDecimal("100.0"), LocalDate.now().minusDays(3)),
            Transaction("GOOG", 100.0, BUY, BigDecimal("200.0"), LocalDate.now().minusDays(2)),
            Transaction("AAPL", 50.0, SELL, BigDecimal("110.0"), LocalDate.now().minusDays(1)),
            Transaction("GOOG", 50.0, SELL, BigDecimal("190.0"), LocalDate.now().minusDays(1)),
            Transaction("VOO", 50.0, BUY, BigDecimal("100.0"), LocalDate.now())
        )
        every { marketClient.getQuotes(any()) } returns mapOf(
            "AAPL" to BigDecimal("130.0"),
            "GOOG" to BigDecimal("220.0"),
            "VOO" to BigDecimal("150.0")
        )

        val result = findProfitableTransactionsForTaxAllowance(transactions, marketClient)
        // AAPL -> 30 profit per share = 1500
        // GOOG -> 20 profit per share = 1000
        expectThat(result).containsExactly(
            TickerShares("AAPL", 50.0),
            TickerShares("GOOG", 24.01)
        )
    }

    @Test
    fun `should consider fractional shares when calculating shares to be sold`() {
        val transactions = listOf(
            Transaction("FFF", 50.0, BUY, BigDecimal("100.0"), LocalDate.now()),
            Transaction("AAA", 80.0, BUY, BigDecimal("300.0"), LocalDate.now().minusDays(3)),
            Transaction("BBB", 100.0, BUY, BigDecimal("500.0"), LocalDate.now().minusDays(2)),
            Transaction("BBB", 50.0, SELL, BigDecimal("300.0"), LocalDate.now().minusDays(1)),
            Transaction("CCC", 50.0, BUY, BigDecimal("400.0"), LocalDate.now().minusDays(1)),
            Transaction("DDD", 50.0, BUY, BigDecimal("100.0"), LocalDate.now()),
            Transaction("GGG", 50.0, BUY, BigDecimal("100.0"), LocalDate.now().minusDays(4)),
            Transaction("AAA", 50.0, BUY, BigDecimal("280.0"), LocalDate.now().plusDays(1))
        )
        every { marketClient.getQuotes(any()) } returns mapOf(
            "AAA" to BigDecimal("310.0"),
            "BBB" to BigDecimal("510.0"),
            "CCC" to BigDecimal("485.0"),
            "DDD" to BigDecimal("120.0"),
            "FFF" to BigDecimal("120.0"),
            "GGG" to BigDecimal("110.0")
        )

        val result = findProfitableTransactionsForTaxAllowance(transactions, marketClient)
        expectThat(result).containsExactly(
            TickerShares("GGG", 50.0), // 500
            TickerShares("AAA", 80.0), // 800
            TickerShares("BBB", 50.0), // 500
            TickerShares("CCC", 2.12)
        )
    }

    @Test
    fun `should consider shares sold after the buy transactions to calculate still available shares`() {
        val transactions = listOf(
            // AAA - 50
            Transaction("AAA", 80.0, BUY, BigDecimal("300.0"), LocalDate.now().minusDays(7)),
            Transaction("AAA", 20.0, BUY, BigDecimal("300.0"), LocalDate.now().minusDays(7)),// all sold
            Transaction("AAA", 100.0, SELL, BigDecimal("500.0"), LocalDate.now().minusDays(6)),
            Transaction("AAA", 50.0, BUY, BigDecimal("300.0"), LocalDate.now().minusDays(5)),

            // BBB - 60
            Transaction("BBB", 100.0, BUY, BigDecimal("200.0"), LocalDate.now().minusDays(5)), //50
            Transaction("BBB", 20.0, SELL, BigDecimal("200.0"), LocalDate.now().minusDays(4)),
            Transaction("BBB", 30.0, SELL, BigDecimal("200.0"), LocalDate.now().minusDays(3)),
            Transaction("BBB", 10.0, BUY, BigDecimal("200.0"), LocalDate.now().minusDays(2)),

            // CCC - 30
            Transaction("CCC", 50.0, BUY, BigDecimal("400.0"), LocalDate.now().minusDays(4)),
            Transaction("CCC", 50.0, BUY, BigDecimal("400.0"), LocalDate.now().minusDays(4)),
            Transaction("CCC", 50.0, BUY, BigDecimal("400.0"), LocalDate.now().minusDays(4)),
            Transaction("CCC", 130.0, SELL, BigDecimal("400.0"), LocalDate.now().minusDays(3)),
            Transaction("CCC", 10.0, BUY, BigDecimal("400.0"), LocalDate.now().minusDays(2)),

            Transaction("DDD", 10.0, BUY, BigDecimal("100.0"), LocalDate.now().minusDays(1)),
        )

        every { marketClient.getQuotes(any()) } returns mapOf(
            "AAA" to BigDecimal("310.0"),
            "BBB" to BigDecimal("220.0"),
            "CCC" to BigDecimal("451.0"),
            "DDD" to BigDecimal("150.0"),
        )

        val result = findProfitableTransactionsForTaxAllowance(transactions, marketClient)
        expectThat(result).containsExactly(
            TickerShares("AAA", 50.0), // 500
            TickerShares("BBB", 50.0), // 1500
            TickerShares("CCC", 9.42) // 480,42
        )
    }

    @Test
    fun `should adjust a list of transactions - sequential order`() {
        // Input transactions
        val transactions = listOf(
            Transaction("AAA", 100.0, BUY, BigDecimal("100.0"), LocalDate.now().minusDays(17)),//50, 20, 0
            Transaction("AAA", 50.0, SELL, BigDecimal("100.0"), LocalDate.now().minusDays(16)),
            Transaction("AAA", 200.0, BUY, BigDecimal("111.0"), LocalDate.now().minusDays(15)),//250, 20
            Transaction("AAA", 230.0, SELL, BigDecimal("100.0"), LocalDate.now().minusDays(14)),
            Transaction("AAA", 70.0, BUY, BigDecimal("122.0"), LocalDate.now().minusDays(13)),

            Transaction("BBB", 1000.0, BUY, BigDecimal("100.0"), LocalDate.now().minusDays(12)),//500, 0
            Transaction("BBB", 500.0, SELL, BigDecimal("100.0"), LocalDate.now().minusDays(11)),
            Transaction("BBB", 800.0, BUY, BigDecimal("111.0"), LocalDate.now().minusDays(10)),//300
            Transaction("BBB", 1000.0, SELL, BigDecimal("100.0"), LocalDate.now().minusDays(9)),

            Transaction("CCC", 20.0, BUY, BigDecimal("100.0"), LocalDate.now().minusDays(8)),
            Transaction("CCC", 30.0, BUY, BigDecimal("111.0"), LocalDate.now().minusDays(7)),
            Transaction("CCC", 100.0, BUY, BigDecimal("122.0"), LocalDate.now().minusDays(6)),//10
            Transaction("CCC", 140.0, SELL, BigDecimal("100.0"), LocalDate.now().minusDays(5)),

            Transaction("DDD", 300.0, BUY, BigDecimal("100.0"), LocalDate.now().minusDays(4)),
            Transaction("DDD", 200.0, BUY, BigDecimal("111.0"), LocalDate.now().minusDays(3)),//100, 50
            Transaction("DDD", 400.0, SELL, BigDecimal("100.0"), LocalDate.now().minusDays(2)),
            Transaction("DDD", 50.0, SELL, BigDecimal("100.0"), LocalDate.now().minusDays(1)),

            Transaction("EEE", 50.0, BUY, BigDecimal("100.0"), LocalDate.now().minusDays(2)),
            Transaction("EEE", 50.0, SELL, BigDecimal("100.0"), LocalDate.now().minusDays(1)),
            Transaction("EEE", 42.0, BUY, BigDecimal("111.0"), LocalDate.now()),
        )

        // Expected output transactions
        val expectedTransactions = listOf(
            Transaction("AAA", 20.0, BUY, BigDecimal("111.0"), LocalDate.now().minusDays(15)),
            Transaction("AAA", 70.0, BUY, BigDecimal("122.0"), LocalDate.now().minusDays(13)),
            Transaction("BBB", 300.0, BUY, BigDecimal("111.0"), LocalDate.now().minusDays(10)),
            Transaction("CCC", 10.0, BUY, BigDecimal("122.0"), LocalDate.now().minusDays(6)),
            Transaction("DDD", 50.0, BUY, BigDecimal("111.0"), LocalDate.now().minusDays(3)),
            Transaction("EEE", 42.0, BUY, BigDecimal("111.0"), LocalDate.now()),
        )

        // Adjust transactions and check the output
        val adjustedTransactions = consolidateTransactionHistory(transactions)
        println(adjustedTransactions)
        expectThat(adjustedTransactions).containsExactly(expectedTransactions)
    }

    @Test
    fun `should adjust a list of transactions - mixed order`() {
        // Input transactions
        val transactions = listOf(
            Transaction("AAA", 100.0, BUY, BigDecimal("100.0"), LocalDate.now().minusDays(5)),//50, 20, 0
            Transaction("BBB", 1000.0, BUY, BigDecimal("100.0"), LocalDate.now().minusDays(5)),//500, 0
            Transaction("CCC", 20.0, BUY, BigDecimal("100.0"), LocalDate.now().minusDays(5)),
            Transaction("DDD", 300.0, BUY, BigDecimal("100.0"), LocalDate.now().minusDays(5)),
            Transaction("EEE", 50.0, BUY, BigDecimal("100.0"), LocalDate.now().minusDays(5)),

            Transaction("AAA", 50.0, SELL, BigDecimal("100.0"), LocalDate.now().minusDays(4)),
            Transaction("BBB", 500.0, SELL, BigDecimal("100.0"), LocalDate.now().minusDays(4)),
            Transaction("CCC", 30.0, BUY, BigDecimal("111.0"), LocalDate.now().minusDays(4)),
            Transaction("DDD", 200.0, BUY, BigDecimal("111.0"), LocalDate.now().minusDays(4)),//100, 50
            Transaction("EEE", 50.0, SELL, BigDecimal("100.0"), LocalDate.now().minusDays(4)),

            Transaction("AAA", 200.0, BUY, BigDecimal("111.0"), LocalDate.now().minusDays(3)),//250, 20
            Transaction("BBB", 800.0, BUY, BigDecimal("111.0"), LocalDate.now().minusDays(3)),//300
            Transaction("CCC", 100.0, BUY, BigDecimal("122.0"), LocalDate.now().minusDays(3)),//10
            Transaction("DDD", 400.0, SELL, BigDecimal("100.0"), LocalDate.now().minusDays(3)),
            Transaction("EEE", 42.0, BUY, BigDecimal("111.0"), LocalDate.now().minusDays(3)),

            Transaction("AAA", 230.0, SELL, BigDecimal("100.0"), LocalDate.now().minusDays(2)),
            Transaction("BBB", 1000.0, SELL, BigDecimal("100.0"), LocalDate.now().minusDays(2)),
            Transaction("CCC", 140.0, SELL, BigDecimal("100.0"), LocalDate.now().minusDays(2)),
            Transaction("DDD", 50.0, SELL, BigDecimal("100.0"), LocalDate.now().minusDays(2)),

            Transaction("AAA", 70.0, BUY, BigDecimal("122.0"), LocalDate.now().minusDays(1)),
        )

        // Expected output transactions
        val expectedTransactions = listOf(
            Transaction("DDD", 50.0, BUY, BigDecimal("111.0"), LocalDate.now().minusDays(4)),
            Transaction("AAA", 20.0, BUY, BigDecimal("111.0"), LocalDate.now().minusDays(3)),
            Transaction("BBB", 300.0, BUY, BigDecimal("111.0"), LocalDate.now().minusDays(3)),
            Transaction("CCC", 10.0, BUY, BigDecimal("122.0"), LocalDate.now().minusDays(3)),
            Transaction("EEE", 42.0, BUY, BigDecimal("111.0"), LocalDate.now().minusDays(3)),
            Transaction("AAA", 70.0, BUY, BigDecimal("122.0"), LocalDate.now().minusDays(1)),
        )

        // Adjust transactions and check the output
        val adjustedTransactions = consolidateTransactionHistory(transactions)
        println(adjustedTransactions)
        expectThat(adjustedTransactions).containsExactly(expectedTransactions)
    }
}
