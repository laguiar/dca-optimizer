package io.github.dca.plugins

import io.github.dca.DcaRequest
import io.github.dca.StrategyType
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*
import java.math.BigDecimal

fun Application.configureRequestValidation() {
    install(RequestValidation) {
        validate<DcaRequest> { req ->
            val errors = mutableListOf<String>()
            if (req.assets.isEmpty()) {
                errors += "Assets list cannot be empty"
            }
            if (req.strategy.type == StrategyType.WEIGHT && req.portfolioValue <= BigDecimal.ZERO) {
                errors += "WEIGHT strategy requires Portfolio value"
            }

            if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors.joinToString("; "))
        }
    }
}
