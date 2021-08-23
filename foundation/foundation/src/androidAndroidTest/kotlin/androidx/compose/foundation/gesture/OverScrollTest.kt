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

package androidx.compose.foundation.gesture

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.OverScrollController
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.matchers.isZero
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.center
import androidx.compose.ui.test.centerX
import androidx.compose.ui.test.centerY
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class OverScrollTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun overscrollController_scrollable_drag() {
        var consumeOnlyHalf = false
        val controller = TestOverScrollController()
        val scrollableState = ScrollableState { delta ->
            if (consumeOnlyHalf) {
                delta / 2
            } else {
                delta
            }
        }
        var viewConfig: ViewConfiguration? = null
        rule.setContent {
            viewConfig = LocalViewConfiguration.current
            SideEffect {
                // pretend to know the size
                controller
                    .refreshContainerInfo(
                        Size(500f, 500f),
                        true
                    )
            }
            Box {
                Box(
                    Modifier
                        .testTag("box")
                        .size(300.dp)
                        .scrollable(
                            state = scrollableState,
                            orientation = Orientation.Horizontal,
                            overScrollController = controller
                        )
                )
            }
        }

        rule.runOnIdle {
            // we passed isContentScrolls = 1, so initial draw must occur
            Truth.assertThat(controller.drawCallsCount).isEqualTo(1)
        }

        rule.onNodeWithTag("box").assertExists()

        var centerXAxis = 0f
        rule.onNodeWithTag("box").performGesture {
            centerXAxis = centerX
            down(center)
            moveBy(Offset(1000f, 0f))
        }

        rule.runOnIdle {
            Truth.assertThat(controller.releaseCallsCount).isZero()
            Truth.assertThat(controller.lastInitialDragDelta.x).isGreaterThan(0f)
            Truth.assertThat(controller.lastInitialDragDelta.y).isZero()
            // there was only one pointer position coming from the center, let's check
            Truth.assertThat(controller.lastPointerPosition?.x)
                .isEqualTo(centerXAxis + (viewConfig?.touchSlop ?: 0f))
            // consuming all, so overscroll is 0
            Truth.assertThat(controller.lastOverscrollDelta).isEqualTo(Offset.Zero)
        }

        rule.onNodeWithTag("box").performGesture {
            up()
        }

        rule.runOnIdle {
            Truth.assertThat(controller.releaseCallsCount).isEqualTo(1)
            consumeOnlyHalf = true
        }

        rule.onNodeWithTag("box").performGesture {
            down(center)
            moveBy(Offset(1000f, 0f))
        }

        rule.runOnIdle {
            Truth.assertThat(controller.lastInitialDragDelta.x).isGreaterThan(0f)
            Truth.assertThat(controller.lastInitialDragDelta.y).isZero()
            Truth.assertThat(controller.lastOverscrollDelta.x)
                .isEqualTo(controller.lastInitialDragDelta.x / 2)
        }

        rule.onNodeWithTag("box").performGesture {
            up()
        }

        rule.runOnIdle {
            Truth.assertThat(controller.releaseCallsCount).isEqualTo(2)
        }
    }

    @Test
    fun overscrollController_scrollable_fling() {
        var acummulatedScroll = 0f
        val controller = TestOverScrollController()
        val scrollableState = ScrollableState { delta ->
            if (acummulatedScroll > 1000f) {
                0f
            } else {
                acummulatedScroll += delta
                delta
            }
        }
        rule.setContent {
            SideEffect {
                // pretend to know the size
                controller
                    .refreshContainerInfo(
                        Size(500f, 500f),
                        true
                    )
            }
            Box {
                Box(
                    Modifier
                        .testTag("box")
                        .size(300.dp)
                        .scrollable(
                            state = scrollableState,
                            orientation = Orientation.Horizontal,
                            overScrollController = controller
                        )
                )
            }
        }

        rule.runOnIdle {
            // we passed isContentScrolls = 1, so initial draw must occur
            Truth.assertThat(controller.drawCallsCount).isEqualTo(1)
        }

        rule.onNodeWithTag("box").assertExists()

        rule.onNodeWithTag("box").performGesture {
            swipeWithVelocity(
                center,
                Offset(centerX + 10800, centerY),
                endVelocity = 30000f
            )
        }

        rule.runOnIdle {
            Truth.assertThat(controller.lastVelocity.x).isGreaterThan(0f)
        }
    }

    class TestOverScrollController : OverScrollController {
        var releaseCallsCount = 0
        var drawCallsCount = 0
        var lastVelocity = Velocity.Zero
        var lastInitialDragDelta = Offset.Zero
        var lastOverscrollDelta = Offset.Zero
        var lastPointerPosition: Offset? = Offset.Zero
        var containerSize = Size.Zero
        var isContentScrolls = false
        override fun release(): Boolean {
            releaseCallsCount += 1
            return true
        }

        override fun processDragDelta(
            initialDragDelta: Offset,
            overScrollDelta: Offset,
            pointerPosition: Offset?
        ): Boolean {
            lastInitialDragDelta = initialDragDelta
            lastOverscrollDelta = overScrollDelta
            lastPointerPosition = pointerPosition
            return true
        }

        override fun processVelocity(velocity: Velocity): Boolean {
            lastVelocity = velocity
            return true
        }

        override fun refreshContainerInfo(size: Size, isContentScrolls: Boolean) {
            containerSize = size
            this.isContentScrolls = isContentScrolls
        }

        override fun DrawScope.drawOverScroll() {
            drawCallsCount += 1
        }
    }
}