package io.github.dca.plugins

import io.github.dca.DcaRequest
import io.github.dca.DcaResponse
import io.github.dca.Distribution
import io.github.dca.StrategyType
import io.github.dca.distributeByRating
import io.github.dca.distributeByTarget
import io.github.dca.strategy.distributeByPortfolio
import io.github.dca.strategy.distributeByWeight
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

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
        StrategyType.DIVIDEND -> TODO()
        StrategyType.REBALANCE -> TODO()
    }

private fun calculateDistribution(
    request: DcaRequest,
    applyStrategy: (DcaRequest) -> Distribution
): DcaResponse = DcaResponse(applyStrategy(request))
