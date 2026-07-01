package com.ballast.sharedui.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.ballast.shoppe.feature.router.RouterScreen
import com.ballast.shoppe.feature.router.RouterViewModel
import com.copperleaf.ballast.navigation.routing.Backstack
import com.copperleaf.ballast.navigation.routing.RouterContract
import com.copperleaf.ballast.navigation.routing.renderCurrentDestination
import com.copperleaf.ballast.navigation.vm.Router

@Composable
internal fun RouterContent() {
    val coroutineScope = rememberCoroutineScope()
    val router: Router<RouterScreen> =
        remember(coroutineScope) {
            RouterViewModel(
                viewModelScope = coroutineScope,
                initialRoute = RouterScreen.Home,
            )
        }
    val routerState: Backstack<RouterScreen> by router.observeStates().collectAsState()

    routerState.renderCurrentDestination(
        route = { routerScreen: RouterScreen ->
            when (routerScreen) {
                RouterScreen.Home -> HomeContent(
                    onGoToCounter = {
                        router.trySend(RouterContract.Inputs.GoToDestination(RouterScreen.Counter.matcher.routeFormat))
                    }
                )

                RouterScreen.Counter -> CounterContent(
                    onGoBack = { router.trySend(RouterContract.Inputs.GoBack()) },
                )
            }
        },
        notFound = { },
    )
}
