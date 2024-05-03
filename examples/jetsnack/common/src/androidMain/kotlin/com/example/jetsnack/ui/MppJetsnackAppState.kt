package com.example.jetsnack.ui

import android.content.res.Resources
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.navigation.*
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.jetsnack.model.SnackbarManager
import com.example.jetsnack.ui.home.HomeSections
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
actual class MppJetsnackAppState(
    actual val scaffoldState: ScaffoldState,
    actual val snackbarManager: SnackbarManager,
    actual val coroutineScope: CoroutineScope,
    val navController: NavHostController,
    val resources: Resources
) {

    init {
        coroutineScope.launch {
            snackbarManager.messages.collect { currentMessages ->
                if (currentMessages.isNotEmpty()) {
                    val message = currentMessages[0]
                    val text = resources.getText(message.message).toString()

                    // Display the snackbar on the screen. `showSnackbar` is a function
                    // that suspends until the snackbar disappears from the screen
                    scaffoldState.snackbarHostState.showSnackbar(text)
                    // Once the snackbar is gone or dismissed, notify the SnackbarManager
                    snackbarManager.setMessageShown(message.id)
                }
            }
        }
    }

    private val bottomBarRoutes = bottomBarTabs.map { it.route }

    actual val bottomBarTabs: Array<HomeSections>
        get() = HomeSections.values()
    actual val currentRoute: String?
        get() = navController.currentDestination?.route


    @Composable
    actual fun shouldShowBottomBar(): Boolean {
        return navController
            .currentBackStackEntryAsState().value?.destination?.route in bottomBarRoutes
    }

    actual fun navigateToBottomBarRoute(route: String) {
        if (route != currentRoute) {
            navController.navigate(route) {
                launchSingleTop = true
                restoreState = true
                // Pop up backstack to the first destination and save state. This makes going back
                // to the start destination when pressing back in any other bottom tab.
                popUpTo(findStartDestination(navController.graph).id) {
                    saveState = true
                }
            }
        }
    }

    fun navigateToSnackDetail(snackId: Long, from: NavBackStackEntry) {
        // In order to discard duplicated navigation events, we check the Lifecycle
        if (from.lifecycleIsResumed()) {
            navController.navigate("${MainDestinations.SNACK_DETAIL_ROUTE}/$snackId")
        }
    }

    fun upPress() {
        navController.navigateUp()
    }
}

@Suppress("UsePropertyAccessSyntax")
private fun NavBackStackEntry.lifecycleIsResumed() =
    this.getLifecycle().currentState == Lifecycle.State.RESUMED

private val NavGraph.startDestination: NavDestination?
    get() = findNode(startDestinationId)

/**
 * Copied from similar function in NavigationUI.kt
 *
 * https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:navigation/navigation-ui/src/main/java/androidx/navigation/ui/NavigationUI.kt
 */
private tailrec fun findStartDestination(graph: NavDestination): NavDestination {
    return if (graph is NavGraph) findStartDestination(graph.startDestination!!) else graph
}

@Composable
actual fun rememberMppJetsnackAppState(): MppJetsnackAppState {
    val scaffoldState = rememberScaffoldState()
    val navController = rememberNavController()
    val resources = resources()
    val snackbarManager = SnackbarManager
    val coroutineScope = rememberCoroutineScope()

    return remember(scaffoldState, navController, snackbarManager, resources, coroutineScope) {
        MppJetsnackAppState(scaffoldState, snackbarManager, coroutineScope, navController, resources)
    }
}

/**
 * A composable function that returns the [Resources]. It will be recomposed when `Configuration`
 * gets updated.
 */
@Composable
@ReadOnlyComposable
private fun resources(): Resources {
    LocalConfiguration.current
    return LocalContext.current.resources
}