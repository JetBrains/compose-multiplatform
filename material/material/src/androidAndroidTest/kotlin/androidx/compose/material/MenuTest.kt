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

package androidx.compose.material

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AmbientDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntBounds
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class MenuTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun menu_canBeTriggered() {
        var expanded by mutableStateOf(false)

        rule.setContent {
            DropdownMenu(
                expanded = expanded,
                toggle = {
                    Box(Modifier.size(20.dp).background(color = Color.Blue))
                },
                onDismissRequest = {}
            ) {
                DropdownMenuItem(modifier = Modifier.testTag("MenuContent"), onClick = {}) {
                    Text("Option 1")
                }
            }
        }

        rule.onNodeWithTag("MenuContent").assertDoesNotExist()
        rule.mainClock.autoAdvance = false

        rule.runOnUiThread { expanded = true }
        rule.mainClock.advanceTimeByFrame() // Trigger the popup
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame() // Kick off the animation
        rule.mainClock.advanceTimeBy(InTransitionDuration.toLong())
        rule.onNodeWithTag("MenuContent").assertExists()

        rule.runOnUiThread { expanded = false }
        rule.mainClock.advanceTimeByFrame() // Trigger the popup
        rule.mainClock.advanceTimeByFrame() // Kick off the animation
        rule.mainClock.advanceTimeBy(OutTransitionDuration.toLong())
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithTag("MenuContent").assertDoesNotExist()

        rule.runOnUiThread { expanded = true }
        rule.mainClock.advanceTimeByFrame() // Trigger the popup
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame() // Kick off the animation
        rule.mainClock.advanceTimeBy(InTransitionDuration.toLong())
        rule.onNodeWithTag("MenuContent").assertExists()
    }

    @Test
    fun menu_hasExpectedSize() {
        rule.setContent {
            with(AmbientDensity.current) {
                DropdownMenu(
                    expanded = true,
                    toggle = {
                        Box(Modifier.size(20.toDp()).background(color = Color.Blue))
                    },
                    onDismissRequest = {}
                ) {
                    Box(Modifier.testTag("MenuContent1").preferredSize(70.toDp()))
                    Box(Modifier.testTag("MenuContent2").preferredSize(130.toDp()))
                }
            }
        }

        rule.onNodeWithTag("MenuContent1").assertExists()
        rule.onNodeWithTag("MenuContent2").assertExists()
        val node = rule.onNode(
            isPopup() and hasAnyDescendant(hasTestTag("MenuContent1")) and
                hasAnyDescendant(hasTestTag("MenuContent2"))
        ).assertExists().fetchSemanticsNode()
        with(rule.density) {
            assertThat(node.size.width).isEqualTo(130)
            assertThat(node.size.height).isEqualTo(DropdownMenuVerticalPadding.toIntPx() * 2 + 200)
        }
    }

    @Test
    fun menu_positioning_bottomEnd() {
        val screenWidth = 500
        val screenHeight = 1000
        val density = Density(1f)
        val windowBounds = IntBounds(0, 0, screenWidth, screenHeight)
        val anchorPosition = IntOffset(100, 200)
        val anchorSize = IntSize(10, 20)
        val offsetX = 20
        val offsetY = 40
        val popupSize = IntSize(50, 80)

        val ltrPosition = DropdownMenuPositionProvider(
            DpOffset(offsetX.dp, offsetY.dp),
            density
        ).calculatePosition(
            IntBounds(anchorPosition, anchorSize),
            windowBounds,
            LayoutDirection.Ltr,
            popupSize
        )

        assertThat(ltrPosition.x).isEqualTo(
            anchorPosition.x + offsetX
        )
        assertThat(ltrPosition.y).isEqualTo(
            anchorPosition.y + anchorSize.height + offsetY
        )

        val rtlPosition = DropdownMenuPositionProvider(
            DpOffset(offsetX.dp, offsetY.dp),
            density
        ).calculatePosition(
            IntBounds(anchorPosition, anchorSize),
            windowBounds,
            LayoutDirection.Rtl,
            popupSize
        )

        assertThat(rtlPosition.x).isEqualTo(
            anchorPosition.x + anchorSize.width - offsetX - popupSize.width
        )
        assertThat(rtlPosition.y).isEqualTo(
            anchorPosition.y + anchorSize.height + offsetY
        )
    }

    @Test
    fun menu_positioning_topStart() {
        val screenWidth = 500
        val screenHeight = 1000
        val density = Density(1f)
        val windowBounds = IntBounds(0, 0, screenWidth, screenHeight)
        val anchorPosition = IntOffset(450, 950)
        val anchorPositionRtl = IntOffset(50, 950)
        val anchorSize = IntSize(10, 20)
        val offsetX = 20
        val offsetY = 40
        val popupSize = IntSize(150, 80)

        val ltrPosition = DropdownMenuPositionProvider(
            DpOffset(offsetX.dp, offsetY.dp),
            density
        ).calculatePosition(
            IntBounds(anchorPosition, anchorSize),
            windowBounds,
            LayoutDirection.Ltr,
            popupSize
        )

        assertThat(ltrPosition.x).isEqualTo(
            anchorPosition.x + anchorSize.width - offsetX - popupSize.width
        )
        assertThat(ltrPosition.y).isEqualTo(
            anchorPosition.y - popupSize.height - offsetY
        )

        val rtlPosition = DropdownMenuPositionProvider(
            DpOffset(offsetX.dp, offsetY.dp),
            density
        ).calculatePosition(
            IntBounds(anchorPositionRtl, anchorSize),
            windowBounds,
            LayoutDirection.Rtl,
            popupSize
        )

        assertThat(rtlPosition.x).isEqualTo(
            anchorPositionRtl.x + offsetX
        )
        assertThat(rtlPosition.y).isEqualTo(
            anchorPositionRtl.y - popupSize.height - offsetY
        )
    }

    @Test
    fun menu_positioning_top() {
        val screenWidth = 500
        val screenHeight = 1000
        val density = Density(1f)
        val windowBounds = IntBounds(0, 0, screenWidth, screenHeight)
        val anchorPosition = IntOffset(0, 0)
        val anchorSize = IntSize(50, 20)
        val popupSize = IntSize(150, 500)

        // The min margin above and below the menu, relative to the screen.
        val MenuVerticalMargin = 32.dp
        val verticalMargin = with(density) { MenuVerticalMargin.toIntPx() }

        val position = DropdownMenuPositionProvider(
            DpOffset(0.dp, 0.dp),
            density
        ).calculatePosition(
            IntBounds(anchorPosition, anchorSize),
            windowBounds,
            LayoutDirection.Ltr,
            popupSize
        )

        assertThat(position.y).isEqualTo(
            verticalMargin
        )
    }

    @Test
    fun menu_positioning_callback() {
        val screenWidth = 500
        val screenHeight = 1000
        val density = Density(1f)
        val windowBounds = IntBounds(0, 0, screenWidth, screenHeight)
        val anchorPosition = IntOffset(100, 200)
        val anchorSize = IntSize(10, 20)
        val offsetX = 20
        val offsetY = 40
        val popupSize = IntSize(50, 80)

        var obtainedParentBounds = IntBounds(0, 0, 0, 0)
        var obtainedMenuBounds = IntBounds(0, 0, 0, 0)
        DropdownMenuPositionProvider(
            DpOffset(offsetX.dp, offsetY.dp),
            density
        ) { parentBounds, menuBounds ->
            obtainedParentBounds = parentBounds
            obtainedMenuBounds = menuBounds
        }.calculatePosition(
            IntBounds(anchorPosition, anchorSize),
            windowBounds,
            LayoutDirection.Ltr,
            popupSize
        )

        assertThat(obtainedParentBounds).isEqualTo(IntBounds(anchorPosition, anchorSize))
        assertThat(obtainedMenuBounds).isEqualTo(
            IntBounds(
                anchorPosition.x + offsetX,
                anchorPosition.y + anchorSize.height + offsetY,
                anchorPosition.x + offsetX + popupSize.width,
                anchorPosition.y + anchorSize.height + offsetY + popupSize.height
            )
        )
    }

    @Test
    fun dropdownMenuItem_emphasis() {
        var onSurface = Color.Unspecified
        var enabledContentColor = Color.Unspecified
        var disabledContentColor = Color.Unspecified
        var enabledContentAlpha = 1f
        var disabledContentAlpha = 1f

        rule.setContent {
            onSurface = MaterialTheme.colors.onSurface
            enabledContentAlpha = ContentAlpha.high
            disabledContentAlpha = ContentAlpha.disabled
            DropdownMenu(
                toggle = { Box(Modifier.size(20.dp)) },
                onDismissRequest = {},
                expanded = true
            ) {
                DropdownMenuItem(onClick = {}) {
                    enabledContentColor = AmbientContentColor.current
                        .copy(alpha = AmbientContentAlpha.current)
                }
                DropdownMenuItem(enabled = false, onClick = {}) {
                    disabledContentColor = AmbientContentColor.current
                        .copy(alpha = AmbientContentAlpha.current)
                }
            }
        }

        assertThat(enabledContentColor).isEqualTo(onSurface.copy(alpha = enabledContentAlpha))
        assertThat(disabledContentColor).isEqualTo(onSurface.copy(alpha = disabledContentAlpha))
    }

    @Test
    fun dropdownMenuItem_onClick() {
        var clicked = false
        val onClick: () -> Unit = { clicked = true }

        rule.setContent {
            DropdownMenuItem(
                onClick,
                modifier = Modifier.testTag("MenuItem").clickable(onClick = onClick)
            ) {
                Box(Modifier.size(40.dp))
            }
        }

        rule.onNodeWithTag("MenuItem").performClick()

        rule.runOnIdle {
            assertThat(clicked).isTrue()
        }
    }
}
