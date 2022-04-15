/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.focus

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class FocusSearchNonPlacedItemsTest {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var focusManager: FocusManager
    private val initialFocus: FocusRequester = FocusRequester()

    @Test
    fun moveFocusPrevious_skipsUnplacedItem() {
        // Arrange.
        val (parent, item1, item2, item3) = List(4) { mutableStateOf(false) }
        rule.setContentForTest {
            LayoutHorizontally(parent, unplacedIndices = listOf(1)) {
                FocusableBox(item1, 0, 0, 10, 10)
                FocusableBox(item2, 10, 10, 10, 10)
                FocusableBox(item3, 20, 20, 10, 10, initialFocus)
            }
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(FocusDirection.Previous)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parent.value).isFalse()
            assertThat(item1.value).isTrue()
            assertThat(item2.value).isFalse()
            assertThat(item3.value).isFalse()
        }
    }

    @Test
    fun moveFocusNext_skipsUnplacedItem() {
        // Arrange.
        val (parent, item1, item2, item3) = List(4) { mutableStateOf(false) }
        rule.setContentForTest {
            LayoutHorizontally(parent, unplacedIndices = listOf(1)) {
                FocusableBox(item1, 0, 0, 10, 10, initialFocus)
                FocusableBox(item2, 10, 10, 10, 10)
                FocusableBox(item3, 20, 20, 10, 10)
            }
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(FocusDirection.Next)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parent.value).isFalse()
            assertThat(item1.value).isFalse()
            assertThat(item2.value).isFalse()
            assertThat(item3.value).isTrue()
        }
    }

    @Test
    fun moveFocusLeft_skipsUnplacedItem() {
        // Arrange.
        val (parent, item1, item2, item3) = List(4) { mutableStateOf(false) }
        rule.setContentForTest {
            LayoutHorizontally(parent, unplacedIndices = listOf(1)) {
                FocusableBox(item1, 0, 0, 10, 10)
                FocusableBox(item2, 10, 10, 10, 10)
                FocusableBox(item3, 20, 20, 10, 10, initialFocus)
            }
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(FocusDirection.Left)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parent.value).isFalse()
            assertThat(item1.value).isTrue()
            assertThat(item2.value).isFalse()
            assertThat(item3.value).isFalse()
        }
    }

    @Test
    fun moveFocusRight_skipsUnplacedItem() {
        // Arrange.
        val (parent, item1, item2, item3) = List(4) { mutableStateOf(false) }
        rule.setContentForTest {
            LayoutHorizontally(parent, unplacedIndices = listOf(1)) {
                FocusableBox(item1, 0, 0, 10, 10, initialFocus)
                FocusableBox(item2, 10, 10, 10, 10)
                FocusableBox(item3, 20, 20, 10, 10)
            }
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(FocusDirection.Right)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parent.value).isFalse()
            assertThat(item1.value).isFalse()
            assertThat(item2.value).isFalse()
            assertThat(item3.value).isTrue()
        }
    }

    @Test
    fun moveFocusUp_skipsUnplacedItem() {
        // Arrange.
        val (parent, item1, item2, item3) = List(4) { mutableStateOf(false) }
        rule.setContentForTest {
            LayoutVertically(parent, unplacedIndices = listOf(1)) {
                FocusableBox(item1, 0, 0, 10, 10)
                FocusableBox(item2, 10, 10, 10, 10)
                FocusableBox(item3, 20, 20, 10, 10, initialFocus)
            }
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(FocusDirection.Up)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parent.value).isFalse()
            assertThat(item1.value).isTrue()
            assertThat(item2.value).isFalse()
            assertThat(item3.value).isFalse()
        }
    }

    @Test
    fun moveFocusDown_skipsUnplacedItem() {
        // Arrange.
        val (parent, item1, item2, item3) = List(4) { mutableStateOf(false) }
        rule.setContentForTest {
            LayoutVertically(parent, unplacedIndices = listOf(1)) {
                FocusableBox(item1, 0, 0, 10, 10, initialFocus)
                FocusableBox(item2, 10, 10, 10, 10)
                FocusableBox(item3, 20, 20, 10, 10)
            }
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(FocusDirection.Down)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parent.value).isFalse()
            assertThat(item1.value).isFalse()
            assertThat(item2.value).isFalse()
            assertThat(item3.value).isTrue()
        }
    }

    @Test
    fun moveFocusIn_skipsUnplacedItem() {
        // Arrange.
        val (parent, item1, item2) = List(3) { mutableStateOf(false) }
        rule.setContentForTest {
            LayoutHorizontally(parent, unplacedIndices = listOf(0), initialFocus) {
                FocusableBox(item1, 0, 0, 10, 10)
                FocusableBox(item2, 10, 10, 10, 10)
            }
        }

        // Act.
        rule.runOnIdle {
            @OptIn(ExperimentalComposeUiApi::class)
            focusManager.moveFocus(FocusDirection.In)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parent.value).isFalse()
            assertThat(item1.value).isFalse()
            assertThat(item2.value).isTrue()
        }
    }

    @Test
    fun moveFocusPrevious_skipsFirstUnplacedItem() {
        // Arrange.
        val (parent, item1, item2, item3) = List(4) { mutableStateOf(false) }
        rule.setContentForTest {
            LayoutHorizontally(parent, unplacedIndices = listOf(0)) {
                FocusableBox(item1, 0, 0, 10, 10)
                FocusableBox(item2, 10, 10, 10, 10, initialFocus)
                FocusableBox(item3, 20, 20, 10, 10)
            }
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(FocusDirection.Previous)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parent.value).isTrue()
            assertThat(item1.value).isFalse()
            assertThat(item2.value).isFalse()
            assertThat(item3.value).isFalse()
        }
    }

    @Test
    fun moveFocusNext_skipsLastUnplacedItem() {
        // Arrange.
        val (parent, item1, item2, item3) = List(4) { mutableStateOf(false) }
        rule.setContentForTest {
            LayoutHorizontally(parent, unplacedIndices = listOf(2)) {
                FocusableBox(item1, 0, 0, 10, 10)
                FocusableBox(item2, 10, 10, 10, 10, initialFocus)
                FocusableBox(item3, 20, 20, 10, 10)
            }
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(FocusDirection.Next)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parent.value).isTrue()
            assertThat(item1.value).isFalse()
            assertThat(item2.value).isFalse()
            assertThat(item3.value).isFalse()
        }
    }

    @Test
    fun moveFocusPrevious_skipsMultipleUnplacedItems() {
        // Arrange.
        val (parent, item1, item2, item3, item4) = List(5) { mutableStateOf(false) }
        rule.setContentForTest {
            LayoutHorizontally(parent, unplacedIndices = listOf(1, 2)) {
                FocusableBox(item1, 0, 0, 10, 10)
                FocusableBox(item2, 10, 10, 10, 10)
                FocusableBox(item3, 10, 10, 10, 10)
                FocusableBox(item4, 20, 20, 10, 10, initialFocus)
            }
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(FocusDirection.Previous)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parent.value).isFalse()
            assertThat(item1.value).isTrue()
            assertThat(item2.value).isFalse()
            assertThat(item3.value).isFalse()
            assertThat(item4.value).isFalse()
        }
    }

    @Test
    fun moveFocusNext_skipsMultipleUnplacedItems() {
        // Arrange.
        val (parent, item1, item2, item3, item4) = List(5) { mutableStateOf(false) }
        rule.setContentForTest {
            LayoutHorizontally(parent, unplacedIndices = listOf(1, 2)) {
                FocusableBox(item1, 0, 0, 10, 10, initialFocus)
                FocusableBox(item2, 10, 10, 10, 10)
                FocusableBox(item3, 20, 20, 10, 10)
                FocusableBox(item4, 20, 20, 10, 10)
            }
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(FocusDirection.Next)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parent.value).isFalse()
            assertThat(item1.value).isFalse()
            assertThat(item2.value).isFalse()
            assertThat(item3.value).isFalse()
            assertThat(item4.value).isTrue()
        }
    }

    @Test
    fun moveFocusLeft_skipsMultipleUnplacedItems() {
        // Arrange.
        val (parent, item1, item2, item3, item4) = List(5) { mutableStateOf(false) }
        rule.setContentForTest {
            LayoutHorizontally(parent, unplacedIndices = listOf(1, 2)) {
                FocusableBox(item1, 0, 0, 10, 10)
                FocusableBox(item2, 10, 10, 10, 10)
                FocusableBox(item3, 20, 20, 10, 10)
                FocusableBox(item4, 20, 20, 10, 10, initialFocus)
            }
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(FocusDirection.Left)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parent.value).isFalse()
            assertThat(item1.value).isTrue()
            assertThat(item2.value).isFalse()
            assertThat(item3.value).isFalse()
            assertThat(item4.value).isFalse()
        }
    }

    @Test
    fun moveFocusRight_skipsMultipleUnplacedItems() {
        // Arrange.
        val (parent, item1, item2, item3, item4) = List(5) { mutableStateOf(false) }
        rule.setContentForTest {
            LayoutHorizontally(parent, unplacedIndices = listOf(1, 2)) {
                FocusableBox(item1, 0, 0, 10, 10, initialFocus)
                FocusableBox(item2, 10, 10, 10, 10)
                FocusableBox(item3, 20, 20, 10, 10)
                FocusableBox(item4, 20, 20, 10, 10)
            }
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(FocusDirection.Right)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parent.value).isFalse()
            assertThat(item1.value).isFalse()
            assertThat(item2.value).isFalse()
            assertThat(item3.value).isFalse()
            assertThat(item4.value).isTrue()
        }
    }

    @Test
    fun moveFocusUp_skipsMultipleUnplacedItems() {
        // Arrange.
        val (parent, item1, item2, item3, item4) = List(5) { mutableStateOf(false) }
        rule.setContentForTest {
            LayoutVertically(parent, unplacedIndices = listOf(1, 2)) {
                FocusableBox(item1, 0, 0, 10, 10)
                FocusableBox(item2, 10, 10, 10, 10)
                FocusableBox(item3, 20, 20, 10, 10)
                FocusableBox(item4, 20, 20, 10, 10, initialFocus)
            }
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(FocusDirection.Up)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parent.value).isFalse()
            assertThat(item1.value).isTrue()
            assertThat(item2.value).isFalse()
            assertThat(item3.value).isFalse()
            assertThat(item4.value).isFalse()
        }
    }

    @Test
    fun moveFocusDown_skipsMultipleUnplacedItems() {
        // Arrange.
        val (parent, item1, item2, item3, item4) = List(5) { mutableStateOf(false) }
        rule.setContentForTest {
            LayoutVertically(parent, unplacedIndices = listOf(1, 2)) {
                FocusableBox(item1, 0, 0, 10, 10, initialFocus)
                FocusableBox(item2, 10, 10, 10, 10)
                FocusableBox(item3, 20, 20, 10, 10)
                FocusableBox(item4, 20, 20, 10, 10)
            }
        }

        // Act.
        rule.runOnIdle {
            focusManager.moveFocus(FocusDirection.Down)
        }

        // Assert.
        rule.runOnIdle {
            assertThat(parent.value).isFalse()
            assertThat(item1.value).isFalse()
            assertThat(item2.value).isFalse()
            assertThat(item3.value).isFalse()
            assertThat(item4.value).isTrue()
        }
    }

    @Composable
    internal fun FocusableBox(
        isFocused: MutableState<Boolean>,
        x: Dp,
        y: Dp,
        width: Dp,
        height: Dp,
        focusRequester: FocusRequester? = null,
        content: @Composable BoxScope.() -> Unit = {}
    ) {
        Box(
            modifier = Modifier
                .offset(x, y)
                .size(width, height)
                .focusRequester(focusRequester ?: remember { FocusRequester() })
                .onFocusChanged { isFocused.value = it.isFocused }
                .focusTarget(),
            content = content
        )
    }

    @Composable
    fun LayoutHorizontally(
        isFocused: MutableState<Boolean>,
        unplacedIndices: List<Int>,
        focusRequester: FocusRequester? = null,
        content: @Composable () -> Unit,
    ) {
        Layout(
            content = content,
            modifier = Modifier
                .focusRequester(focusRequester ?: remember { FocusRequester() })
                .onFocusChanged { isFocused.value = it.isFocused }
                .focusTarget()
        ) { measurables, constraints ->
            var width = 0
            var height = 0
            val placeables = measurables.map {
                it.measure(constraints).run {
                    val offset = IntOffset(width, height)
                    width += this.width
                    height = maxOf(height, this.height)
                    Pair(this, offset)
                }
            }

            layout(width, height) {
                placeables.forEachIndexed { index, placeable ->
                    if (!unplacedIndices.contains(index)) {
                        placeable.first.placeRelative(placeable.second)
                    }
                }
            }
        }
    }

    @Composable
    fun LayoutVertically(
        isFocused: MutableState<Boolean>,
        unplacedIndices: List<Int>,
        focusRequester: FocusRequester? = null,
        content: @Composable () -> Unit
    ) {
        Layout(
            content = content,
            modifier = Modifier
                .focusRequester(focusRequester ?: remember { FocusRequester() })
                .onFocusChanged { isFocused.value = it.isFocused }
                .focusTarget()
        ) { measurables, constraints ->
            var width = 0
            var height = 0
            val placeables = measurables.map {
                it.measure(constraints).run {
                    val offset = IntOffset(width, height)
                    width = maxOf(width, this.width)
                    height += this.height
                    Pair(this, offset)
                }
            }

            layout(width, height) {
                placeables.forEachIndexed { index, placeable ->
                    if (!unplacedIndices.contains(index)) {
                        placeable.first.placeRelative(placeable.second)
                    }
                }
            }
        }
    }

    private fun ComposeContentTestRule.setContentForTest(composable: @Composable () -> Unit) {
        setContent {
            focusManager = LocalFocusManager.current
            composable()
        }
        rule.runOnIdle { initialFocus.requestFocus() }
    }
}