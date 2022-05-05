package io.github.dca

import io.quarkus.vertx.web.Body
import io.quarkus.vertx.web.ReactiveRoutes.APPLICATION_JSON
import io.quarkus.vertx.web.Route
import io.quarkus.vertx.web.RouteBase
import javax.enterprise.context.ApplicationScoped
import javax.validation.Valid

@ApplicationScoped
@RouteBase(path = "/api", consumes = [APPLICATION_JSON], produces = [APPLICATION_JSON])
class ApplicationRouter(private val handler: DcaHandler) {

    @Route(path = "/optimize", methods = [Route.HttpMethod.POST])
    fun optimize(@Body @Valid request: DcaRequest): DcaResponse = handler.optimize(request)
}
