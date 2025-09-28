package io.github.dca

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal

typealias BigDecimalNumber = @Serializable(with = BigDecimalNumericSerializer::class) BigDecimal
typealias Distribution = Map<String, BigDecimalNumber>

private const val ATH_THRESHOLD = 8.0
private const val OVER_TARGET_THRESHOLD = 0.01

@Serializable
data class WithdrawalCalculationRequest(
    val totalAmount: BigDecimalNumber,
    val monthlyWithdraw: BigDecimalNumber,
    val expectedYearlyReturn: Double
)

@Serializable
data class WithdrawalCalculationResponse(
    val years: Double,
    val isInfinite: Boolean
)

/**
 * Advanced withdrawal calculation request that includes inflation and tax considerations.
 * 
 * @param totalAmount The initial total amount available for withdrawal
 * @param monthlyWithdraw The amount to withdraw each month
 * @param expectedYearlyReturn The expected annual return rate as a percentage
 * @param yearlyInflationRate The annual inflation rate as a percentage
 * @param yearlyTaxAllowance The annual tax-free capital gains allowance
 * @param averageTaxRate The average tax rate applied to capital gains exceeding the allowance
 */
@Serializable
data class AdvancedWithdrawalCalculationRequest(
    val totalAmount: BigDecimalNumber,
    val monthlyWithdraw: BigDecimalNumber,
    val expectedYearlyReturn: Double,
    val yearlyInflationRate: Double = 0.0,
    val yearlyTaxAllowance: BigDecimalNumber = BigDecimal.ZERO,
    val averageTaxRate: Double = 0.0
)

/**
 * Response for advanced withdrawal calculation that includes detailed information.
 * 
 * @param years The number of years the money will last
 * @param isInfinite Whether the money will last indefinitely
 * @param realReturn The real return rate after inflation
 * @param totalTaxPaid The total amount of tax paid over the withdrawal period
 * @param inflationAdjustedWithdrawal The final monthly withdrawal amount adjusted for inflation
 * @param yearlyBreakdown Optional detailed breakdown of calculations by year
 */
@Serializable
data class AdvancedWithdrawalCalculationResponse(
    val years: Double,
    val isInfinite: Boolean,
    val realReturn: Double,
    val totalTaxPaid: BigDecimalNumber,
    val inflationAdjustedWithdrawal: BigDecimalNumber,
    val yearlyBreakdown: List<YearlyBreakdown>? = null
)

/**
 * Detailed breakdown of calculations for a specific year.
 * 
 * @param year The year number (1-based)
 * @param startingBalance The balance at the start of the year
 * @param returns The investment returns earned during the year
 * @param withdrawals The total withdrawals made during the year
 * @param taxPaid The tax paid on capital gains during the year
 * @param inflationImpact The impact of inflation on purchasing power
 * @param endingBalance The balance at the end of the year
 */
@Serializable
data class YearlyBreakdown(
    val year: Int,
    val startingBalance: BigDecimalNumber,
    val returns: BigDecimalNumber,
    val withdrawals: BigDecimalNumber,
    val taxPaid: BigDecimalNumber,
    val inflationImpact: BigDecimalNumber,
    val endingBalance: BigDecimalNumber
)

@Serializable
data class InitialAmountCalculationRequest(
    val shouldLastForYears: Double,
    val monthlyWithdraw: BigDecimalNumber,
    val expectedYearlyReturn: Double
)

@Serializable
data class InitialAmountCalculationResponse(
    val totalAmount: BigDecimalNumber
)

@Serializable
data class DcaRequest(
    val amount: BigDecimalNumber,
    val portfolioValue: BigDecimalNumber = BigDecimal.ZERO,
    val strategy: DcaStrategy = DcaStrategy.default(),
    val assets: List<Asset>
) {
    init {
        require(assets.isNotEmpty()) { "Assets list cannot be empty" }
        require(
            if (strategy.type == StrategyType.WEIGHT) portfolioValue > BigDecimal.ZERO else true
        ) { "WEIGHT strategy requires Portfolio value" }
    }
}

@Serializable
data class DcaResponse(val distribution: Distribution)

@Serializable
data class Asset(
    val ticker: String,
    val weight: Double = 0.0,
    val target: Double = 0.0,
    val fromAth: Double = 0.0,
    val rating: Int = 0,
    val yield: Double = 0.0,
    val gains: BigDecimalNumber = BigDecimal.ZERO
) {
    val isWeightBellowTarget = weight < target
}

@Serializable
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

@Serializable
data class Thresholds(
    val fromAth: Double = ATH_THRESHOLD,
    val overTarget: Double = OVER_TARGET_THRESHOLD
)

enum class StrategyType {
    TARGET, WEIGHT, PORTFOLIO, RATING, DIVIDEND, REBALANCE, CONVEX_OPTIMIZATION
}

private object BigDecimalNumericSerializer : KSerializer<BigDecimal> {

    override val descriptor = PrimitiveSerialDescriptor("java.math.BigDecimal", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): BigDecimal =
        if (decoder is JsonDecoder) {
            BigDecimal(decoder.decodeJsonElement().jsonPrimitive.content)
        } else {
            BigDecimal(decoder.decodeString())
        }

    override fun serialize(encoder: Encoder, value: BigDecimal) =
        encoder.encodeString(value.toPlainString())
}
