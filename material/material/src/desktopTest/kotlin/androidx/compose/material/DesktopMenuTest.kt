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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.internal.keyEvent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Ignore
import org.junit.Rule
import org.junit.runners.JUnit4
import org.junit.runner.RunWith
import org.junit.Test

@RunWith(JUnit4::class)
class DesktopMenuTest {

    @get:Rule
    val rule = createComposeRule()

    private val windowSize = IntSize(100, 100)
    private val anchorPosition = IntOffset(10, 10)
    private val anchorSize = IntSize(80, 20)

    @Test
    fun menu_positioning_vertical_underAnchor() {
        val popupSize = IntSize(80, 70)

        val position = DesktopDropdownMenuPositionProvider(
            DpOffset.Zero,
            Density(1f)
        ).calculatePosition(
            IntRect(anchorPosition, anchorSize),
            windowSize,
            LayoutDirection.Ltr,
            popupSize
        )

        assertThat(position).isEqualTo(IntOffset(10, 30))
    }

    @Test
    fun menu_positioning_vertical_windowTop() {
        val popupSize = IntSize(80, 100)

        val position = DesktopDropdownMenuPositionProvider(
            DpOffset.Zero,
            Density(1f)
        ).calculatePosition(
            IntRect(anchorPosition, anchorSize),
            windowSize,
            LayoutDirection.Ltr,
            popupSize
        )

        assertThat(position).isEqualTo(IntOffset(10, 0))
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun `pressing ESC button invokes onDismissRequest`() {
        var dismissCount = 0
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                DropdownMenu(true, onDismissRequest = {
                    dismissCount++
                }, modifier = Modifier.testTag("dropDownMenu")) {
                    DropdownMenuItem({}) { Text("item1") }
                }
            }
        }

        rule.onNodeWithTag("dropDownMenu")
            .performKeyPress(keyEvent(Key.Escape, KeyEventType.KeyDown))

        rule.runOnIdle {
            Assert.assertEquals(1, dismissCount)
        }

        rule.onNodeWithTag("dropDownMenu")
            .performKeyPress(keyEvent(Key.Escape, KeyEventType.KeyUp))

        rule.runOnIdle {
            Assert.assertEquals(1, dismissCount)
        }
    }

    @Ignore // TODO: remove ignore when changes from http://r.android.com/2520700 will be merged
    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun `navigate DropDownMenu using arrows`() {
        var item1Clicked = 0
        var item2Clicked = 0
        var item3Clicked = 0

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                DropdownMenu(true, onDismissRequest = {},
                    modifier = Modifier.testTag("dropDownMenu")) {
                    DropdownMenuItem({
                        item1Clicked++
                    }) { Text("item1") }
                    DropdownMenuItem({
                        item2Clicked++
                    }) { Text("item2") }
                    DropdownMenuItem({
                        item3Clicked++
                    }) { Text("item3") }
                }
            }
        }

        fun performKeyDownAndUp(key: Key) {
            rule.onNodeWithTag("dropDownMenu").apply {
                performKeyPress(keyEvent(key, KeyEventType.KeyDown))
                performKeyPress(keyEvent(key, KeyEventType.KeyUp))
            }
        }

        fun assertClicksCount(i1: Int, i2: Int, i3: Int) {
            rule.runOnIdle {
                assertThat(item1Clicked).isEqualTo(i1)
                assertThat(item2Clicked).isEqualTo(i2)
                assertThat(item3Clicked).isEqualTo(i3)
            }
        }

        performKeyDownAndUp(Key.DirectionDown)
        performKeyDownAndUp(Key.Enter)
        assertClicksCount(1, 0, 0)

        performKeyDownAndUp(Key.DirectionUp)
        performKeyDownAndUp(Key.Enter)
        assertClicksCount(1, 0, 1)

        performKeyDownAndUp(Key.DirectionUp)
        performKeyDownAndUp(Key.Enter)
        assertClicksCount(1, 1, 1)

        performKeyDownAndUp(Key.DirectionDown)
        performKeyDownAndUp(Key.Enter)
        assertClicksCount(1, 1, 2)

        performKeyDownAndUp(Key.DirectionDown)
        performKeyDownAndUp(Key.Enter)
        assertClicksCount(2, 1, 2)

        performKeyDownAndUp(Key.DirectionDown)
        performKeyDownAndUp(Key.Enter)
        assertClicksCount(2, 2, 2)
    }

    @OptIn(ExperimentalMaterialApi::class, ExperimentalTestApi::class)
    @Test
    fun `right click opens DropdownMenuState`() {
        val state = DropdownMenuState(DropdownMenuState.Status.Closed)
        rule.setContent {
            Box(
                modifier = Modifier
                    .testTag("box")
                    .size(100.dp, 100.dp)
                    .contextMenuOpenDetector(
                        state = state
                    )
            )
        }

        rule.onNodeWithTag("box").performMouseInput {
            rightClick(Offset(10f, 10f))
        }

        assertThat(state.status == DropdownMenuState.Status.Open(Offset(10f, 10f)))
    }

    @OptIn(ExperimentalMaterialApi::class, ExperimentalTestApi::class)
    @Test
    fun `right doesn't open DropdownMenuState when disabled`() {
        val state = DropdownMenuState(DropdownMenuState.Status.Closed)
        rule.setContent {
            Box(
                modifier = Modifier
                    .testTag("box")
                    .size(100.dp, 100.dp)
                    .contextMenuOpenDetector(
                        state = state,
                        enabled = false
                    )
            )
        }

        rule.onNodeWithTag("box").performMouseInput {
            rightClick(Offset(10f, 10f))
        }

        assertThat(state.status == DropdownMenuState.Status.Closed)
    }
}
