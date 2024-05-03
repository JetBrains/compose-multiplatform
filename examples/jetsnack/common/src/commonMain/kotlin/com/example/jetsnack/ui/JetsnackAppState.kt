/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.jetsnack.ui

import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.jetsnack.model.SnackbarManager
import com.example.jetsnack.ui.home.HomeSections
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Destinations used in the [JetsnackApp].
 */
object MainDestinations {
    const val HOME_ROUTE = "home"
    const val SNACK_DETAIL_ROUTE = "snack"
    const val SNACK_ID_KEY = "snackId"
}


@Composable
expect fun rememberMppJetsnackAppState(): MppJetsnackAppState

@Stable
expect class MppJetsnackAppState {

    val scaffoldState: ScaffoldState
    val snackbarManager: SnackbarManager
    val coroutineScope: CoroutineScope
    val bottomBarTabs: Array<HomeSections>
    val currentRoute: String?

    @Composable
    fun shouldShowBottomBar(): Boolean

    fun navigateToBottomBarRoute(route: String)
}

/**
 * Responsible for holding state related to [JetsnackApp] and containing UI-related logic.
 */
@Stable
class JetsnackAppState(
    val scaffoldState: ScaffoldState,
//    val navController: NavHostController,
    private val snackbarManager: SnackbarManager,
//    private val resources: Resources,
    coroutineScope: CoroutineScope
) {
    // Process snackbars coming from SnackbarManager
    init {
        coroutineScope.launch {
            snackbarManager.messages.collect { currentMessages ->
                if (currentMessages.isNotEmpty()) {
                    val message = currentMessages[0]
                    // TODO: implement
                    val text = "TODO: resources.getText(message.messageId)"

                    // Display the snackbar on the screen. `showSnackbar` is a function
                    // that suspends until the snackbar disappears from the screen
                    scaffoldState.snackbarHostState.showSnackbar(text.toString())
                    // Once the snackbar is gone or dismissed, notify the SnackbarManager
                    snackbarManager.setMessageShown(message.id)
                }
            }
        }
    }

    // ----------------------------------------------------------
    // BottomBar state source of truth
    // ----------------------------------------------------------

    val bottomBarTabs = HomeSections.values()
    private val bottomBarRoutes = bottomBarTabs.map { it.route }

    // Reading this attribute will cause recompositions when the bottom bar needs shown, or not.
    // Not all routes need to show the bottom bar.
    val shouldShowBottomBar: Boolean
        @Composable get() = true
//    navController
//            .currentBackStackEntryAsState().value?.destination?.route in bottomBarRoutes

    // ----------------------------------------------------------
    // Navigation state source of truth
    // ----------------------------------------------------------

    val currentRoute: String?
        get() = HomeSections.FEED.route//navController.currentDestination?.route

    fun upPress() {
//        navController.navigateUp()
    }

    fun navigateToBottomBarRoute(route: String) {
//        if (route != currentRoute) {
//            navController.navigate(route) {
//                launchSingleTop = true
//                restoreState = true
//                // Pop up backstack to the first destination and save state. This makes going back
//                // to the start destination when pressing back in any other bottom tab.
//                popUpTo(findStartDestination(navController.graph).id) {
//                    saveState = true
//                }
//            }
//        }
    }

//    fun navigateToSnackDetail(snackId: Long, from: NavBackStackEntry) {
        // In order to discard duplicated navigation events, we check the Lifecycle
//        if (from.lifecycleIsResumed()) {
//            navController.navigate("${MainDestinations.SNACK_DETAIL_ROUTE}/$snackId")
//        }
//    }
}

/**
 * If the lifecycle is not resumed it means this NavBackStackEntry already processed a nav event.
 *
 * This is used to de-duplicate navigation events.
 */
//private fun NavBackStackEntry.lifecycleIsResumed() =
//    this.getLifecycle().currentState == Lifecycle.State.RESUMED
//
//private val NavGraph.startDestination: NavDestination?
//    get() = findNode(startDestinationId)

/**
 * Copied from similar function in NavigationUI.kt
 *
 * https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:navigation/navigation-ui/src/main/java/androidx/navigation/ui/NavigationUI.kt
 */
//private tailrec fun findStartDestination(graph: NavDestination): NavDestination {
//    return if (graph is NavGraph) findStartDestination(graph.startDestination!!) else graph
//}

/**
 * A composable function that returns the [Resources]. It will be recomposed when `Configuration`
 * gets updated.
 */
//@Composable
//@ReadOnlyComposable
//private fun resources(): Resources {
//    LocalConfiguration.current
//    return LocalContext.current.resources
//}
