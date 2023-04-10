package io.github.dca.quotes

import java.math.BigDecimal

interface MarketClient {
    fun getQuotes(tickers: Set<String>): Map<String, BigDecimal>
}
