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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ContextAmbient
import androidx.navigation.NavDestination
import androidx.navigation.NavDestinationBuilder
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.contains
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.internal.runner.junit4.statement.UiThreadStatement.runOnUiThread
import androidx.ui.test.createComposeRule
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class NavHostTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testSingleDestinationSet() {
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(ContextAmbient.current)

            NavHost(navController, startDestination = "First") {
                test("First")
            }
        }

        assertWithMessage("Destination should be added to the graph")
            .that(generateId("First") in navController.graph)
            .isTrue()
    }

    @Test
    fun testNavigate() {
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(ContextAmbient.current)

            NavHost(navController, startDestination = "First") {
                test("First")
                test("Second")
            }
        }

        assertWithMessage("Destination should be added to the graph")
            .that(generateId("First") in navController.graph)
            .isTrue()

        runOnUiThread {
            navController.navigate("Second")
        }

        assertWithMessage("Second destination should be current")
            .that(navController.currentDestination?.id)
            .isEqualTo(generateId("Second"))
    }

    @Test
    fun testPop() {
        lateinit var navController: TestNavHostController
        composeTestRule.setContent {
            navController = TestNavHostController(ContextAmbient.current)

            NavHost(navController, startDestination = "First") {
                test("First")
                test("Second")
            }
        }

        runOnUiThread {
            navController.setCurrentDestination(generateId("Second"))
            navController.popBackStack()
        }

        assertWithMessage("First destination should be current")
            .that(navController.currentDestination?.id)
            .isEqualTo(generateId("First"))
    }

    @Test
    fun testChangeStartDestination() {
        lateinit var navController: TestNavHostController
        lateinit var state: MutableState<String>
        composeTestRule.setContent {
            state = remember { mutableStateOf("First") }

            navController = TestNavHostController(ContextAmbient.current)

            NavHost(navController, startDestination = state.value) {
                test("First")
                test("Second")
            }
        }

        runOnUiThread {
            state.value = "Second"
        }

        composeTestRule.runOnIdle {
            assertWithMessage("Second destination should be current")
                .that(navController.currentDestination?.id)
                .isEqualTo(generateId("Second"))
        }
    }
}

private inline fun NavGraphBuilder.test(
    @IdRes id: Any,
    builder: NavDestinationBuilder<NavDestination>.() -> Unit = { }
) = test(generateId(id), builder)
