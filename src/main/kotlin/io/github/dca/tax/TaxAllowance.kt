package io.github.dca.tax

import io.github.dca.ZERO
import io.github.dca.divide
import io.github.dca.multiply
import io.github.dca.quotes.MarketClient
import java.math.BigDecimal
import kotlin.math.ceil

// TODO make it parameterizable
private val taxAllowanceCap = BigDecimal.valueOf(2000.0)
private val allowanceSafeMargin = taxAllowanceCap.multiply(0.99)

fun findProfitableTransactionsForTaxAllowance(
    transactions: List<Transaction>,
    marketClient: MarketClient
): List<TickerShares> {
    // Get current prices for a list of tickers
    val currentPrices = marketClient.getQuotes(
        tickers = transactions.map { it.ticker }.toSet()
    )

    return transactions
        .let(::consolidateTransactionHistory)
        .fold(listOf<TickerShares>() to BigDecimal.ZERO) { (profitableTransactions, profit), transaction ->
            calculateTransactionProfit(transaction, currentPrices, profit)
                ?.let { (tickerShares, totalProfit) ->
                    profitableTransactions.plus(tickerShares) to totalProfit
                }
                ?: (profitableTransactions to profit)
        }
        .first // returns only the list of TickerShares
}

private fun calculateTransactionProfit(
    transaction: Transaction,
    currentPrices: Map<Ticker, BigDecimal>,
    currentProfit: BigDecimal
): Pair<TickerShares, BigDecimal>? {
    val currentPrice = currentPrices[transaction.ticker] ?: BigDecimal.ZERO
    val remainingShares = transaction.shares
    val transactionProfit = (currentPrice - transaction.price).multiply(remainingShares)
    // check if adding all remaining shares will surpass the cap value
    return if (transactionProfit > BigDecimal.ZERO && currentProfit < allowanceSafeMargin) {
        val newProfit = currentProfit + transactionProfit
        if (newProfit <= taxAllowanceCap) {
            TickerShares(transaction.ticker, remainingShares) to newProfit
        } else {
            val profitPerShare = transactionProfit.divide(remainingShares)
            calculateSharesToBeSold(
                currentProfit = currentProfit,
                profitPerShare = profitPerShare,
                fractionalProfit = profitPerShare.divide(100.0)
            )
                .let { sharesToBeSold ->
                    TickerShares(transaction.ticker, sharesToBeSold) to
                            currentProfit + (profitPerShare.multiply(sharesToBeSold))
                }
        }
    } else null
}

private const val fractionalIncrement = 0.01
private fun calculateSharesToBeSold(
    currentProfit: BigDecimal,
    profitPerShare: BigDecimal,
    fractionalProfit: BigDecimal
) = generateSequence(ZERO) { it + fractionalIncrement }
    .takeWhile {
        val totalProfit = currentProfit + profitPerShare.multiply(it)
        totalProfit + fractionalProfit < taxAllowanceCap && totalProfit < allowanceSafeMargin
    }
    .last()
    .plus(fractionalIncrement) // since the takeWhile condition does not include the final increment
    .let { ceil(it * 100) / 100 } // trick to not get several decimals

