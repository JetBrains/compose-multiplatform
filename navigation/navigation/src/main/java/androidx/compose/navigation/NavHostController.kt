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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.onCommit
import androidx.compose.runtime.remember
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController

/**
 * Gets the current navigation back stack entry as a [MutableState]. When the given navController
 * changes the back stack due to a [NavController.navigate] or [NavController.popBackStack] this
 * will trigger a recompose and return the top entry on the back stack.
 *
 * @return a mutable state of the current back stack entry
 */
@Composable
fun NavController.currentBackStackEntryAsState(): State<NavBackStackEntry?> {
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
