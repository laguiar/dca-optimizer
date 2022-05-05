package io.github.dca

import java.math.BigDecimal
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

private const val ATH_THRESHOLD = 5.0
private const val OVER_TARGET_THRESHOLD = 0.0

data class DcaRequest(
    @field: NotNull
    val amount: BigDecimal,
    val strategy: DcaStrategy? = null,
    @field: NotEmpty
    val assets: List<Asset>
)

data class DcaResponse(val distribution: Distribution)

data class Asset(
    val ticker: String,
    val weight: Double,
    val target: Double,
    val fromAth: Double? = null
)

data class DcaStrategy(
    val calculationFactor: CalculationFactor,
    val thresholds: Thresholds
) {
    companion object {
        fun default() = DcaStrategy(
            calculationFactor = CalculationFactor.TARGET,
            thresholds = Thresholds()
        )
    }
}

data class Thresholds(
    val fromAth: Double = ATH_THRESHOLD,
    val overTarget: Double = OVER_TARGET_THRESHOLD
)

enum class CalculationFactor {
    TARGET, WEIGHT, PORTFOLIO
}
