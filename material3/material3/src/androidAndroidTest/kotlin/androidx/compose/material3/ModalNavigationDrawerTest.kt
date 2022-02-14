/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.tokens.NavigationDrawerTokens
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3Api::class)
class ModalNavigationDrawerTest {

    @get:Rule
    val rule = createComposeRule()

    private fun advanceClock() {
        rule.mainClock.advanceTimeBy(100_000L)
    }

    val NavigationDrawerWidth = NavigationDrawerTokens.ContainerWidth

    @Test
    fun navigationDrawer_testOffset_whenOpen() {
        rule.setMaterialContent(lightColorScheme()) {
            val drawerState = rememberDrawerState(DrawerValue.Open)
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    Box(Modifier.fillMaxSize().testTag("content"))
                },
                content = {}
            )
        }

        rule.onNodeWithTag("content")
            .assertLeftPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun navigationDrawer_testOffset_whenClosed() {
        rule.setMaterialContent(lightColorScheme()) {
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    Box(Modifier.fillMaxSize().testTag("content"))
                },
                content = {}
            )
        }

        rule.onNodeWithTag("content")
            .assertLeftPositionInRootIsEqualTo(-NavigationDrawerWidth)
    }

    @Test
    fun navigationDrawer_testWidth_whenOpen() {
        rule.setMaterialContent(lightColorScheme()) {
            val drawerState = rememberDrawerState(DrawerValue.Open)
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    Box(Modifier.fillMaxSize().testTag("content"))
                },
                content = {}
            )
        }

        rule.onNodeWithTag("content")
            .assertWidthIsEqualTo(NavigationDrawerTokens.ContainerWidth)
    }

    @Test
    @SmallTest
    fun navigationDrawer_hasPaneTitle() {
        lateinit var navigationMenu: String
        rule.setMaterialContent(lightColorScheme()) {
            ModalNavigationDrawer(
                drawerState = rememberDrawerState(DrawerValue.Open),
                drawerContent = {
                    Box(Modifier.fillMaxSize().testTag("navigationDrawerTag"))
                },
                content = {}
            )
            navigationMenu = getString(Strings.NavigationMenu)
        }

        rule.onNodeWithTag("navigationDrawerTag", useUnmergedTree = true)
            .onParent()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, navigationMenu))
    }

    @Test
    @LargeTest
    fun navigationDrawer_openAndClose(): Unit = runBlocking(AutoTestFrameClock()) {
        lateinit var drawerState: DrawerState
        rule.setMaterialContent(lightColorScheme()) {
            drawerState = rememberDrawerState(DrawerValue.Closed)
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    Box(Modifier.fillMaxSize().testTag(DrawerTestTag))
                },
                content = {}
            )
        }

        // Drawer should start in closed state
        rule.onNodeWithTag(DrawerTestTag).assertLeftPositionInRootIsEqualTo(-NavigationDrawerWidth)

        // When the drawer state is set to Opened
        drawerState.open()
        // Then the drawer should be opened
        rule.onNodeWithTag(DrawerTestTag).assertLeftPositionInRootIsEqualTo(0.dp)

        // When the drawer state is set to Closed
        drawerState.close()
        // Then the drawer should be closed
        rule.onNodeWithTag(DrawerTestTag).assertLeftPositionInRootIsEqualTo(-NavigationDrawerWidth)
    }

    @Test
    @LargeTest
    fun navigationDrawer_animateTo(): Unit = runBlocking(AutoTestFrameClock()) {
        lateinit var drawerState: DrawerState
        rule.setMaterialContent(lightColorScheme()) {
            drawerState = rememberDrawerState(DrawerValue.Closed)
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    Box(Modifier.fillMaxSize().testTag(DrawerTestTag))
                },
                content = {}
            )
        }

        // Drawer should start in closed state
        rule.onNodeWithTag(DrawerTestTag).assertLeftPositionInRootIsEqualTo(-NavigationDrawerWidth)

        // When the drawer state is set to Opened
        drawerState.animateTo(DrawerValue.Open, TweenSpec())
        // Then the drawer should be opened
        rule.onNodeWithTag(DrawerTestTag).assertLeftPositionInRootIsEqualTo(0.dp)

        // When the drawer state is set to Closed
        drawerState.animateTo(DrawerValue.Closed, TweenSpec())
        // Then the drawer should be closed
        rule.onNodeWithTag(DrawerTestTag).assertLeftPositionInRootIsEqualTo(-NavigationDrawerWidth)
    }

    @Test
    @LargeTest
    fun navigationDrawer_snapTo(): Unit = runBlocking(AutoTestFrameClock()) {
        lateinit var drawerState: DrawerState
        rule.setMaterialContent(lightColorScheme()) {
            drawerState = rememberDrawerState(DrawerValue.Closed)
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    Box(Modifier.fillMaxSize().testTag(DrawerTestTag))
                },
                content = {}
            )
        }

        // Drawer should start in closed state
        rule.onNodeWithTag(DrawerTestTag).assertLeftPositionInRootIsEqualTo(-NavigationDrawerWidth)

        // When the drawer state is set to Opened
        drawerState.snapTo(DrawerValue.Open)
        // Then the drawer should be opened
        rule.onNodeWithTag(DrawerTestTag).assertLeftPositionInRootIsEqualTo(0.dp)

        // When the drawer state is set to Closed
        drawerState.snapTo(DrawerValue.Closed)
        // Then the drawer should be closed
        rule.onNodeWithTag(DrawerTestTag).assertLeftPositionInRootIsEqualTo(-NavigationDrawerWidth)
    }

    @Test
    @LargeTest
    fun navigationDrawer_currentValue(): Unit = runBlocking(AutoTestFrameClock()) {
        lateinit var drawerState: DrawerState
        rule.setMaterialContent(lightColorScheme()) {
            drawerState = rememberDrawerState(DrawerValue.Closed)
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    Box(Modifier.fillMaxSize().testTag(DrawerTestTag))
                },
                content = {}
            )
        }

        // Drawer should start in closed state
        assertThat(drawerState.currentValue).isEqualTo(DrawerValue.Closed)

        // When the drawer state is set to Opened
        drawerState.snapTo(DrawerValue.Open)
        // Then the drawer should be opened
        assertThat(drawerState.currentValue).isEqualTo(DrawerValue.Open)

        // When the drawer state is set to Closed
        drawerState.snapTo(DrawerValue.Closed)
        // Then the drawer should be closed
        assertThat(drawerState.currentValue).isEqualTo(DrawerValue.Closed)
    }

    @Test
    @LargeTest
    fun navigationDrawer_bodyContent_clickable(): Unit = runBlocking(AutoTestFrameClock()) {
        var drawerClicks = 0
        var bodyClicks = 0
        lateinit var drawerState: DrawerState
        rule.setMaterialContent(lightColorScheme()) {
            drawerState = rememberDrawerState(DrawerValue.Closed)
            // emulate click on the screen
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    Box(Modifier.fillMaxSize().clickable { drawerClicks += 1 })
                },
                content = {
                    Box(Modifier.testTag(DrawerTestTag).fillMaxSize().clickable { bodyClicks += 1 })
                }
            )
        }

        // Click in the middle of the drawer (which is the middle of the body)
        rule.onNodeWithTag(DrawerTestTag).performTouchInput { click() }

        rule.runOnIdle {
            assertThat(drawerClicks).isEqualTo(0)
            assertThat(bodyClicks).isEqualTo(1)
        }
        drawerState.open()

        // Click on the left-center pixel of the drawer
        rule.onNodeWithTag(DrawerTestTag).performTouchInput {
            click(centerLeft)
        }

        rule.runOnIdle {
            assertThat(drawerClicks).isEqualTo(1)
            assertThat(bodyClicks).isEqualTo(1)
        }
    }

    @Test
    @LargeTest
    fun navigationDrawer_drawerContent_doesntPropagateClicksWhenOpen(): Unit = runBlocking(
        AutoTestFrameClock()
    ) {
        var bodyClicks = 0
        lateinit var drawerState: DrawerState
        rule.setMaterialContent(lightColorScheme()) {
            drawerState = rememberDrawerState(DrawerValue.Closed)
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    Box(Modifier.fillMaxSize().testTag(DrawerTestTag))
                },
                content = {
                    Box(Modifier.fillMaxSize().clickable { bodyClicks += 1 })
                }
            )
        }

        // Click in the middle of the drawer
        rule.onNodeWithTag(DrawerTestTag).performClick()

        rule.runOnIdle {
            assertThat(bodyClicks).isEqualTo(1)
        }
        drawerState.open()

        // Click on the left-center pixel of the drawer
        rule.onNodeWithTag(DrawerTestTag).performTouchInput {
            click(centerLeft)
        }

        rule.runOnIdle {
            assertThat(bodyClicks).isEqualTo(1)
        }
    }

    @Test
    @LargeTest
    fun navigationDrawer_openBySwipe() {
        lateinit var drawerState: DrawerState
        rule.setMaterialContent(lightColorScheme()) {
            drawerState = rememberDrawerState(DrawerValue.Closed)
            Box(Modifier.testTag(DrawerTestTag)) {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        Box(Modifier.fillMaxSize().background(color = Color.Magenta))
                    },
                    content = {
                        Box(Modifier.fillMaxSize().background(color = Color.Red))
                    }
                )
            }
        }

        rule.onNodeWithTag(DrawerTestTag)
            .performTouchInput { swipeRight() }

        rule.runOnIdle {
            assertThat(drawerState.currentValue).isEqualTo(DrawerValue.Open)
        }

        rule.onNodeWithTag(DrawerTestTag)
            .performTouchInput { swipeLeft() }

        rule.runOnIdle {
            assertThat(drawerState.currentValue).isEqualTo(DrawerValue.Closed)
        }
    }

    @Test
    @LargeTest
    fun navigationDrawer_confirmStateChangeRespect() {
        lateinit var drawerState: DrawerState
        rule.setMaterialContent(lightColorScheme()) {
            drawerState = rememberDrawerState(
                DrawerValue.Open,
                confirmStateChange = {
                    it != DrawerValue.Closed
                }
            )
            Box(Modifier.testTag(DrawerTestTag)) {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        Box(
                            Modifier.fillMaxSize()
                                .testTag("content")
                                .background(color = Color.Magenta)
                        )
                    },
                    content = {
                        Box(Modifier.fillMaxSize().background(color = Color.Red))
                    }
                )
            }
        }

        rule.onNodeWithTag(DrawerTestTag)
            .performTouchInput { swipeLeft() }

        // still open
        rule.runOnIdle {
            assertThat(drawerState.currentValue).isEqualTo(DrawerValue.Open)
        }

        rule.onNodeWithTag("content", useUnmergedTree = true)
            .onParent()
            .performSemanticsAction(SemanticsActions.Dismiss)

        rule.runOnIdle {
            assertThat(drawerState.currentValue).isEqualTo(DrawerValue.Open)
        }
    }

    @Test
    @LargeTest
    fun navigationDrawer_openBySwipe_rtl() {
        lateinit var drawerState: DrawerState
        rule.setMaterialContent(lightColorScheme()) {
            drawerState = rememberDrawerState(DrawerValue.Closed)
            // emulate click on the screen
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(Modifier.testTag(DrawerTestTag)) {
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            Box(Modifier.fillMaxSize().background(color = Color.Magenta))
                        },
                        content = {
                            Box(Modifier.fillMaxSize().background(color = Color.Red))
                        }
                    )
                }
            }
        }

        rule.onNodeWithTag(DrawerTestTag)
            .performTouchInput { swipeLeft() }

        rule.runOnIdle {
            assertThat(drawerState.currentValue).isEqualTo(DrawerValue.Open)
        }

        rule.onNodeWithTag(DrawerTestTag)
            .performTouchInput { swipeRight() }

        rule.runOnIdle {
            assertThat(drawerState.currentValue).isEqualTo(DrawerValue.Closed)
        }
    }

    @Test
    @LargeTest
    fun navigationDrawer_noDismissActionWhenClosed_hasDissmissActionWhenOpen(): Unit = runBlocking(
        AutoTestFrameClock()
    ) {
        lateinit var drawerState: DrawerState
        rule.setMaterialContent(lightColorScheme()) {
            drawerState = rememberDrawerState(DrawerValue.Closed)
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    Box(Modifier.fillMaxSize().testTag(DrawerTestTag))
                },
                content = {}
            )
        }

        // Drawer should start in closed state and have no dismiss action
        rule.onNodeWithTag(DrawerTestTag, useUnmergedTree = true)
            .onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Dismiss))

        // When the drawer state is set to Opened
        drawerState.open()
        // Then the drawer should be opened and have dismiss action
        rule.onNodeWithTag(DrawerTestTag, useUnmergedTree = true)
            .onParent()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss))

        // When the drawer state is set to Closed using dismiss action
        rule.onNodeWithTag(DrawerTestTag, useUnmergedTree = true)
            .onParent()
            .performSemanticsAction(SemanticsActions.Dismiss)
        // Then the drawer should be closed and have no dismiss action
        rule.onNodeWithTag(DrawerTestTag, useUnmergedTree = true)
            .onParent()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Dismiss))
    }

    @Test
    fun navigationDrawer_scrimNode_reportToSemanticsWhenOpen_notReportToSemanticsWhenClosed() {
        val topTag = "navigationDrawer"
        lateinit var closeDrawer: String
        rule.setMaterialContent(lightColorScheme()) {
            ModalNavigationDrawer(
                modifier = Modifier.testTag(topTag),
                drawerState = rememberDrawerState(DrawerValue.Open),
                drawerContent = {
                    Box(Modifier.fillMaxSize().testTag(DrawerTestTag))
                },
                content = {
                    Box(Modifier.fillMaxSize().testTag("body"))
                }
            )
            closeDrawer = getString(Strings.CloseDrawer)
        }

        // The drawer should be opened
        rule.onNodeWithTag(DrawerTestTag).assertLeftPositionInRootIsEqualTo(0.dp)

        var topNode = rule.onNodeWithTag(topTag).fetchSemanticsNode()
        assertEquals(3, topNode.children.size)
        rule.onNodeWithContentDescription(closeDrawer)
            .assertHasClickAction()
            .performSemanticsAction(SemanticsActions.OnClick)

        // Then the drawer should be closed
        rule.onNodeWithTag(DrawerTestTag).assertLeftPositionInRootIsEqualTo(-NavigationDrawerWidth)

        topNode = rule.onNodeWithTag(topTag).fetchSemanticsNode()
        assertEquals(2, topNode.children.size)
    }
}

private val DrawerTestTag = "drawer"