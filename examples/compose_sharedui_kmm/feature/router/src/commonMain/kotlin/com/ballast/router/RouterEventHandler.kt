package com.ballast.shoppe.feature.router

import com.copperleaf.ballast.EventHandler
import com.copperleaf.ballast.EventHandlerScope
import com.copperleaf.ballast.navigation.routing.RouterContract

internal class RouterEventHandler :
    EventHandler<RouterContract.Inputs<RouterScreen>, RouterContract.Events<RouterScreen>, RouterContract.State<RouterScreen>> {
    override suspend fun EventHandlerScope<RouterContract.Inputs<RouterScreen>, RouterContract.Events<RouterScreen>, RouterContract.State<RouterScreen>>.handleEvent(
        event: RouterContract.Events<RouterScreen>,
    ) {
        when {
            event is RouterContract.Events.BackstackEmptied ->
                postInput(RouterContract.Inputs.GoToDestination(RouterScreen.Home.matcher.routeFormat))
        }
    }
}
