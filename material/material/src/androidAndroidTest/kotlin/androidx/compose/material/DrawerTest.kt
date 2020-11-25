/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.material

import android.os.SystemClock.sleep
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Providers
import androidx.compose.runtime.emptyContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AmbientLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.bottomCenter
import androidx.compose.ui.test.centerLeft
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class DrawerTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun modalDrawer_testOffset_whenOpen() {
        rule.setMaterialContent {
            val drawerState = rememberDrawerState(DrawerValue.Open)
            ModalDrawerLayout(
                drawerState = drawerState,
                drawerContent = {
                    Box(Modifier.fillMaxSize().testTag("content"))
                },
                bodyContent = emptyContent()
            )
        }

        rule.onNodeWithTag("content")
            .assertLeftPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun modalDrawer_testOffset_whenClosed() {
        rule.setMaterialContent {
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            ModalDrawerLayout(
                drawerState = drawerState,
                drawerContent = {
                    Box(Modifier.fillMaxSize().testTag("content"))
                },
                bodyContent = emptyContent()
            )
        }

        val width = rule.rootWidth()
        rule.onNodeWithTag("content")
            .assertLeftPositionInRootIsEqualTo(-width)
    }

    @Test
    fun modalDrawer_testEndPadding_whenOpen() {
        rule.setMaterialContent {
            val drawerState = rememberDrawerState(DrawerValue.Open)
            ModalDrawerLayout(
                drawerState = drawerState,
                drawerContent = {
                    Box(Modifier.fillMaxSize().testTag("content"))
                },
                bodyContent = emptyContent()
            )
        }

        rule.onNodeWithTag("content")
            .assertWidthIsEqualTo(rule.rootWidth() - 56.dp)
    }

    @Test
    fun bottomDrawer_testOffset_whenOpen() {
        rule.setMaterialContent {
            val drawerState = rememberBottomDrawerState(BottomDrawerValue.Open)
            BottomDrawerLayout(
                drawerState = drawerState,
                drawerContent = {
                    Box(Modifier.fillMaxSize().testTag("content"))
                },
                bodyContent = emptyContent()
            )
        }

        val width = rule.rootWidth()
        val height = rule.rootHeight()
        val expectedTop = if (width > height) 0.dp else (height / 2)
        rule.onNodeWithTag("content")
            .assertTopPositionInRootIsEqualTo(expectedTop)
    }

    @Test
    fun bottomDrawer_testOffset_whenClosed() {
        rule.setMaterialContent {
            val drawerState = rememberBottomDrawerState(BottomDrawerValue.Closed)
            BottomDrawerLayout(
                drawerState = drawerState,
                drawerContent = {
                    Box(Modifier.fillMaxSize().testTag("content"))
                },
                bodyContent = emptyContent()
            )
        }

        val height = rule.rootHeight()
        rule.onNodeWithTag("content")
            .assertTopPositionInRootIsEqualTo(height)
    }

    @Test
    @LargeTest
    fun modalDrawer_openAndClose() {
        lateinit var drawerState: DrawerState
        rule.setMaterialContent {
            drawerState = rememberDrawerState(DrawerValue.Closed)
            ModalDrawerLayout(
                drawerState = drawerState,
                drawerContent = {
                    Box(Modifier.fillMaxSize().testTag("drawer"))
                },
                bodyContent = emptyContent()
            )
        }

        val width = rule.rootWidth()

        // Drawer should start in closed state
        rule.onNodeWithTag("drawer").assertLeftPositionInRootIsEqualTo(-width)

        // When the drawer state is set to Opened
        rule.runOnIdle {
            drawerState.open()
        }
        // Then the drawer should be opened
        rule.onNodeWithTag("drawer").assertLeftPositionInRootIsEqualTo(0.dp)

        // When the drawer state is set to Closed
        rule.runOnIdle {
            drawerState.close()
        }
        // Then the drawer should be closed
        rule.onNodeWithTag("drawer").assertLeftPositionInRootIsEqualTo(-width)
    }

    @Test
    @LargeTest
    fun modalDrawer_bodyContent_clickable() {
        var drawerClicks = 0
        var bodyClicks = 0
        lateinit var drawerState: DrawerState
        rule.setMaterialContent {
            drawerState = rememberDrawerState(DrawerValue.Closed)
            // emulate click on the screen
            ModalDrawerLayout(
                drawerState = drawerState,
                drawerContent = {
                    Box(Modifier.fillMaxSize().clickable { drawerClicks += 1 })
                },
                bodyContent = {
                    Box(Modifier.testTag("Drawer").fillMaxSize().clickable { bodyClicks += 1 })
                }
            )
        }

        // Click in the middle of the drawer (which is the middle of the body)
        rule.onNodeWithTag("Drawer").performGesture { click() }

        rule.runOnIdle {
            assertThat(drawerClicks).isEqualTo(0)
            assertThat(bodyClicks).isEqualTo(1)

            drawerState.open()
        }
        sleep(100) // TODO(147586311): remove this sleep when opening the drawer triggers a wait

        // Click on the left-center pixel of the drawer
        rule.onNodeWithTag("Drawer").performGesture {
            click(centerLeft)
        }

        rule.runOnIdle {
            assertThat(drawerClicks).isEqualTo(1)
            assertThat(bodyClicks).isEqualTo(1)
        }
    }

    @Test
    @LargeTest
    fun bottomDrawer_openAndClose() {
        lateinit var drawerState: BottomDrawerState
        rule.setMaterialContent {
            drawerState = rememberBottomDrawerState(BottomDrawerValue.Closed)
            BottomDrawerLayout(
                drawerState = drawerState,
                drawerContent = {
                    Box(Modifier.fillMaxSize().testTag("drawer"))
                },
                bodyContent = emptyContent()
            )
        }

        val width = rule.rootWidth()
        val height = rule.rootHeight()
        val topWhenOpened = if (width > height) 0.dp else (height / 2)
        val topWhenClosed = height

        // Drawer should start in closed state
        rule.onNodeWithTag("drawer").assertTopPositionInRootIsEqualTo(topWhenClosed)

        // When the drawer state is set to Opened
        rule.runOnIdle {
            drawerState.open()
        }
        // Then the drawer should be opened
        rule.onNodeWithTag("drawer").assertTopPositionInRootIsEqualTo(topWhenOpened)

        // When the drawer state is set to Closed
        rule.runOnIdle {
            drawerState.close()
        }
        // Then the drawer should be closed
        rule.onNodeWithTag("drawer").assertTopPositionInRootIsEqualTo(topWhenClosed)
    }

    @Test
    fun bottomDrawer_bodyContent_clickable() {
        var drawerClicks = 0
        var bodyClicks = 0
        lateinit var drawerState: BottomDrawerState
        rule.setMaterialContent {
            drawerState = rememberBottomDrawerState(BottomDrawerValue.Closed)
            // emulate click on the screen
            BottomDrawerLayout(
                drawerState = drawerState,
                drawerContent = {
                    Box(Modifier.fillMaxSize().clickable { drawerClicks += 1 })
                },
                bodyContent = {
                    Box(Modifier.testTag("Drawer").fillMaxSize().clickable { bodyClicks += 1 })
                }
            )
        }

        // Click in the middle of the drawer (which is the middle of the body)
        rule.onNodeWithTag("Drawer").performGesture { click() }

        rule.runOnIdle {
            assertThat(drawerClicks).isEqualTo(0)
            assertThat(bodyClicks).isEqualTo(1)
        }

        rule.runOnUiThread {
            drawerState.open()
        }
        sleep(100) // TODO(147586311): remove this sleep when opening the drawer triggers a wait

        // Click on the bottom-center pixel of the drawer
        rule.onNodeWithTag("Drawer").performGesture {
            click(bottomCenter)
        }

        assertThat(drawerClicks).isEqualTo(1)
        assertThat(bodyClicks).isEqualTo(1)
    }

    @Test
    @LargeTest
    fun modalDrawer_openBySwipe() {
        lateinit var drawerState: DrawerState
        rule.setMaterialContent {
            drawerState = rememberDrawerState(DrawerValue.Closed)
            // emulate click on the screen
            Box(Modifier.testTag("Drawer")) {
                ModalDrawerLayout(
                    drawerState = drawerState,
                    drawerContent = {
                        Box(Modifier.fillMaxSize().background(color = Color.Magenta))
                    },
                    bodyContent = {
                        Box(Modifier.fillMaxSize().background(color = Color.Red))
                    }
                )
            }
        }

        rule.onNodeWithTag("Drawer")
            .performGesture { swipeRight() }

        rule.runOnIdle {
            assertThat(drawerState.value).isEqualTo(DrawerValue.Open)
        }

        rule.onNodeWithTag("Drawer")
            .performGesture { swipeLeft() }

        rule.runOnIdle {
            assertThat(drawerState.value).isEqualTo(DrawerValue.Closed)
        }
    }

    @Test
    @LargeTest
    fun modalDrawer_openBySwipe_rtl() {
        lateinit var drawerState: DrawerState
        rule.setMaterialContent {
            drawerState = rememberDrawerState(DrawerValue.Closed)
            // emulate click on the screen
            Providers(AmbientLayoutDirection provides LayoutDirection.Rtl) {
                Box(Modifier.testTag("Drawer")) {
                    ModalDrawerLayout(
                        drawerState = drawerState,
                        drawerContent = {
                            Box(Modifier.fillMaxSize().background(color = Color.Magenta))
                        },
                        bodyContent = {
                            Box(Modifier.fillMaxSize().background(color = Color.Red))
                        }
                    )
                }
            }
        }

        rule.onNodeWithTag("Drawer")
            .performGesture { swipeLeft() }

        rule.runOnIdle {
            assertThat(drawerState.value).isEqualTo(DrawerValue.Open)
        }

        rule.onNodeWithTag("Drawer")
            .performGesture { swipeRight() }

        rule.runOnIdle {
            assertThat(drawerState.value).isEqualTo(DrawerValue.Closed)
        }
    }

    @Test
    @LargeTest
    fun bottomDrawer_openBySwipe() {
        lateinit var drawerState: BottomDrawerState
        rule.setMaterialContent {
            drawerState = rememberBottomDrawerState(BottomDrawerValue.Closed)
            // emulate click on the screen
            Box(Modifier.testTag("Drawer")) {
                BottomDrawerLayout(
                    drawerState = drawerState,
                    drawerContent = {
                        Box(Modifier.fillMaxSize().background(color = Color.Magenta))
                    },
                    bodyContent = {
                        Box(Modifier.fillMaxSize().background(color = Color.Red))
                    }
                )
            }
        }
        val isLandscape = rule.rootWidth() > rule.rootHeight()

        rule.onNodeWithTag("Drawer")
            .performGesture { swipeUp() }

        rule.runOnIdle {
            assertThat(drawerState.value).isEqualTo(
                if (isLandscape) BottomDrawerValue.Open else BottomDrawerValue.Expanded
            )
        }

        rule.onNodeWithTag("Drawer")
            .performGesture { swipeDown() }

        rule.runOnIdle {
            assertThat(drawerState.value).isEqualTo(BottomDrawerValue.Closed)
        }
    }

    @Test
    @LargeTest
    fun modalDrawer_noDismissActionWhenClosed_hasDissmissActionWhenOpen() {
        lateinit var drawerState: DrawerState
        rule.setMaterialContent {
            drawerState = rememberDrawerState(DrawerValue.Closed)
            ModalDrawerLayout(
                drawerState = drawerState,
                drawerContent = {
                    Box(Modifier.fillMaxSize().testTag("drawer"))
                },
                bodyContent = emptyContent()
            )
        }

        // Drawer should start in closed state and have no dismiss action
        rule.onNodeWithTag("drawer", useUnmergedTree = true)
            .onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Dismiss))

        // When the drawer state is set to Opened
        rule.runOnIdle {
            drawerState.open()
        }
        // Then the drawer should be opened and have dismiss action
        rule.onNodeWithTag("drawer", useUnmergedTree = true)
            .onParent()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))

        // When the drawer state is set to Closed using dismiss action
        rule.onNodeWithTag("drawer", useUnmergedTree = true)
            .onParent()
            .performSemanticsAction(SemanticsActions.Dismiss)
        // Then the drawer should be closed and have no dismiss action
        rule.onNodeWithTag("drawer", useUnmergedTree = true)
            .onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Dismiss))
    }

    @Test
    @LargeTest
    fun bottomDrawer_noDismissActionWhenClosed_hasDissmissActionWhenOpen() {
        lateinit var drawerState: BottomDrawerState
        rule.setMaterialContent {
            drawerState = rememberBottomDrawerState(BottomDrawerValue.Closed)
            BottomDrawerLayout(
                drawerState = drawerState,
                drawerContent = {
                    Box(Modifier.fillMaxSize().testTag("drawer"))
                },
                bodyContent = emptyContent()
            )
        }

        // Drawer should start in closed state and have no dismiss action
        rule.onNodeWithTag("drawer", useUnmergedTree = true)
            .onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Dismiss))

        // When the drawer state is set to Opened
        rule.runOnIdle {
            drawerState.open()
        }
        // Then the drawer should be opened and have dismiss action
        rule.onNodeWithTag("drawer", useUnmergedTree = true)
            .onParent()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))

        // When the drawer state is set to Closed using dismiss action
        rule.onNodeWithTag("drawer", useUnmergedTree = true)
            .onParent()
            .performSemanticsAction(SemanticsActions.Dismiss)
        // Then the drawer should be closed and have no dismiss action
        rule.onNodeWithTag("drawer", useUnmergedTree = true)
            .onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Dismiss))
    }
}
