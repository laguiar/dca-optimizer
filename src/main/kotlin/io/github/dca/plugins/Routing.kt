package io.github.dca.plugins

import io.github.dca.*
import io.github.dca.strategy.distributeByPortfolio
import io.github.dca.strategy.distributeByWeight
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

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
        post("/api/calculate-advanced-withdrawal") {
            call.receive<AdvancedWithdrawalCalculationRequest>()
                .let { call.respond(HttpStatusCode.OK, calculateAdvancedWithdrawalDuration(it)) }
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
