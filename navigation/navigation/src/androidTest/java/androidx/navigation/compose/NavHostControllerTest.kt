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

package androidx.navigation.compose

import androidx.annotation.IdRes
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ContextAmbient
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestinationBuilder
import androidx.navigation.NavGraphBuilder
import androidx.navigation.createGraph
import androidx.navigation.get
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.ui.test.createComposeRule
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@LargeTest
@RunWith(AndroidJUnit4::class)
class NavHostControllerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testCurrentBackStackEntrySetGraph() {
        var currentBackStackEntry: State<NavBackStackEntry?> = mutableStateOf(null)
        composeTestRule.setContent {
            val navController = TestNavHostController(ContextAmbient.current)

            navController.graph = navController.createGraph(startDestination = FIRST_DESTINATION) {
                test(FIRST_DESTINATION)
            }

            currentBackStackEntry = navController.currentBackStackEntryAsState()
        }

        assertWithMessage("the currentBackStackEntry should be set with the graph")
            .that(currentBackStackEntry.value?.destination?.id)
            .isEqualTo(FIRST_DESTINATION)
    }

    @Test
    fun testCurrentBackStackEntryNavigate() {
        var currentBackStackEntry: State<NavBackStackEntry?> = mutableStateOf(null)
        lateinit var navController: NavController
        composeTestRule.setContent {
            navController = TestNavHostController(ContextAmbient.current)

            navController.graph = navController.createGraph(startDestination = FIRST_DESTINATION) {
                test(FIRST_DESTINATION)
                test(SECOND_DESTINATION)
            }

            currentBackStackEntry = navController.currentBackStackEntryAsState()
        }

        assertWithMessage("the currentBackStackEntry should be set with the graph")
            .that(currentBackStackEntry.value?.destination?.id)
            .isEqualTo(FIRST_DESTINATION)

        composeTestRule.runOnUiThread {
            navController.navigate(SECOND_DESTINATION)
        }

        assertWithMessage("the currentBackStackEntry should be after navigate")
            .that(currentBackStackEntry.value?.destination?.id)
            .isEqualTo(SECOND_DESTINATION)
    }

    @Test
    fun testCurrentBackStackEntryPop() {
        var currentBackStackEntry: State<NavBackStackEntry?> = mutableStateOf(null)
        lateinit var navController: TestNavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(ContextAmbient.current)

            navController.graph = navController.createGraph(startDestination = FIRST_DESTINATION) {
                test(FIRST_DESTINATION)
                test(SECOND_DESTINATION)
            }

            currentBackStackEntry = navController.currentBackStackEntryAsState()
        }

        composeTestRule.runOnUiThread {
            navController.setCurrentDestination(SECOND_DESTINATION)
            navController.popBackStack()
        }

        assertWithMessage("the currentBackStackEntry should return to first destination after pop")
            .that(currentBackStackEntry.value?.destination?.id)
            .isEqualTo(FIRST_DESTINATION)
    }
}

inline fun NavGraphBuilder.test(
    @IdRes id: Int,
    builder: NavDestinationBuilder<NavDestination>.() -> Unit = { }
) = destination(NavDestinationBuilder<NavDestination>(provider["test"], id).apply(builder))

private const val FIRST_DESTINATION = 1
private const val SECOND_DESTINATION = 2
