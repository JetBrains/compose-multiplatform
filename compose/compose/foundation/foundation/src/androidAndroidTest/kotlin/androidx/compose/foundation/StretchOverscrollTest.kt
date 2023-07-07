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

package androidx.compose.foundation

import android.os.Build
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.WithTouchSlop
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.AnimationDurationScaleRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalFoundationApi::class)
@MediumTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
@RunWith(AndroidJUnit4::class)
class StretchOverscrollTest {
    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val animationScaleRule: AnimationDurationScaleRule =
        AnimationDurationScaleRule.createForAllTests(1f)

    @Test
    fun stretchOverscroll_whenPulled_consumesOppositePreScroll_pullLeft() {
        val state = setStretchOverscrollContent(Orientation.Horizontal)

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            down(center)
            // Stretch by 200
            moveBy(Offset(-200f, 0f))
            // Pull 200 in the opposite direction - because we had 200 pixels of stretch before,
            // this should only relax the existing overscroll, and not dispatch anything to the
            // state
            moveBy(Offset(200f, 0f))
        }

        rule.runOnIdle {
            // All 200 should have been consumed by overscroll that was relaxing the existing
            // stretch
            assertThat(state.scrollPosition).isEqualTo(0f)
        }
    }

    @Test
    fun stretchOverscroll_whenPulledWithSmallDelta_doesNotConsumesOppositePreScroll_pullLeft() {
        val state = setStretchOverscrollContent(Orientation.Horizontal)

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            down(center)
            // Try and stretch by 0.4f
            moveBy(Offset(-0.4f, 0f))
            // Pull 200 in the opposite direction - overscroll should have ignored the 0.4f, and
            // so all 200 should be dispatched to the state with nothing being consumed
            moveBy(Offset(200f, 0f))
        }

        rule.runOnIdle {
            // All 200 should be dispatched directly to the state
            assertThat(state.scrollPosition).isEqualTo(200f)
        }
    }

    @Test
    fun stretchOverscroll_whenPulled_consumesOppositePreScroll_pullTop() {
        val state = setStretchOverscrollContent(Orientation.Vertical)

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            down(center)
            // Stretch by 200
            moveBy(Offset(0f, -200f))
            // Pull 200 in the opposite direction - because we had 200 pixels of stretch before,
            // this should only relax the existing overscroll, and not dispatch anything to the
            // state
            moveBy(Offset(0f, 200f))
        }

        rule.runOnIdle {
            // All 200 should have been consumed by overscroll that was relaxing the existing
            // stretch
            assertThat(state.scrollPosition).isEqualTo(0f)
        }
    }

    @Test
    fun stretchOverscroll_whenPulledWithSmallDelta_doesNotConsumesOppositePreScroll_pullTop() {
        val state = setStretchOverscrollContent(Orientation.Vertical)

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            down(center)
            // Try and stretch by 0.4f
            moveBy(Offset(0f, -0.4f))
            // Pull 200 in the opposite direction - overscroll should have ignored the 0.4f, and
            // so all 200 should be dispatched to the state with nothing being consumed
            moveBy(Offset(0f, 200f))
        }

        rule.runOnIdle {
            // All 200 should be dispatched directly to the state
            assertThat(state.scrollPosition).isEqualTo(200f)
        }
    }

    @Test
    fun stretchOverscroll_whenPulled_consumesOppositePreScroll_pullRight() {
        val state = setStretchOverscrollContent(Orientation.Horizontal)

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            down(center)
            // Stretch by 200 (the max scroll value is 1000)
            moveBy(Offset(1200f, 0f))
            // Pull 200 in the opposite direction - because we had 200 pixels of stretch before,
            // this should only relax the existing overscroll, and not dispatch anything to the
            // state
            moveBy(Offset(-200f, 0f))
        }

        rule.runOnIdle {
            // All -200 should have been consumed by overscroll that was relaxing the existing
            // stretch
            assertThat(state.scrollPosition).isEqualTo(1000f)
        }
    }

    @Test
    fun stretchOverscroll_whenPulledWithSmallDelta_doesNotConsumesOppositePreScroll_pullRight() {
        val state = setStretchOverscrollContent(Orientation.Horizontal)

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            down(center)
            // Try and stretch by 0.4f (the max scroll value is 1000)
            moveBy(Offset(1000.4f, 0f))
            // Pull 200 in the opposite direction - overscroll should have ignored the 0.4f, and
            // so all -200 should be dispatched to the state with nothing being consumed
            moveBy(Offset(-200f, 0f))
        }

        rule.runOnIdle {
            // All -200 should be dispatched directly to the state
            assertThat(state.scrollPosition).isEqualTo(800f)
        }
    }

    @Test
    fun stretchOverscroll_whenPulled_consumesOppositePreScroll_pullBottom() {
        val state = setStretchOverscrollContent(Orientation.Vertical)

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            down(center)
            // Stretch by 200 (the max scroll value is 1000)
            moveBy(Offset(0f, 1200f))
            // Pull 200 in the opposite direction - because we had 200 pixels of stretch before,
            // this should only relax the existing overscroll, and not dispatch anything to the
            // state
            moveBy(Offset(0f, -200f))
        }

        rule.runOnIdle {
            // All -200 should have been consumed by overscroll that was relaxing the existing
            // stretch
            assertThat(state.scrollPosition).isEqualTo(1000f)
        }
    }

    @Test
    fun stretchOverscroll_whenPulledWithSmallDelta_doesNotConsumesOppositePreScroll_pullBottom() {
        val state = setStretchOverscrollContent(Orientation.Vertical)

        rule.onNodeWithTag(OverscrollBox).performTouchInput {
            down(center)
            // Try and stretch by 0.4f (the max scroll value is 1000)
            moveBy(Offset(0f, 1000.4f))
            // Pull 200 in the opposite direction - overscroll should have ignored the 0.4f, and
            // so all -200 should be dispatched to the state with nothing being consumed
            moveBy(Offset(0f, -200f))
        }

        rule.runOnIdle {
            // All -200 should be dispatched directly to the state
            assertThat(state.scrollPosition).isEqualTo(800f)
        }
    }

    private fun setStretchOverscrollContent(orientation: Orientation): TestScrollableState {
        animationScaleRule.setAnimationDurationScale(1f)
        val state = TestScrollableState()
        rule.setContent {
            WithTouchSlop(touchSlop = 0f) {
                val overscroll = ScrollableDefaults.overscrollEffect()
                Box(
                    Modifier
                        .testTag(OverscrollBox)
                        .size(100.dp)
                        .scrollable(
                            state = state,
                            orientation = orientation,
                            overscrollEffect = overscroll
                        )
                        .overscroll(overscroll)
                )
            }
        }
        return state
    }
}

/**
 * Returns a default [ScrollableState] with a [scrollPosition] clamped between 0f and 1000f.
 */
private class TestScrollableState : ScrollableState {
    var scrollPosition by mutableStateOf(0f)
        private set

    // Using ScrollableState here instead of ScrollState as ScrollState will automatically round to
    // an int, and we need to assert floating point values
    private val scrollableState = ScrollableState {
        val newPosition = (scrollPosition + it).coerceIn(0f, 1000f)
        val consumed = newPosition - scrollPosition
        scrollPosition = newPosition
        consumed
    }

    override suspend fun scroll(
        scrollPriority: MutatePriority,
        block: suspend ScrollScope.() -> Unit
    ) = scrollableState.scroll(scrollPriority, block)

    override fun dispatchRawDelta(delta: Float) = scrollableState.dispatchRawDelta(delta)

    override val isScrollInProgress: Boolean
        get() = scrollableState.isScrollInProgress
}

private const val OverscrollBox = "box"