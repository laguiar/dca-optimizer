package io.github.dca

import java.math.BigDecimal
import javax.validation.constraints.NotEmpty
import javax.validation.constraints.NotNull

private const val ATH_THRESHOLD = 5.0
private const val OVER_TARGET_THRESHOLD = 0.0

data class DcaRequest(
    @field: NotNull
    val amount: BigDecimal,
    val strategy: DcaStrategy = DcaStrategy.default(),
    @field: NotEmpty
    val assets: List<Asset>
)

data class DcaResponse(val distribution: Distribution)

data class Asset(
    val ticker: String,
    val weight: Double = 0.0,
    val target: Double = 0.0,
    val fromAth: Double = 0.0,
    val rating: Int = 0,
    val yield: Double = 0.0,
)

data class DcaStrategy(
    val type: StrategyType,
    val thresholds: Thresholds = Thresholds(
        fromAth = ATH_THRESHOLD,
        overTarget = OVER_TARGET_THRESHOLD
    )
) {
    companion object {
        fun default() = DcaStrategy(
            type = StrategyType.TARGET,
            thresholds = Thresholds(
                fromAth = ATH_THRESHOLD,
                overTarget = OVER_TARGET_THRESHOLD
            )
        )
    }
}

data class Thresholds(
    val fromAth: Double = ATH_THRESHOLD,
    val overTarget: Double = OVER_TARGET_THRESHOLD
)

enum class StrategyType {
    TARGET, WEIGHT, PORTFOLIO, RATING, DIVIDEND
}
