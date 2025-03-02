package io.github.dca.plugins

import io.github.dca.DcaRequest
import io.github.dca.DcaResponse
import io.github.dca.Distribution
import io.github.dca.InitialAmountCalculationRequest
import io.github.dca.StrategyType
import io.github.dca.WithdrawalCalculationRequest
import io.github.dca.calculateInitialAmount
import io.github.dca.calculateWithdrawalDuration
import io.github.dca.distributeByRating
import io.github.dca.distributeByTarget
import io.github.dca.simulateWithdrawalDuration
import io.github.dca.strategy.distributeByPortfolio
import io.github.dca.strategy.distributeByWeight
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
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
        post("/api/simulate-withdrawal") {
            call.receive<WithdrawalCalculationRequest>()
                .let { call.respond(HttpStatusCode.OK, simulateWithdrawalDuration(it)) }
        }
        post("/api/calculate-withdrawal") {
            call.receive<WithdrawalCalculationRequest>()
                .let { call.respond(HttpStatusCode.OK, calculateWithdrawalDuration(it)) }
        }
        post("/api/calculate-target-amount") {
            call.receive<InitialAmountCalculationRequest>()
                .let { call.respond(HttpStatusCode.OK, calculateInitialAmount(it)) }
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
