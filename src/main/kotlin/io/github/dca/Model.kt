package io.github.dca

import java.math.BigDecimal
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

private const val ATH_THRESHOLD = 10.0
private const val OVER_TARGET_THRESHOLD = 0.0

data class DcaRequest(
    @field: NotNull
    val amount: BigDecimal,
    val strategy: DcaStrategy? = null,
    @field: NotEmpty
    val assets: List<Asset>
)

data class OptimizerResponse(val distribution: Distribution)

data class Asset(
    val ticker: String,
    val weight: Double,
    val target: Double,
    val belowAth: Double? = null
)

data class DcaStrategy(
    val calculationFactor: CalculationFactor = CalculationFactor.TARGET,
    val athThreshold: Double = ATH_THRESHOLD,
    val overTargetThreshold: Double = OVER_TARGET_THRESHOLD
) {
    companion object {
        fun default() = DcaStrategy()
    }
}

enum class CalculationFactor {
    TARGET, WEIGHT
}
