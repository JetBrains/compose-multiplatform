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

package androidx.compose.foundation.lazy

import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.TouchSlop
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class LazyNestedScrollingTest {
    private val LazyTag = "LazyTag"

    @get:Rule
    val rule = createComposeRule()

    var expectedDragOffset = Float.MAX_VALUE

    @Before
    fun test() {
        expectedDragOffset = with(rule.density) {
            TouchSlop.toPx() + 20
        }
    }

    @Test
    fun column_nestedScrollingBackwardInitially() {
        val items = (1..3).toList()
        var draggedOffset = 0f
        rule.setContent {
            Box(
                Modifier.draggable(Orientation.Vertical) {
                    draggedOffset += it
                }
            ) {
                LazyColumnFor(
                    items = items,
                    modifier = Modifier.size(100.dp).testTag(LazyTag)
                ) {
                    Spacer(Modifier.size(50.dp).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag(LazyTag)
            .performGesture {
                down(Offset(x = 10f, y = 10f))
                moveBy(Offset(x = 0f, y = 100f))
                up()
            }

        rule.runOnIdle {
            Truth.assertThat(draggedOffset).isEqualTo(100f)
        }
    }

    @Test
    fun column_nestedScrollingBackwardOnceWeScrolledForwardPreviously() {
        val items = (1..3).toList()
        var draggedOffset = 0f
        rule.setContent {
            Box(
                Modifier.draggable(Orientation.Vertical) {
                    draggedOffset += it
                }
            ) {
                LazyColumnFor(
                    items = items,
                    modifier = Modifier.size(100.dp).testTag(LazyTag)
                ) {
                    Spacer(Modifier.size(50.dp).testTag("$it"))
                }
            }
        }

        // scroll forward
        rule.onNodeWithTag(LazyTag)
            .scrollBy(y = 20.dp, density = rule.density)

        // scroll back so we again on 0 position
        // we scroll one extra dp to prevent rounding issues
        rule.onNodeWithTag(LazyTag)
            .scrollBy(y = -(21.dp), density = rule.density)

        rule.onNodeWithTag(LazyTag)
            .performGesture {
                draggedOffset = 0f
                down(Offset(x = 10f, y = 10f))
                moveBy(Offset(x = 0f, y = expectedDragOffset))
                up()
            }

        rule.runOnIdle {
            Truth.assertThat(draggedOffset).isEqualTo(expectedDragOffset)
        }
    }

    @Test
    fun column_nestedScrollingForwardWhenTheFullContentIsInitiallyVisible() {
        val items = (1..2).toList()
        var draggedOffset = 0f
        rule.setContent {
            Box(
                Modifier.draggable(Orientation.Vertical) {
                    draggedOffset += it
                }
            ) {
                LazyColumnFor(
                    items = items,
                    modifier = Modifier.size(100.dp).testTag(LazyTag)
                ) {
                    Spacer(Modifier.size(40.dp).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag(LazyTag)
            .performGesture {
                down(Offset(x = 10f, y = 10f))
                moveBy(Offset(x = 0f, y = -expectedDragOffset))
                up()
            }

        rule.runOnIdle {
            Truth.assertThat(draggedOffset).isEqualTo(-expectedDragOffset)
        }
    }

    @Test
    fun column_nestedScrollingForwardWhenScrolledToTheEnd() {
        val items = (1..3).toList()
        var draggedOffset = 0f
        rule.setContent {
            Box(
                Modifier.draggable(Orientation.Vertical) {
                    draggedOffset += it
                }
            ) {
                LazyColumnFor(
                    items = items,
                    modifier = Modifier.size(100.dp).testTag(LazyTag)
                ) {
                    Spacer(Modifier.size(50.dp).testTag("$it"))
                }
            }
        }

        // scroll forward
        rule.onNodeWithTag(LazyTag)
            .scrollBy(y = 50.dp, density = rule.density)

        rule.onNodeWithTag(LazyTag)
            .performGesture {
                draggedOffset = 0f
                down(Offset(x = 10f, y = 10f))
                moveBy(Offset(x = 0f, y = -expectedDragOffset))
                up()
            }

        rule.runOnIdle {
            Truth.assertThat(draggedOffset).isEqualTo(-expectedDragOffset)
        }
    }

    @Test
    fun row_nestedScrollingBackwardInitially() {
        val items = (1..3).toList()
        var draggedOffset = 0f
        rule.setContent {
            Box(
                Modifier.draggable(Orientation.Horizontal) {
                    draggedOffset += it
                }
            ) {
                LazyRowFor(
                    items = items,
                    modifier = Modifier.size(100.dp).testTag(LazyTag)
                ) {
                    Spacer(Modifier.size(50.dp).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag(LazyTag)
            .performGesture {
                down(Offset(x = 10f, y = 10f))
                moveBy(Offset(x = expectedDragOffset, y = 0f))
                up()
            }

        rule.runOnIdle {
            Truth.assertThat(draggedOffset).isEqualTo(expectedDragOffset)
        }
    }

    @Test
    fun row_nestedScrollingBackwardOnceWeScrolledForwardPreviously() {
        val items = (1..3).toList()
        var draggedOffset = 0f
        rule.setContent {
            Box(
                Modifier.draggable(Orientation.Horizontal) {
                    draggedOffset += it
                }
            ) {
                LazyRowFor(
                    items = items,
                    modifier = Modifier.size(100.dp).testTag(LazyTag)
                ) {
                    Spacer(Modifier.size(50.dp).testTag("$it"))
                }
            }
        }

        // scroll forward
        rule.onNodeWithTag(LazyTag)
            .scrollBy(x = 20.dp, density = rule.density)

        // scroll back so we again on 0 position
        // we scroll one extra dp to prevent rounding issues
        rule.onNodeWithTag(LazyTag)
            .scrollBy(x = -(21.dp), density = rule.density)

        rule.onNodeWithTag(LazyTag)
            .performGesture {
                draggedOffset = 0f
                down(Offset(x = 10f, y = 10f))
                moveBy(Offset(x = expectedDragOffset, y = 0f))
                up()
            }

        rule.runOnIdle {
            Truth.assertThat(draggedOffset).isEqualTo(expectedDragOffset)
        }
    }

    @Test
    fun row_nestedScrollingForwardWhenTheFullContentIsInitiallyVisible() {
        val items = (1..2).toList()
        var draggedOffset = 0f
        rule.setContent {
            Box(
                Modifier.draggable(Orientation.Horizontal) {
                    draggedOffset += it
                }
            ) {
                LazyRowFor(
                    items = items,
                    modifier = Modifier.size(100.dp).testTag(LazyTag)
                ) {
                    Spacer(Modifier.size(40.dp).testTag("$it"))
                }
            }
        }

        rule.onNodeWithTag(LazyTag)
            .performGesture {
                down(Offset(x = 10f, y = 10f))
                moveBy(Offset(x = -expectedDragOffset, y = 0f))
                up()
            }

        rule.runOnIdle {
            Truth.assertThat(draggedOffset).isEqualTo(-expectedDragOffset)
        }
    }

    @Test
    fun row_nestedScrollingForwardWhenScrolledToTheEnd() {
        val items = (1..3).toList()
        var draggedOffset = 0f
        rule.setContent {
            Box(
                Modifier.draggable(Orientation.Horizontal) {
                    draggedOffset += it
                }
            ) {
                LazyRowFor(
                    items = items,
                    modifier = Modifier.size(100.dp).testTag(LazyTag)
                ) {
                    Spacer(Modifier.size(50.dp).testTag("$it"))
                }
            }
        }

        // scroll forward
        rule.onNodeWithTag(LazyTag)
            .scrollBy(x = 50.dp, density = rule.density)

        rule.onNodeWithTag(LazyTag)
            .performGesture {
                draggedOffset = 0f
                down(Offset(x = 10f, y = 10f))
                moveBy(Offset(x = -expectedDragOffset, y = 0f))
                up()
            }

        rule.runOnIdle {
            Truth.assertThat(draggedOffset).isEqualTo(-expectedDragOffset)
        }
    }
}
