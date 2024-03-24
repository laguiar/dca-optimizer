package io.github.dca.tax

import io.github.dca.multiply
import java.math.BigDecimal

data class TaxAllowanceParameters(
    val taxAllowanceCap: BigDecimal = BigDecimal.valueOf(2000.0),
    val allowanceSafeMargin: BigDecimal = taxAllowanceCap.multiply(0.99)
)
