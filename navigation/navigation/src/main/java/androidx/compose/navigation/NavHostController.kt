/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.navigation

import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.onCommit
import androidx.compose.runtime.remember
import androidx.compose.runtime.savedinstancestate.Saver
import androidx.compose.runtime.savedinstancestate.rememberSavedInstanceState
import androidx.compose.ui.platform.ContextAmbient
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navigation

/**
 * Gets the current navigation back stack entry as a [MutableState]. When the given navController
 * changes the back stack due to a [NavController.navigate] or [NavController.popBackStack] this
 * will trigger a recompose and return the top entry on the back stack.
 *
 * @return a mutable state of the current back stack entry
 */
@Composable
public fun NavController.currentBackStackEntryAsState(): State<NavBackStackEntry?> {
    val currentNavBackStackEntry = remember { mutableStateOf(currentBackStackEntry) }
    // setup the onDestinationChangedListener responsible for detecting when the
    // current back stack entry changes
    onCommit(this) {
        val callback = NavController.OnDestinationChangedListener { controller, _, _ ->
            currentNavBackStackEntry.value = controller.currentBackStackEntry
        }
        addOnDestinationChangedListener(callback)
        // remove the navController on dispose (i.e. when the composable is destroyed)
        onDispose {
            removeOnDestinationChangedListener(callback)
        }
    }
    return currentNavBackStackEntry
}

/**
 * Creates a NavHostController that handles the adding of the [ComposeNavigator].
 *
 * @see NavHost
 */
@Composable
public fun rememberNavController(): NavHostController {
    val context = ContextAmbient.current
    return rememberSavedInstanceState(saver = NavControllerSaver(context)) {
        createNavController(context)
    }
}

private fun createNavController(context: Context) =
    NavHostController(context).apply {
        navigatorProvider.addNavigator(ComposeNavigator())
    }

/**
 * Saver to save and restore the NavController across config change and process death.
 */
private fun NavControllerSaver(
    context: Context
): Saver<NavHostController, *> = Saver<NavHostController, Bundle>(
    save = { it.saveState() },
    restore = { createNavController(context).apply { restoreState(it) } }
)

/**
 * Navigate to a destination from the current navigation graph.
 *
 * @param destinationId a id to navigate to
 */
public fun NavHostController.navigate(destinationId: Any) {
    navigate(generateId(destinationId))
}

/**
 * Construct a new [NavGraph]
 *
 * @param id the id to set on the graph
 * @param startDestination an object to identify a destination
 * @param builder the builder used to construct the graph
 */
internal fun NavHostController.createGraph(
    id: Int = 0,
    startDestination: Any,
    builder: NavGraphBuilder.() -> Unit
): NavGraph = navigatorProvider.navigation(id, generateId(startDestination), builder)

/**
 * Used to generate an id from any object
 */
internal fun generateId(anchor: Any) = anchor.hashCode() + initialId
private const val initialId = 0x00010000
