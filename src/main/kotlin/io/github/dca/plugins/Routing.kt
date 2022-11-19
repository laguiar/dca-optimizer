package io.github.dca.plugins

import io.github.dca.DcaRequest
import io.github.dca.DcaResponse
import io.github.dca.Distribution
import io.github.dca.StrategyType
import io.github.dca.distributeByPortfolio
import io.github.dca.distributeByRating
import io.github.dca.distributeByTarget
import io.github.dca.distributeByWeight
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*

fun Application.configureRouting() {
    routing {
        post("/api/optimize") {
            call.receive<DcaRequest>()
                .let { call.respond(HttpStatusCode.OK, processOptimization(it)) }
        }
    }
}

private fun processOptimization(request: DcaRequest): DcaResponse =
    when (request.strategy.type) {
        StrategyType.TARGET -> calculateDistribution(request, ::distributeByTarget)
        StrategyType.WEIGHT -> calculateDistribution(request, ::distributeByWeight)
        StrategyType.PORTFOLIO -> calculateDistribution(request, ::distributeByPortfolio)
        StrategyType.RATING -> calculateDistribution(request, ::distributeByRating)
        else -> TODO()
    }

private fun calculateDistribution(
    request: DcaRequest,
    applyStrategy: (DcaRequest) -> Distribution
): DcaResponse =
    DcaResponse(applyStrategy(request))
