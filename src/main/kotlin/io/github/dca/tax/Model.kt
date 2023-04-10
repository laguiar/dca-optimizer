package io.github.dca.tax

import java.math.BigDecimal
import java.time.LocalDate

enum class Direction { BUY, SELL }
internal typealias Ticker = String

data class Transaction(
    val ticker: Ticker,
    val shares: Double,
    val direction: Direction,
    val price: BigDecimal,
    val date: LocalDate
)

data class TickerShares(val ticker: Ticker, val shares: Double)
