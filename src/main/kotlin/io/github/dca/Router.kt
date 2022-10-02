package io.github.dca

import io.quarkus.vertx.web.Body
import io.quarkus.vertx.web.ReactiveRoutes.APPLICATION_JSON
import io.quarkus.vertx.web.Route
import io.quarkus.vertx.web.RouteBase
import javax.enterprise.context.ApplicationScoped
import javax.validation.Valid

@ApplicationScoped
@RouteBase(path = "/api", consumes = [APPLICATION_JSON], produces = [APPLICATION_JSON])
class ApplicationRouter {

    @Route(path = "/optimize", methods = [Route.HttpMethod.POST])
    fun optimize(@Body @Valid request: DcaRequest): DcaResponse = processOptimization(request)
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
    applyStrategy(request)
        .let(::DcaResponse)
