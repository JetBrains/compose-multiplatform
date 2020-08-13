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

import androidx.compose.animation.core.ManualAnimationClock
import androidx.compose.foundation.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.test.filters.MediumTest
import androidx.ui.test.center
import androidx.ui.test.createComposeRule
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.performGesture
import androidx.ui.test.runOnIdle
import androidx.ui.test.swipe
import androidx.ui.test.swipeWithVelocity
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.sign

@MediumTest
@RunWith(JUnit4::class)
@OptIn(ExperimentalMaterialApi::class)
class SwipeableTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    private val swipeableTag = "swipeableTag"

    private lateinit var clock: ManualAnimationClock

    @Before
    fun init() {
        clock = ManualAnimationClock(initTimeMillis = 0L)
    }

    /**
     * Tests that [swipeable] detects horizontal swipes and ignores vertical swipes.
     */
    @Test
    fun swipeable_horizontalSwipe() {
        val state = SwipeableState("A", clock)
        setSwipeableContent {
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(0f) },
                orientation = Orientation.Horizontal
            )
        }

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
        }

        swipeRight()
        advanceClock()

        runOnIdle {
            assertThat(state.value).isEqualTo("B")
        }

        swipeLeft()
        advanceClock()

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
        }

        swipeDown()
        advanceClock()

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
        }

        swipeUp()
        advanceClock()

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
        }
    }

    /**
     * Tests that [swipeable] detects vertical swipes and ignores horizontal swipes.
     */
    @Test
    fun swipeable_verticalSwipe() {
        val state = SwipeableState("A", clock)
        setSwipeableContent {
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(0f) },
                orientation = Orientation.Vertical
            )
        }

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
        }

        swipeDown()
        advanceClock()

        runOnIdle {
            assertThat(state.value).isEqualTo("B")
        }

        swipeUp()
        advanceClock()

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
        }

        swipeRight()
        advanceClock()

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
        }

        swipeLeft()
        advanceClock()

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
        }
    }

    /**
     * Tests that [swipeable] ignores horizontal swipes.
     */
    @Test
    fun swipeable_disabled_horizontal() {
        val state = SwipeableState("A", clock)
        setSwipeableContent {
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(0f) },
                orientation = Orientation.Horizontal,
                enabled = false
            )
        }

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
        }

        swipeRight()
        advanceClock()

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
        }

        swipeLeft()
        advanceClock()

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
        }
    }

    /**
     * Tests that [swipeable] ignores vertical swipes.
     */
    @Test
    fun swipeable_disabled_vertical() {
        val state = SwipeableState("A", clock)
        setSwipeableContent {
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(0f) },
                orientation = Orientation.Vertical,
                enabled = false
            )
        }

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
        }

        swipeDown()
        advanceClock()

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
        }

        swipeUp()
        advanceClock()

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
        }
    }

    /**
     * Tests that [swipeable] reverses the direction of horizontal swipes.
     */
    @Test
    fun swipeable_reverseDirection_horizontal() {
        val state = SwipeableState("A", clock)
        setSwipeableContent {
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(0f) },
                orientation = Orientation.Horizontal,
                reverseDirection = true
            )
        }

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
        }

        swipeRight()
        advanceClock()

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
        }

        swipeLeft()
        advanceClock()

        runOnIdle {
            assertThat(state.value).isEqualTo("B")
        }

        swipeRight()
        advanceClock()

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
        }
    }

    /**
     * Tests that [swipeable] reverses the direction of vertical swipes.
     */
    @Test
    fun swipeable_reverseDirection_vertical() {
        val state = SwipeableState("A", clock)
        setSwipeableContent {
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(0f) },
                orientation = Orientation.Vertical,
                reverseDirection = true
            )
        }

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
        }

        swipeDown()
        advanceClock()

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
        }

        swipeUp()
        advanceClock()

        runOnIdle {
            assertThat(state.value).isEqualTo("B")
        }

        swipeDown()
        advanceClock()

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
        }
    }

    /**
     * Tests that the state and offset of [swipeable] are updated when swiping.
     */
    @Test
    fun swipeable_updatedWhenSwiping() {
        val state = SwipeableState("A", clock)
        setSwipeableContent {
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(0f) },
                orientation = Orientation.Horizontal
            )
        }

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
            assertThat(state.offset.value).isEqualTo(0f)
        }

        swipeRight()

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
            assertThat(state.offset.value).isNonZero()
            assertThat(state.offset.value).isLessThan(100f)
        }

        advanceClock()

        runOnIdle {
            assertThat(state.value).isEqualTo("B")
            assertThat(state.offset.value).isEqualTo(100f)
        }

        swipeLeft()

        runOnIdle {
            assertThat(state.value).isEqualTo("B")
            assertThat(state.offset.value).isNonZero()
            assertThat(state.offset.value).isLessThan(100f)
        }

        advanceClock()

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
            assertThat(state.offset.value).isEqualTo(0f)
        }
    }

    /**
     * Tests that fixed thresholds work correctly.
     */
    @Test
    fun swipeable_thresholds_fixed_small() {
        val state = SwipeableState("A", clock)
        val offsetDp = with(composeTestRule.density) { 35.toDp() }
        setSwipeableContent {
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FixedThreshold(offsetDp) },
                orientation = Orientation.Horizontal,
                velocityThreshold = Dp.Infinity
            )
        }

        val thresholdAtoB = 35f
        val thresholdBtoA = 65f

        for (i in 0..10) {
            state.snapTo("A")

            swipeRight(offset = 50f + i * 10f)

            val passedThreshold = runOnIdle {
                state.offset.value >= thresholdAtoB
            }

            advanceClock()

            runOnIdle {
                assertThat(state.value).isEqualTo(if (passedThreshold) "B" else "A")
            }
        }

        for (i in 0..10) {
            state.snapTo("B")

            swipeLeft(offset = 50f + i * 10f)

            val passedThreshold = runOnIdle {
                state.offset.value <= thresholdBtoA
            }

            advanceClock()

            runOnIdle {
                assertThat(state.value).isEqualTo(if (passedThreshold) "A" else "B")
            }
        }
    }

    /**
     * Tests that fixed thresholds work correctly.
     */
    @Test
    fun swipeable_thresholds_fixed_large() {
        val state = SwipeableState("A", clock)
        val offsetDp = with(composeTestRule.density) { 65.toDp() }
        setSwipeableContent {
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FixedThreshold(offsetDp) },
                orientation = Orientation.Horizontal,
                velocityThreshold = Dp.Infinity
            )
        }

        val thresholdAtoB = 65f
        val thresholdBtoA = 35f

        for (i in 0..10) {
            state.snapTo("A")

            swipeRight(offset = 50f + i * 10f)

            val passedThreshold = runOnIdle {
                state.offset.value >= thresholdAtoB
            }

            advanceClock()

            runOnIdle {
                assertThat(state.value).isEqualTo(if (passedThreshold) "B" else "A")
            }
        }

        for (i in 0..10) {
            state.snapTo("B")

            swipeLeft(offset = 50f + i * 10f)

            val passedThreshold = runOnIdle {
                state.offset.value <= thresholdBtoA
            }

            advanceClock()

            runOnIdle {
                assertThat(state.value).isEqualTo(if (passedThreshold) "A" else "B")
            }
        }
    }

    /**
     * Tests that fractional thresholds work correctly.
     */
    @Test
    fun swipeable_thresholds_fractional_half() {
        val state = SwipeableState("A", clock)
        setSwipeableContent {
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                orientation = Orientation.Horizontal,
                velocityThreshold = Dp.Infinity
            )
        }

        val thresholdAtoB = 50f
        val thresholdBtoA = 50f

        for (i in 0..10) {
            state.snapTo("A")

            swipeRight(offset = 50f + i * 10f)

            val passedThreshold = runOnIdle {
                state.offset.value >= thresholdAtoB
            }

            advanceClock()

            runOnIdle {
                assertThat(state.value).isEqualTo(if (passedThreshold) "B" else "A")
            }
        }

        for (i in 0..10) {
            state.snapTo("B")

            swipeLeft(offset = 50f + i * 10f)

            val passedThreshold = runOnIdle {
                state.offset.value <= thresholdBtoA
            }

            advanceClock()

            runOnIdle {
                assertThat(state.value).isEqualTo(if (passedThreshold) "A" else "B")
            }
        }
    }

    /**
     * Tests that fractional thresholds work correctly.
     */
    @Test
    fun swipeable_thresholds_fractional_quarter() {
        val state = SwipeableState("A", clock)
        setSwipeableContent {
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(0.25f) },
                orientation = Orientation.Horizontal,
                velocityThreshold = Dp.Infinity
            )
        }

        val thresholdAtoB = 25f
        val thresholdBtoA = 75f

        for (i in 0..10) {
            state.snapTo("A")

            swipeRight(offset = 50f + i * 10f)

            val passedThreshold = runOnIdle {
                state.offset.value >= thresholdAtoB
            }

            advanceClock()

            runOnIdle {
                assertThat(state.value).isEqualTo(if (passedThreshold) "B" else "A")
            }
        }

        for (i in 0..10) {
            state.snapTo("B")

            swipeLeft(offset = 50f + i * 10f)

            val passedThreshold = runOnIdle {
                state.offset.value <= thresholdBtoA
            }

            advanceClock()

            runOnIdle {
                assertThat(state.value).isEqualTo(if (passedThreshold) "A" else "B")
            }
        }
    }

    /**
     * Tests that fractional thresholds work correctly.
     */
    @Test
    fun swipeable_thresholds_fractional_threeQuarters() {
        val state = SwipeableState("A", clock)
        setSwipeableContent {
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(0.75f) },
                orientation = Orientation.Horizontal,
                velocityThreshold = Dp.Infinity
            )
        }

        val thresholdAtoB = 75f
        val thresholdBtoA = 25f

        for (i in 0..10) {
            state.snapTo("A")

            swipeRight(offset = 50f + i * 10f)

            val passedThreshold = runOnIdle {
                state.offset.value >= thresholdAtoB
            }

            advanceClock()

            runOnIdle {
                assertThat(state.value).isEqualTo(if (passedThreshold) "B" else "A")
            }
        }

        for (i in 0..10) {
            state.snapTo("B")

            swipeLeft(offset = 50f + i * 10f)

            val passedThreshold = runOnIdle {
                state.offset.value <= thresholdBtoA
            }

            advanceClock()

            runOnIdle {
                assertThat(state.value).isEqualTo(if (passedThreshold) "A" else "B")
            }
        }
    }

    /**
     * Tests that mixing fixed and fractional thresholds works correctly.
     */
    @Test
    fun swipeable_thresholds_mixed() {
        val state = SwipeableState("A", clock)
        val offsetDp = with(composeTestRule.density) { 35.toDp() }
        setSwipeableContent {
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { from, to ->
                    if (from < to) {
                        FixedThreshold(offsetDp)
                    } else {
                        FractionalThreshold(0.75f)
                    }
                },
                orientation = Orientation.Horizontal,
                velocityThreshold = Dp.Infinity
            )
        }

        val thresholdAtoB = 35f
        val thresholdBtoA = 25f

        for (i in 0..10) {
            state.snapTo("A")

            swipeRight(offset = 50f + i * 10f)

            val passedThreshold = runOnIdle {
                state.offset.value >= thresholdAtoB
            }

            advanceClock()

            runOnIdle {
                assertThat(state.value).isEqualTo(if (passedThreshold) "B" else "A")
            }
        }

        for (i in 0..10) {
            state.snapTo("B")

            swipeLeft(offset = 50f + i * 10f)

            val passedThreshold = runOnIdle {
                state.offset.value <= thresholdBtoA
            }

            advanceClock()

            runOnIdle {
                assertThat(state.value).isEqualTo(if (passedThreshold) "A" else "B")
            }
        }
    }

    /**
     * Tests that a custom implementation of [ThresholdConfig] works correctly.
     */
    @Test
    fun swipeable_thresholds_custom() {
        val state = SwipeableState("A", clock)
        setSwipeableContent {
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ ->
                    object : ThresholdConfig {
                        override fun Density.computeThreshold(
                            fromValue: Float,
                            toValue: Float
                        ): Float {
                            return 40 + 5 * sign(toValue - fromValue)
                        }
                    }
                },
                orientation = Orientation.Horizontal,
                velocityThreshold = Dp.Infinity
            )
        }

        val thresholdAtoB = 45f
        val thresholdBtoA = 35f

        for (i in 0..10) {
            state.snapTo("A")

            swipeRight(offset = 50f + i * 10f)

            val passedThreshold = runOnIdle {
                state.offset.value >= thresholdAtoB
            }

            advanceClock()

            runOnIdle {
                assertThat(state.value).isEqualTo(if (passedThreshold) "B" else "A")
            }
        }

        for (i in 0..10) {
            state.snapTo("B")

            swipeLeft(offset = 50f + i * 10f)

            val passedThreshold = runOnIdle {
                state.offset.value <= thresholdBtoA
            }

            advanceClock()

            runOnIdle {
                assertThat(state.value).isEqualTo(if (passedThreshold) "A" else "B")
            }
        }
    }

    /**
     * Tests that the velocity threshold works correctly.
     */
    @Test
    fun swipeable_velocityThreshold() {
        val state = SwipeableState("A", clock)
        val velocityThresholdDp = with(composeTestRule.density) { 500.toDp() }
        setSwipeableContent {
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(1f) },
                orientation = Orientation.Horizontal,
                velocityThreshold = velocityThresholdDp
            )
        }

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
        }

        swipeRight(velocity = 499f)
        advanceClock()

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
        }

        swipeRight(velocity = 501f)
        advanceClock()

        runOnIdle {
            assertThat(state.value).isEqualTo("B")
        }

        swipeLeft(velocity = 499f)
        advanceClock()

        runOnIdle {
            assertThat(state.value).isEqualTo("B")
        }

        swipeLeft(velocity = 501f)
        advanceClock()

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
        }
    }

    /**
     * Tests that [swipeable] will animate to a neighbouring state, after a high-velocity swipe.
     */
    @Test
    fun swipeable_cannotSkipStatesByFlinging() {
        val state = SwipeableState("A", clock)
        setSwipeableContent {
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B", 200f to "C"),
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                orientation = Orientation.Horizontal
            )
        }

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
        }

        swipeRight(velocity = 1000f)
        advanceClock()

        runOnIdle {
            assertThat(state.value).isNotEqualTo("C")
        }
    }

    /**
     * Tests that the overflow is updated when swiping past the bounds.
     */
    @Test
    fun swipeable_overflow() {
        val state = SwipeableState("A", clock)
        setSwipeableContent {
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A"),
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                orientation = Orientation.Horizontal,
                resistanceFactorAtMin = 0f,
                resistanceFactorAtMax = 0f
            )
        }

        runOnIdle {
            assertThat(state.offset.value).isEqualTo(0f)
            assertThat(state.overflow.value).isEqualTo(0f)
        }

        swipeRight()

        runOnIdle {
            assertThat(state.offset.value).isZero()
            assertThat(state.overflow.value).isGreaterThan(0f)
        }

        advanceClock()

        runOnIdle {
            assertThat(state.offset.value).isEqualTo(0f)
            assertThat(state.overflow.value).isEqualTo(0f)
        }

        swipeLeft()

        runOnIdle {
            assertThat(state.offset.value).isZero()
            assertThat(state.overflow.value).isLessThan(0f)
        }

        advanceClock()

        runOnIdle {
            assertThat(state.offset.value).isEqualTo(0f)
            assertThat(state.overflow.value).isEqualTo(0f)
        }
    }

    /**
     * Tests that resistance is applied correctly when swiping past the min bound.
     */
    @Test
    fun swipeable_resistance_atMinBound() {
        val state = SwipeableState("A", clock)
        setSwipeableContent {
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A"),
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                orientation = Orientation.Horizontal,
                resistanceFactorAtMin = 5f,
                resistanceFactorAtMax = 0f
            )
        }

        val width = with(composeTestRule.density) { rootWidth().toPx() }

        swipeLeft()

        runOnIdle {
            assertThat(state.offset.value).isEqualTo(
                computeResistance(width, 5f, state.overflow.value)
            )
        }

        advanceClock()

        runOnIdle {
            assertThat(state.offset.value).isEqualTo(0f)
        }
    }

    /**
     * Tests that resistance is applied correctly when swiping past the max bound.
     */
    @Test
    fun swipeable_resistance_atMaxBound() {
        val state = SwipeableState("A", clock)
        setSwipeableContent {
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A"),
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                orientation = Orientation.Horizontal,
                resistanceFactorAtMin = 0f,
                resistanceFactorAtMax = 5f
            )
        }

        val width = with(composeTestRule.density) { rootWidth().toPx() }

        swipeRight()

        runOnIdle {
            assertThat(state.offset.value).isEqualTo(
                computeResistance(width, 5f, state.overflow.value)
            )
        }

        advanceClock()

        runOnIdle {
            assertThat(state.offset.value).isEqualTo(0f)
        }
    }

    /**
     * Tests that the target works correctly.
     */
    @Test
    fun swipeable_targetValue() {
        val state = SwipeableState("A", clock)
        setSwipeableContent {
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                orientation = Orientation.Horizontal,
                velocityThreshold = Dp.Infinity
            )
        }

        for (i in 0..10) {
            state.snapTo("A")

            swipeRight(offset = 50f + i * 10f)

            val target = runOnIdle { state.targetValue }

            advanceClock()

            runOnIdle {
                assertThat(state.value).isEqualTo(target)
            }
        }

        for (i in 0..10) {
            state.snapTo("B")

            swipeLeft(offset = 50f + i * 10f)

            val target = runOnIdle { state.targetValue }

            advanceClock()

            runOnIdle {
                assertThat(state.value).isEqualTo(target)
            }
        }
    }

    /**
     * Tests that the progress works correctly.
     */
    @Test
    fun swipeable_progress() {
        val state = SwipeableState("A", clock)
        setSwipeableContent {
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(0f) },
                orientation = Orientation.Horizontal
            )
        }

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
            assertThat(state.progress.from).isEqualTo("A")
            assertThat(state.progress.to).isEqualTo("A")
            assertThat(state.progress.fraction).isEqualTo(1f)
        }

        swipeRight()

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
            assertThat(state.progress.from).isEqualTo("A")
            assertThat(state.progress.to).isEqualTo("B")
            assertThat(state.progress.fraction).isEqualTo(state.offset.value / 100)
        }

        advanceClock()

        runOnIdle {
            assertThat(state.value).isEqualTo("B")
            assertThat(state.progress.from).isEqualTo("B")
            assertThat(state.progress.to).isEqualTo("B")
            assertThat(state.progress.fraction).isEqualTo(1f)
        }

        swipeLeft()

        runOnIdle {
            assertThat(state.value).isEqualTo("B")
            assertThat(state.progress.from).isEqualTo("B")
            assertThat(state.progress.to).isEqualTo("A")
            assertThat(state.progress.fraction).isEqualTo((100 - state.offset.value) / 100)
        }

        advanceClock()

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
            assertThat(state.progress.from).isEqualTo("A")
            assertThat(state.progress.to).isEqualTo("A")
            assertThat(state.progress.fraction).isEqualTo(1f)
        }
    }

    /**
     * Tests that the direction works correctly.
     */
    @Test
    fun swipeable_direction() {
        val state = SwipeableState("A", clock)
        setSwipeableContent {
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(0f) },
                orientation = Orientation.Horizontal
            )
        }

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
            assertThat(state.direction).isEqualTo(0f)
        }

        swipeRight()

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
            assertThat(state.direction).isEqualTo(1f)
        }

        advanceClock()

        runOnIdle {
            assertThat(state.value).isEqualTo("B")
            assertThat(state.direction).isEqualTo(0f)
        }

        swipeLeft()

        runOnIdle {
            assertThat(state.value).isEqualTo("B")
            assertThat(state.direction).isEqualTo(-1f)
        }

        advanceClock()

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
            assertThat(state.direction).isEqualTo(0f)
        }
    }

    /**
     * Tests that the progress works correctly, after a swipe was in the opposite direction.
     */
    @Test
    fun swipeable_progress_multipleSwipes() {
        val state = SwipeableState("A", clock)
        setSwipeableContent {
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(0f) },
                orientation = Orientation.Horizontal
            )
        }

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
            assertThat(state.progress.from).isEqualTo("A")
            assertThat(state.progress.to).isEqualTo("A")
            assertThat(state.progress.fraction).isEqualTo(1f)
        }

        onNodeWithTag(swipeableTag).performGesture {
            swipe(
                start = center,
                end = Offset(x = center.x + 125f, y = center.y)
            )
            swipe(
                start = center,
                end = Offset(x = center.x - 25f, y = center.y)
            )
        }

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
            assertThat(state.progress.from).isEqualTo("A")
            assertThat(state.progress.to).isEqualTo("B")
            assertThat(state.progress.fraction).isEqualTo(state.offset.value / 100)
        }

        advanceClock()

        runOnIdle {
            assertThat(state.value).isEqualTo("B")
            assertThat(state.progress.from).isEqualTo("B")
            assertThat(state.progress.to).isEqualTo("B")
            assertThat(state.progress.fraction).isEqualTo(1f)
        }

        onNodeWithTag(swipeableTag).performGesture {
            swipe(
                start = center,
                end = Offset(x = center.x - 125f, y = center.y)
            )
            swipe(
                start = center,
                end = Offset(x = center.x + 25f, y = center.y)
            )
        }

        runOnIdle {
            assertThat(state.value).isEqualTo("B")
            assertThat(state.progress.from).isEqualTo("B")
            assertThat(state.progress.to).isEqualTo("A")
            assertThat(state.progress.fraction).isEqualTo(1 - state.offset.value / 100)
        }

        advanceClock()

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
            assertThat(state.progress.from).isEqualTo("A")
            assertThat(state.progress.to).isEqualTo("A")
            assertThat(state.progress.fraction).isEqualTo(1f)
        }
    }

    /**
     * Tests that the direction works correctly, after a swipe was in the opposite direction.
     */
    @Test
    fun swipeable_direction_multipleSwipes() {
        val state = SwipeableState("A", clock)
        setSwipeableContent {
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                orientation = Orientation.Horizontal
            )
        }

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
            assertThat(state.direction).isEqualTo(0f)
        }

        onNodeWithTag(swipeableTag).performGesture {
            swipe(
                start = center,
                end = Offset(x = center.x + 125f, y = center.y)
            )
            swipe(
                start = center,
                end = Offset(x = center.x - 25f, y = center.y)
            )
        }

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
            assertThat(state.direction).isEqualTo(1f)
        }

        advanceClock()

        runOnIdle {
            assertThat(state.value).isEqualTo("B")
            assertThat(state.direction).isEqualTo(0f)
        }

        onNodeWithTag(swipeableTag).performGesture {
            swipe(
                start = center,
                end = Offset(x = center.x - 125f, y = center.y)
            )
            swipe(
                start = center,
                end = Offset(x = center.x + 25f, y = center.y)
            )
        }

        runOnIdle {
            assertThat(state.value).isEqualTo("B")
            assertThat(state.direction).isEqualTo(-1f)
        }

        advanceClock()

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
            assertThat(state.direction).isEqualTo(0f)
        }
    }

    /**
     * Tests that 'snapTo' updates the state and offset immediately.
     */
    @Test
    fun swipeable_snapTo() {
        val state = SwipeableState("A", clock)
        setSwipeableContent {
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                orientation = Orientation.Horizontal
            )
        }

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
            assertThat(state.offset.value).isEqualTo(0f)
        }

        state.snapTo("B")

        runOnIdle {
            assertThat(state.value).isEqualTo("B")
            assertThat(state.offset.value).isEqualTo(100f)
        }
    }

    /**
     * Tests that 'animateTo' starts an animation which updates the state and offset.
     */
    @Test
    fun swipeable_animateTo() {
        val state = SwipeableState("A", clock)
        setSwipeableContent {
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                orientation = Orientation.Horizontal
            )
        }

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
            assertThat(state.offset.value).isEqualTo(0f)
        }

        state.animateTo("B")

        runOnIdle {
            assertThat(state.value).isEqualTo("A")
            assertThat(state.offset.value).isEqualTo(0f)
        }

        advanceClock()

        runOnIdle {
            assertThat(state.value).isEqualTo("B")
            assertThat(state.offset.value).isEqualTo(100f)
        }
    }

    /**
     * Tests that the 'onEnd' callback of 'animateTo' is invoked and with the correct end value.
     */
    @Test
    fun swipeable_animateTo_onEnd() {
        val state = SwipeableState("A", clock)
        setSwipeableContent {
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                orientation = Orientation.Horizontal
            )
        }

        var endValue: String? = null
        state.animateTo("B",
            onEnd = { _, value -> endValue = value }
        )
        assertThat(endValue).isNull()
        advanceClock()
        assertThat(endValue).isEqualTo("B")
    }

    private fun swipeRight(
        offset: Float = 100f,
        velocity: Float? = null
    ) = performSwipe(x = offset, velocity = velocity)

    private fun swipeLeft(
        offset: Float = 100f,
        velocity: Float? = null
    ) = performSwipe(x = -offset, velocity = velocity)

    private fun swipeDown(
        offset: Float = 100f,
        velocity: Float? = null
    ) = performSwipe(y = offset, velocity = velocity)

    private fun swipeUp(
        offset: Float = 100f,
        velocity: Float? = null
    ) = performSwipe(y = -offset, velocity = velocity)

    private fun advanceClock() {
        clock.clockTimeMillis += 100000L
    }

    private fun performSwipe(x: Float = 0f, y: Float = 0f, velocity: Float? = null) {
        onNodeWithTag(swipeableTag).performGesture {
            val start = Offset(center.x - x / 2, center.y - y / 2)
            val end = Offset(center.x + x / 2, center.y + y / 2)
            if (velocity == null) swipe(start, end) else swipeWithVelocity(start, end, velocity)
        }
    }

    private fun setSwipeableContent(swipeableFactory: @Composable () -> Modifier) {
        composeTestRule.setMaterialContent {
            Box(modifier = Modifier.fillMaxSize().testTag(swipeableTag).then(swipeableFactory()))
        }
    }
}