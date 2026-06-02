package nl.parkeerassistent.util

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.routing.Route
import io.ktor.server.routing.RouteSelector
import io.ktor.server.routing.RouteSelectorEvaluation
import io.ktor.server.routing.RoutingNode
import io.ktor.server.routing.RoutingResolveContext
import nl.parkeerassistent.RouterGroup
import nl.parkeerassistent.configureErrorHandling

/**
 * Groups routes that require an authenticated session: before reaching any wrapped handler the
 * request must carry a `token` cookie, otherwise [checkTokenCookie] throws [NoTokenCookieException]
 * (turned into a 401 by [configureErrorHandling]). Uses a transparent selector so wrapped paths
 * resolve unchanged. Keep login routes outside this wrapper.
 */
fun Route.requireTokenCookie(build: RouterGroup): Route {
    val route = createChild(TokenCookieRouteSelector()) as RoutingNode
    route.intercept(ApplicationCallPipeline.Plugins) {
        checkTokenCookie(call)
    }
    return route.apply(build)
}

private class TokenCookieRouteSelector : RouteSelector() {
    override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation =
        RouteSelectorEvaluation.Transparent
}


fun checkTokenCookie(call: ApplicationCall) {
    if (call.request.cookies["token"] == null) {
        throw NoTokenCookieException()
    }
}

class NoTokenCookieException : RuntimeException("No token cookie found")