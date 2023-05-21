package com.ballast.shoppe.feature.router

import com.copperleaf.ballast.navigation.routing.Route
import com.copperleaf.ballast.navigation.routing.RouteAnnotation
import com.copperleaf.ballast.navigation.routing.RouteMatcher

private const val HOME = "/home"
private const val COUNTER = "/counter"

enum class RouterScreen(
    routeFormat: String,
    override val annotations: Set<RouteAnnotation> = emptySet(),
) : Route {
    Home(HOME),
    Counter(COUNTER)
    ;

    override val matcher: RouteMatcher = RouteMatcher.create(routeFormat)
}
