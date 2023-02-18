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

private const val ATH_THRESHOLD = 5.0
private const val OVER_TARGET_THRESHOLD = 0.0

@Serializable
data class DcaRequest(
    val amount: BigDecimalNumber,
    val strategy: DcaStrategy = DcaStrategy.default(),
    val assets: List<Asset>
) {
    init {
        require(assets.isNotEmpty()) { "Assets list cannot be empty" }
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
    TARGET, WEIGHT, PORTFOLIO, RATING, DIVIDEND
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
