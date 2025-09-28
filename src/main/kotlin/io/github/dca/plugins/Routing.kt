package io.github.dca.plugins

import io.github.dca.*
import io.github.dca.strategy.distributeByConvexOptimization
import io.github.dca.strategy.distributeByPortfolio
import io.github.dca.strategy.distributeByWeight
import io.github.dca.withdrawal.calculateAdvancedWithdrawalDuration
import io.github.dca.withdrawal.calculateInitialAmount
import io.github.dca.withdrawal.calculateWithdrawalDuration
import io.github.dca.withdrawal.simulateWithdrawalDuration
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        postApi<DcaRequest>("/api/optimize", ::processOptimization)
        postApi<WithdrawalCalculationRequest>("/api/simulate-withdrawal", ::simulateWithdrawalDuration)
        postApi<WithdrawalCalculationRequest>("/api/calculate-withdrawal", ::calculateWithdrawalDuration)
        postApi<AdvancedWithdrawalCalculationRequest>("/api/calculate-advanced-withdrawal", ::calculateAdvancedWithdrawalDuration)
        postApi<InitialAmountCalculationRequest>("/api/calculate-target-amount", ::calculateInitialAmount)
    }
}

private inline fun <reified Req : Any> Route.postApi(
    path: String,
    crossinline handler: suspend (Req) -> Any
) {
    post(path) {
        val request = call.receive<Req>()
        call.respond(HttpStatusCode.OK, handler(request))
    }
}

private fun processOptimization(request: DcaRequest): DcaResponse =
    when (request.strategy.type) {
        StrategyType.TARGET -> calculateDistribution(request, ::distributeByTarget)
        StrategyType.WEIGHT -> calculateDistribution(request, ::distributeByWeight)
        StrategyType.PORTFOLIO -> calculateDistribution(request, ::distributeByPortfolio)
        StrategyType.RATING -> calculateDistribution(request, ::distributeByRating)
        StrategyType.CONVEX_OPTIMIZATION -> calculateDistribution(request, ::distributeByConvexOptimization)
        StrategyType.DIVIDEND -> TODO()
        StrategyType.REBALANCE -> TODO()
    }

private fun calculateDistribution(
    request: DcaRequest,
    applyStrategy: (DcaRequest) -> Distribution
): DcaResponse = DcaResponse(applyStrategy(request))
