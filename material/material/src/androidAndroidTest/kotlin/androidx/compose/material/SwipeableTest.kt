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

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.sign

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterialApi::class)
class SwipeableTest {

    @get:Rule
    val rule = createComposeRule()

    private val swipeableTag = "swipeableTag"

    @Before
    fun init() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    /**
     * Tests that [swipeable] detects horizontal swipes and ignores vertical swipes.
     */
    @Test
    fun swipeable_horizontalSwipe() {
        lateinit var state: SwipeableState<String>
        setSwipeableContent {
            state = rememberSwipeableState("A")
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(0f) },
                orientation = Orientation.Horizontal
            )
        }

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
        }

        swipeRight()
        advanceClock()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("B")
        }

        swipeLeft()
        advanceClock()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
        }

        swipeDown()
        advanceClock()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
        }

        swipeUp()
        advanceClock()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
        }
    }

    /**
     * Tests that [swipeable] detects vertical swipes and ignores horizontal swipes.
     */
    @Test
    fun swipeable_verticalSwipe() {
        lateinit var state: SwipeableState<String>
        setSwipeableContent {
            state = rememberSwipeableState("A")
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(0f) },
                orientation = Orientation.Vertical
            )
        }

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
        }

        swipeDown()
        advanceClock()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("B")
        }

        swipeUp()
        advanceClock()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
        }

        swipeRight()
        advanceClock()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
        }

        swipeLeft()
        advanceClock()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
        }
    }

    /**
     * Tests that [swipeable] ignores horizontal swipes.
     */
    @Test
    fun swipeable_disabled_horizontal() {
        lateinit var state: SwipeableState<String>
        setSwipeableContent {
            state = rememberSwipeableState("A")
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(0f) },
                orientation = Orientation.Horizontal,
                enabled = false
            )
        }

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
        }

        swipeRight()
        advanceClock()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
        }

        swipeLeft()
        advanceClock()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
        }
    }

    /**
     * Tests that [swipeable] ignores vertical swipes.
     */
    @Test
    fun swipeable_disabled_vertical() {
        lateinit var state: SwipeableState<String>
        setSwipeableContent {
            state = rememberSwipeableState("A")
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(0f) },
                orientation = Orientation.Vertical,
                enabled = false
            )
        }

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
        }

        swipeDown()
        advanceClock()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
        }

        swipeUp()
        advanceClock()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
        }
    }

    /**
     * Tests that [swipeable] reverses the direction of horizontal swipes.
     */
    @Test
    fun swipeable_reverseDirection_horizontal() {
        lateinit var state: SwipeableState<String>
        setSwipeableContent {
            state = rememberSwipeableState("A")
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(0f) },
                orientation = Orientation.Horizontal,
                reverseDirection = true
            )
        }

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
        }

        swipeRight()
        advanceClock()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
        }

        swipeLeft()
        advanceClock()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("B")
        }

        swipeRight()
        advanceClock()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
        }
    }

    /**
     * Tests that [swipeable] reverses the direction of vertical swipes.
     */
    @Test
    fun swipeable_reverseDirection_vertical() {
        lateinit var state: SwipeableState<String>
        setSwipeableContent {
            state = rememberSwipeableState("A")
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(0f) },
                orientation = Orientation.Vertical,
                reverseDirection = true
            )
        }

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
        }

        swipeDown()
        advanceClock()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
        }

        swipeUp()
        advanceClock()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("B")
        }

        swipeDown()
        advanceClock()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
        }
    }

    /**
     * Tests that the state and offset of [swipeable] are updated when swiping.
     */
    @Test
    fun swipeable_updatedWhenSwiping() {
        rule.mainClock.autoAdvance = false
        lateinit var state: SwipeableState<String>
        setSwipeableContent {
            state = rememberSwipeableState("A")
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(0f) },
                orientation = Orientation.Horizontal
            )
        }

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
            assertThat(state.offset.value).isEqualTo(0f)
        }

        swipeRight()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
            assertThat(state.offset.value).isNonZero()
            assertThat(state.offset.value).isLessThan(100f)
        }

        advanceClock()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("B")
            assertThat(state.offset.value).isEqualTo(100f)
        }

        swipeLeft()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("B")
            assertThat(state.offset.value).isNonZero()
            assertThat(state.offset.value).isLessThan(100f)
        }

        advanceClock()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
            assertThat(state.offset.value).isEqualTo(0f)
        }
    }

    /**
     * Tests that fixed thresholds work correctly.
     */
    @Test
    @LargeTest
    fun swipeable_thresholds_fixed_small() = runBlocking(AutoTestFrameClock()) {
        rule.mainClock.autoAdvance = false
        lateinit var state: SwipeableState<String>
        val offsetDp = with(rule.density) { 35.toDp() }
        setSwipeableContent {
            state = rememberSwipeableState("A")
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

            val passedThreshold = rule.runOnIdle {
                state.offset.value >= thresholdAtoB
            }

            advanceClock()

            rule.runOnIdle {
                assertThat(state.currentValue).isEqualTo(if (passedThreshold) "B" else "A")
            }
        }

        for (i in 0..10) {
            state.snapTo("B")

            swipeLeft(offset = 50f + i * 10f)

            val passedThreshold = rule.runOnIdle {
                state.offset.value <= thresholdBtoA
            }

            advanceClock()

            rule.runOnIdle {
                assertThat(state.currentValue).isEqualTo(if (passedThreshold) "A" else "B")
            }
        }
    }

    /**
     * Tests that fixed thresholds work correctly.
     */
    @Test
    @LargeTest
    fun swipeable_thresholds_fixed_large() = runBlocking(AutoTestFrameClock()) {
        lateinit var state: SwipeableState<String>
        val offsetDp = with(rule.density) { 65.toDp() }
        setSwipeableContent {
            state = rememberSwipeableState("A")
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

            val passedThreshold = rule.runOnIdle {
                state.offset.value >= thresholdAtoB
            }

            advanceClock()

            rule.runOnIdle {
                assertThat(state.currentValue).isEqualTo(if (passedThreshold) "B" else "A")
            }
        }

        for (i in 0..10) {
            state.snapTo("B")

            swipeLeft(offset = 50f + i * 10f)

            val passedThreshold = rule.runOnIdle {
                state.offset.value <= thresholdBtoA
            }

            advanceClock()

            rule.runOnIdle {
                assertThat(state.currentValue).isEqualTo(if (passedThreshold) "A" else "B")
            }
        }
    }

    /**
     * Tests that fractional thresholds work correctly.
     */
    @Test
    @LargeTest
    fun swipeable_thresholds_fractional_half() = runBlocking(AutoTestFrameClock()) {
        lateinit var state: SwipeableState<String>
        setSwipeableContent {
            state = rememberSwipeableState("A")
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

            val passedThreshold = rule.runOnIdle {
                state.offset.value >= thresholdAtoB
            }

            advanceClock()

            rule.runOnIdle {
                assertThat(state.currentValue).isEqualTo(if (passedThreshold) "B" else "A")
            }
        }

        for (i in 0..10) {
            state.snapTo("B")

            swipeLeft(offset = 50f + i * 10f)

            val passedThreshold = rule.runOnIdle {
                state.offset.value <= thresholdBtoA
            }

            advanceClock()

            rule.runOnIdle {
                assertThat(state.currentValue).isEqualTo(if (passedThreshold) "A" else "B")
            }
        }
    }

    /**
     * Tests that fractional thresholds work correctly.
     */
    @Test
    @LargeTest
    fun swipeable_thresholds_fractional_quarter() = runBlocking(AutoTestFrameClock()) {
        lateinit var state: SwipeableState<String>
        setSwipeableContent {
            state = rememberSwipeableState("A")
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

            val passedThreshold = rule.runOnIdle {
                state.offset.value >= thresholdAtoB
            }

            advanceClock()

            rule.runOnIdle {
                assertThat(state.currentValue).isEqualTo(if (passedThreshold) "B" else "A")
            }
        }

        for (i in 0..10) {
            state.snapTo("B")

            swipeLeft(offset = 50f + i * 10f)

            val passedThreshold = rule.runOnIdle {
                state.offset.value <= thresholdBtoA
            }

            advanceClock()

            rule.runOnIdle {
                assertThat(state.currentValue).isEqualTo(if (passedThreshold) "A" else "B")
            }
        }
    }

    /**
     * Tests that fractional thresholds work correctly.
     */
    @Test
    @LargeTest
    fun swipeable_thresholds_fractional_threeQuarters() = runBlocking(AutoTestFrameClock()) {
        lateinit var state: SwipeableState<String>
        setSwipeableContent {
            state = rememberSwipeableState("A")
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

            val passedThreshold = rule.runOnIdle {
                state.offset.value >= thresholdAtoB
            }

            advanceClock()

            rule.runOnIdle {
                assertThat(state.currentValue).isEqualTo(if (passedThreshold) "B" else "A")
            }
        }

        for (i in 0..10) {
            state.snapTo("B")

            swipeLeft(offset = 50f + i * 10f)

            val passedThreshold = rule.runOnIdle {
                state.offset.value <= thresholdBtoA
            }

            advanceClock()

            rule.runOnIdle {
                assertThat(state.currentValue).isEqualTo(if (passedThreshold) "A" else "B")
            }
        }
    }

    /**
     * Tests that mixing fixed and fractional thresholds works correctly.
     */
    @Test
    @LargeTest
    fun swipeable_thresholds_mixed() = runBlocking(AutoTestFrameClock()) {
        lateinit var state: SwipeableState<String>
        val offsetDp = with(rule.density) { 35.toDp() }
        setSwipeableContent {
            state = rememberSwipeableState("A")
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

            val passedThreshold = rule.runOnIdle {
                state.offset.value >= thresholdAtoB
            }

            advanceClock()

            rule.runOnIdle {
                assertThat(state.currentValue).isEqualTo(if (passedThreshold) "B" else "A")
            }
        }

        for (i in 0..10) {
            state.snapTo("B")

            swipeLeft(offset = 50f + i * 10f)

            val passedThreshold = rule.runOnIdle {
                state.offset.value <= thresholdBtoA
            }

            advanceClock()

            rule.runOnIdle {
                assertThat(state.currentValue).isEqualTo(if (passedThreshold) "A" else "B")
            }
        }
    }

    /**
     * Tests that a custom implementation of [ThresholdConfig] works correctly.
     */
    @Test
    @LargeTest
    fun swipeable_thresholds_custom() = runBlocking(AutoTestFrameClock()) {
        lateinit var state: SwipeableState<String>
        setSwipeableContent {
            state = rememberSwipeableState("A")
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

            val passedThreshold = rule.runOnIdle {
                state.offset.value >= thresholdAtoB
            }

            advanceClock()

            rule.runOnIdle {
                assertThat(state.currentValue).isEqualTo(if (passedThreshold) "B" else "A")
            }
        }

        for (i in 0..10) {
            state.snapTo("B")

            swipeLeft(offset = 50f + i * 10f)

            val passedThreshold = rule.runOnIdle {
                state.offset.value <= thresholdBtoA
            }

            advanceClock()

            rule.runOnIdle {
                assertThat(state.currentValue).isEqualTo(if (passedThreshold) "A" else "B")
            }
        }
    }

    /**
     * Tests that the velocity threshold works correctly.
     */
    @Test
    fun swipeable_velocityThreshold() {
        lateinit var state: SwipeableState<String>
        val velocityThresholdDp = with(rule.density) { 500.toDp() }
        setSwipeableContent {
            state = rememberSwipeableState("A")
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(1f) },
                orientation = Orientation.Horizontal,
                velocityThreshold = velocityThresholdDp
            )
        }

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
        }

        swipeRight(velocity = 499f)
        advanceClock()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
        }

        swipeRight(velocity = 501f)
        advanceClock()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("B")
        }

        swipeLeft(velocity = 499f)
        advanceClock()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("B")
        }

        swipeLeft(velocity = 501f)
        advanceClock()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
        }
    }

    /**
     * Tests that [swipeable] will animate to a neighbouring state, after a high-velocity swipe.
     */
    @Test
    fun swipeable_cannotSkipStatesByFlinging() {
        lateinit var state: SwipeableState<String>
        setSwipeableContent {
            state = rememberSwipeableState("A")
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B", 200f to "C"),
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                orientation = Orientation.Horizontal
            )
        }

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
        }

        swipeRight(velocity = 1000f)
        advanceClock()

        rule.runOnIdle {
            assertThat(state.currentValue).isNotEqualTo("C")
        }
    }

    /**
     * Tests that the overflow is updated when swiping past the bounds.
     */
    @Test
    fun swipeable_overflow() {
        rule.mainClock.autoAdvance = false
        lateinit var state: SwipeableState<String>
        setSwipeableContent {
            state = rememberSwipeableState("A")
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A"),
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                orientation = Orientation.Horizontal,
                resistance = null
            )
        }

        rule.runOnIdle {
            assertThat(state.offset.value).isEqualTo(0f)
            assertThat(state.overflow.value).isEqualTo(0f)
        }

        swipeRight()

        rule.runOnIdle {
            assertThat(state.offset.value).isZero()
            assertThat(state.overflow.value).isGreaterThan(0f)
        }

        advanceClock()

        rule.runOnIdle {
            assertThat(state.offset.value).isEqualTo(0f)
            assertThat(state.overflow.value).isEqualTo(0f)
        }

        swipeLeft()

        rule.runOnIdle {
            assertThat(state.offset.value).isZero()
            assertThat(state.overflow.value).isLessThan(0f)
        }

        advanceClock()

        rule.runOnIdle {
            assertThat(state.offset.value).isEqualTo(0f)
            assertThat(state.overflow.value).isEqualTo(0f)
        }
    }

    /**
     * Tests that resistance is applied correctly when swiping past the min bound.
     */
    @Test
    fun swipeable_resistance_atMinBound() {
        lateinit var state: SwipeableState<String>
        val resistance = ResistanceConfig(100f, 5f, 0f)
        setSwipeableContent {
            state = rememberSwipeableState("A")
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A"),
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                orientation = Orientation.Horizontal,
                resistance = resistance
            )
        }

        swipeLeft()

        rule.runOnIdle {
            assertThat(state.offset.value).isEqualTo(
                resistance.computeResistance(state.overflow.value)
            )
        }

        advanceClock()

        rule.runOnIdle {
            assertThat(state.offset.value).isEqualTo(0f)
        }
    }

    /**
     * Tests that resistance is applied correctly when swiping past the max bound.
     */
    @Test
    fun swipeable_resistance_atMaxBound() {
        lateinit var state: SwipeableState<String>
        val resistance = ResistanceConfig(100f, 0f, 5f)
        setSwipeableContent {
            state = rememberSwipeableState("A")
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A"),
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                orientation = Orientation.Horizontal,
                resistance = resistance
            )
        }

        swipeRight()

        rule.runOnIdle {
            assertThat(state.offset.value).isEqualTo(
                resistance.computeResistance(state.overflow.value)
            )
        }

        advanceClock()

        rule.runOnIdle {
            assertThat(state.offset.value).isEqualTo(0f)
        }
    }

    /**
     * Tests that the target works correctly.
     */
    @Test
    @LargeTest
    fun swipeable_targetValue() = runBlocking(AutoTestFrameClock()) {
        lateinit var state: SwipeableState<String>
        setSwipeableContent {
            state = rememberSwipeableState("A")
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

            val target = rule.runOnIdle { state.targetValue }

            advanceClock()

            rule.runOnIdle {
                assertThat(state.currentValue).isEqualTo(target)
            }
        }

        for (i in 0..10) {
            state.snapTo("B")

            swipeLeft(offset = 50f + i * 10f)

            val target = rule.runOnIdle { state.targetValue }

            advanceClock()

            rule.runOnIdle {
                assertThat(state.currentValue).isEqualTo(target)
            }
        }
    }

    /**
     * Tests that the progress works correctly.
     */
    @Test
    fun swipeable_progress() {
        rule.mainClock.autoAdvance = false
        lateinit var state: SwipeableState<String>
        setSwipeableContent {
            state = rememberSwipeableState("A")
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(0f) },
                orientation = Orientation.Horizontal
            )
        }

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
            assertThat(state.progress.from).isEqualTo("A")
            assertThat(state.progress.to).isEqualTo("A")
            assertThat(state.progress.fraction).isEqualTo(1f)
        }

        swipeRight()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
            assertThat(state.progress.from).isEqualTo("A")
            assertThat(state.progress.to).isEqualTo("B")
            assertThat(state.progress.fraction).isEqualTo(state.offset.value / 100)
        }

        advanceClock()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("B")
            assertThat(state.progress.from).isEqualTo("B")
            assertThat(state.progress.to).isEqualTo("B")
            assertThat(state.progress.fraction).isEqualTo(1f)
        }

        swipeLeft()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("B")
            assertThat(state.progress.from).isEqualTo("B")
            assertThat(state.progress.to).isEqualTo("A")
            assertThat(state.progress.fraction).isEqualTo((100 - state.offset.value) / 100)
        }

        advanceClock()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
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
        rule.mainClock.autoAdvance = false
        lateinit var state: SwipeableState<String>
        setSwipeableContent {
            state = rememberSwipeableState("A")
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(0f) },
                orientation = Orientation.Horizontal
            )
        }

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
            assertThat(state.direction).isEqualTo(0f)
        }

        swipeRight()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
            assertThat(state.direction).isEqualTo(1f)
        }

        advanceClock()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("B")
            assertThat(state.direction).isEqualTo(0f)
        }

        swipeLeft()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("B")
            assertThat(state.direction).isEqualTo(-1f)
        }

        advanceClock()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
            assertThat(state.direction).isEqualTo(0f)
        }
    }

    /**
     * Tests that the progress works correctly, after a swipe was in the opposite direction.
     */
    @Test
    fun swipeable_progress_multipleSwipes() {
        rule.mainClock.autoAdvance = false
        lateinit var state: SwipeableState<String>
        var slop = 0f
        setSwipeableContent {
            state = rememberSwipeableState("A")
            slop = LocalViewConfiguration.current.touchSlop
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(0f) },
                orientation = Orientation.Horizontal
            )
        }

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
            assertThat(state.progress.from).isEqualTo("A")
            assertThat(state.progress.to).isEqualTo("A")
            assertThat(state.progress.fraction).isEqualTo(1f)
        }

        rule.onNodeWithTag(swipeableTag).performTouchInput {
            swipe(start = center, end = center + Offset(125f - slop, 0f))
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithTag(swipeableTag).performTouchInput {
            swipe(start = center, end = center - Offset(25f, 0f))
        }

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
            assertThat(state.progress.from).isEqualTo("A")
            assertThat(state.progress.to).isEqualTo("B")
            assertThat(state.progress.fraction).isEqualTo(state.offset.value / 100)
        }

        advanceClock()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("B")
            assertThat(state.progress.from).isEqualTo("B")
            assertThat(state.progress.to).isEqualTo("B")
            assertThat(state.progress.fraction).isEqualTo(1f)
        }

        rule.onNodeWithTag(swipeableTag).performTouchInput {
            swipe(start = center, end = center - Offset(125f - slop, 0f))
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithTag(swipeableTag).performTouchInput {
            swipe(start = center, end = center + Offset(25f, 0f))
        }

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("B")
            assertThat(state.progress.from).isEqualTo("B")
            assertThat(state.progress.to).isEqualTo("A")
            assertThat(state.progress.fraction).isWithin(1e-6f).of(1 - state.offset.value / 100)
        }

        advanceClock()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
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
        rule.mainClock.autoAdvance = false
        lateinit var state: SwipeableState<String>
        var slop = 0f
        setSwipeableContent {
            state = rememberSwipeableState("A")
            slop = LocalViewConfiguration.current.touchSlop
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                orientation = Orientation.Horizontal
            )
        }

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
            assertThat(state.direction).isEqualTo(0f)
        }

        val largeSwipe = with(rule.density) { 125.dp.toPx() + slop }
        val smallSwipe = with(rule.density) { 25.dp.toPx() + slop }

        rule.onNodeWithTag(swipeableTag).performTouchInput {
            swipe(start = center, end = center + Offset(largeSwipe, 0f))
        }
        // draggable needs to recompose to toggle startDragImmediately
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithTag(swipeableTag).performTouchInput {
            swipe(start = center, end = center - Offset(smallSwipe, 0f))
        }

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
            assertThat(state.direction).isEqualTo(1f)
        }

        advanceClock()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("B")
            assertThat(state.direction).isEqualTo(0f)
        }

        rule.onNodeWithTag(swipeableTag).performTouchInput {
            swipe(start = center, end = center - Offset(largeSwipe, 0f))
        }
        // draggable needs to recompose to toggle startDragImmediately
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithTag(swipeableTag).performTouchInput {
            swipe(start = center, end = center + Offset(smallSwipe, 0f))
        }

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("B")
            assertThat(state.direction).isEqualTo(-1f)
        }

        advanceClock()

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
            assertThat(state.direction).isEqualTo(0f)
        }
    }

    /**
     * Tests that 'snapTo' updates the state and offset immediately.
     */
    @Test
    fun swipeable_snapTo() = runBlocking(AutoTestFrameClock()) {
        lateinit var state: SwipeableState<String>
        setSwipeableContent {
            state = rememberSwipeableState("A")
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                orientation = Orientation.Horizontal
            )
        }

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("A")
            assertThat(state.offset.value).isEqualTo(0f)
        }

        state.snapTo("B")

        rule.runOnIdle {
            assertThat(state.currentValue).isEqualTo("B")
            assertThat(state.offset.value).isEqualTo(100f)
        }
    }

    /**
     * Tests that 'animateTo' starts an animation which updates the state and offset.
     */
    @Test
    fun swipeable_animateTo() = runBlocking(AutoTestFrameClock()) {
        lateinit var state: SwipeableState<String>
        setSwipeableContent {
            state = rememberSwipeableState("A")
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                orientation = Orientation.Horizontal
            )
        }

        assertThat(state.currentValue).isEqualTo("A")
        assertThat(state.offset.value).isEqualTo(0f)

        val job = launch {
            state.animateTo("B")
        }

        assertThat(state.currentValue).isEqualTo("A")
        assertThat(state.offset.value).isEqualTo(0f)

        job.join()

        assertThat(state.currentValue).isEqualTo("B")
        assertThat(state.offset.value).isEqualTo(100f)
    }

    /**
     * Tests that the 'onEnd' callback of 'animateTo' is invoked and with the correct end value.
     */
    @Test
    fun swipeable_animateTo_onEnd() = runBlocking(AutoTestFrameClock()) {
        lateinit var state: SwipeableState<String>
        setSwipeableContent {
            state = rememberSwipeableState("A")
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                orientation = Orientation.Horizontal
            )
        }

        var result: Boolean?
        try {
            state.animateTo("B")
            result = true
        } catch (c: CancellationException) {
            result = false
        }
        advanceClock()
        assertThat(result).isEqualTo(true)
    }

    /**
     * Tests that the 'onEnd' callback of 'animateTo' is invoked if the animation is interrupted.
     */
    @Test
    @Ignore("couldn't come up with a meaningful test for now. restore in ")
    fun swipeable_animateTo_onEnd_interrupted() = runBlocking {
        rule.mainClock.autoAdvance = false
        lateinit var state: SwipeableState<String>
        setSwipeableContent {
            state = rememberSwipeableState("A")
            Modifier.swipeable(
                state = state,
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                orientation = Orientation.Horizontal
            )
        }

        val job = async {
            state.animateTo("B")
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithTag(swipeableTag).performTouchInput {
            down(center)
            up()
        }
        advanceClock()
        job.await()
    }

    /**
     * Tests that the [SwipeableState] is restored, when created with [rememberSwipeableState].
     */
    @Test
    fun swipeable_restoreSwipeableState() = runBlocking(AutoTestFrameClock()) {
        val restorationTester = StateRestorationTester(rule)
        var state: SwipeableState<String>? = null

        restorationTester.setContent {
            state = rememberSwipeableState("A")
            Box(
                Modifier.swipeable(
                    state = state!!,
                    anchors = mapOf(0f to "A", 100f to "B"),
                    thresholds = { _, _ -> FractionalThreshold(0.5f) },
                    orientation = Orientation.Horizontal
                )
            )
        }

        state!!.animateTo("B")
        rule.runOnIdle {
            state = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            assertThat(state!!.currentValue).isEqualTo("B")
        }
    }

    /**
     * Tests that the `onValueChange` callback of [rememberSwipeableState] is invoked correctly.
     */
    @Test
    fun swipeable_swipeableStateFor_onValueChange() {
        var onStateChangeCallbacks = 0
        lateinit var state: MutableState<String>
        setSwipeableContent {
            state = remember { mutableStateOf("A") }
            Modifier.swipeable(
                state = rememberSwipeableStateFor(
                    value = state.value,
                    onValueChange = {
                        onStateChangeCallbacks += 1
                        state.value = it
                    }
                ),
                anchors = mapOf(0f to "A", 100f to "B"),
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                orientation = Orientation.Horizontal
            )
        }

        rule.runOnIdle {
            assertThat(state.value).isEqualTo("A")
            assertThat(onStateChangeCallbacks).isEqualTo(0)
        }

        swipeRight()

        rule.runOnIdle {
            assertThat(state.value).isEqualTo("B")
            assertThat(onStateChangeCallbacks).isEqualTo(1)
        }
    }

    /**
     * Tests that the [SwipeableState] is updated if the anchors change.
     */
    @Test
    fun swipeable_anchorsUpdated() {
        lateinit var swipeableState: SwipeableState<String>
        lateinit var anchors: MutableState<Map<Float, String>>
        setSwipeableContent {
            swipeableState = rememberSwipeableState("A")
            anchors = remember { mutableStateOf(mapOf(0f to "A")) }
            Modifier.swipeable(
                state = swipeableState,
                anchors = anchors.value,
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                orientation = Orientation.Horizontal
            )
        }

        rule.runOnIdle {
            assertThat(swipeableState.currentValue).isEqualTo("A")
            assertThat(swipeableState.offset.value).isEqualTo(0f)
        }

        anchors.value = mapOf(50f to "A")

        rule.runOnIdle {
            assertThat(swipeableState.currentValue).isEqualTo("A")
            assertThat(swipeableState.offset.value).isEqualTo(50f)
        }
    }

    /**
     * Tests that the new [SwipeableState] `onValueChange` is set up if the anchors didn't change
     */
    @Test
    fun swipeable_newStateIsInitialized_afterRecomposingWithOldAnchors() {
        lateinit var swipeableState: MutableState<SwipeableState<String>>
        val anchors = mapOf(0f to "A")
        setSwipeableContent {
            swipeableState = remember { mutableStateOf(SwipeableState("A")) }
            Modifier.swipeable(
                state = swipeableState.value,
                anchors = anchors,
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                orientation = Orientation.Horizontal
            )
        }

        rule.runOnIdle {
            assertThat(swipeableState.value.currentValue).isEqualTo("A")
            assertThat(swipeableState.value.offset.value).isEqualTo(0f)
            assertThat(swipeableState.value.anchors).isEqualTo(anchors)
        }

        swipeableState.value = SwipeableState("A")

        rule.runOnIdle {
            assertThat(swipeableState.value.currentValue).isEqualTo("A")
            assertThat(swipeableState.value.offset.value).isEqualTo(0f)
            assertThat(swipeableState.value.anchors).isEqualTo(anchors)
        }
    }

    /**
     * Tests that the [SwipeableState] is updated if the anchors change.
     */
    @Test
    fun swipeable_anchorsUpdated_currentAnchorRemoved() {
        lateinit var swipeableState: SwipeableState<String>
        lateinit var anchors: MutableState<Map<Float, String>>
        setSwipeableContent {
            swipeableState = rememberSwipeableState("A")
            anchors = remember { mutableStateOf(mapOf(0f to "A")) }
            Modifier.swipeable(
                state = swipeableState,
                anchors = anchors.value,
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                orientation = Orientation.Horizontal
            )
        }

        rule.runOnIdle {
            assertThat(swipeableState.currentValue).isEqualTo("A")
            assertThat(swipeableState.offset.value).isEqualTo(0f)
        }

        anchors.value = mapOf(50f to "B", 100f to "C")

        rule.runOnIdle {
            assertThat(swipeableState.currentValue).isEqualTo("B")
            assertThat(swipeableState.offset.value).isEqualTo(50f)
        }
    }

    /**
     * Tests that the [SwipeableState] is updated if the anchors change.
     */
    @Test
    fun swipeable_anchorsUpdated_whenAnimationInProgress() = runBlocking(AutoTestFrameClock()) {
        rule.mainClock.autoAdvance = false
        lateinit var swipeableState: SwipeableState<String>
        lateinit var anchors: MutableState<Map<Float, String>>
        setSwipeableContent {
            swipeableState = rememberSwipeableState("A")
            anchors = remember { mutableStateOf(mapOf(10f to "A", 50f to "B", 100f to "C")) }
            Modifier.swipeable(
                state = swipeableState,
                anchors = anchors.value,
                thresholds = { _, _ -> FractionalThreshold(0.5f) },
                orientation = Orientation.Horizontal
            )
        }

        rule.runOnIdle {
            assertThat(swipeableState.currentValue).isEqualTo("A")
            assertThat(swipeableState.offset.value).isEqualTo(10f)
        }

        swipeableState.animateTo("B")

        rule.mainClock.advanceTimeByFrame()

        anchors.value = mapOf(10f to "A", 100f to "C")

        advanceClock()

        rule.runOnIdle {
            // closes wins
            assertThat(swipeableState.currentValue).isEqualTo("A")
            assertThat(swipeableState.offset.value).isEqualTo(10f)
        }
    }

    @Test
    fun testInspectorValue() {
        val anchors = mapOf(0f to "A", 100f to "B")
        rule.setContent {
            val modifier = Modifier.swipeable(
                state = rememberSwipeableState("A"),
                anchors = anchors,
                orientation = Orientation.Horizontal
            ) as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("swipeable")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable()).containsExactly(
                "state",
                "anchors",
                "orientation",
                "enabled",
                "reverseDirection",
                "interactionSource",
                "thresholds",
                "resistance",
                "velocityThreshold"
            )
        }
    }

    @Test
    fun swipeable_defaultVerticalNestedScrollConnection_nestedDrag() {
        lateinit var swipeableState: SwipeableState<String>
        lateinit var anchors: MutableState<Map<Float, String>>
        lateinit var scrollState: ScrollState
        rule.setContent {
            swipeableState = rememberSwipeableState("A")
            anchors = remember { mutableStateOf(mapOf(0f to "A", -1000f to "B")) }
            scrollState = rememberScrollState()
            Box(
                Modifier
                    .size(300.dp)
                    .nestedScroll(swipeableState.PreUpPostDownNestedScrollConnection)
                    .swipeable(
                        state = swipeableState,
                        anchors = anchors.value,
                        thresholds = { _, _ -> FractionalThreshold(0.5f) },
                        orientation = Orientation.Horizontal
                    )
            ) {
                Column(
                    Modifier.fillMaxWidth().testTag(swipeableTag).verticalScroll(scrollState)
                ) {
                    repeat(100) {
                        Text(text = it.toString(), modifier = Modifier.requiredHeight(50.dp))
                    }
                }
            }
        }

        assertThat(swipeableState.currentValue).isEqualTo("A")

        // This actually produces a velocity of 300,000 (1500 px moved in 5 ms), but we are
        // relying on the compatibility behaviour that 2 points produce a zero velocity.
        rule.onNodeWithTag(swipeableTag)
            .performTouchInput {
                down(Offset(x = 10f, y = 10f))
                moveBy(Offset(x = 0f, y = -1500f))
                up()
            }

        advanceClock()

        assertThat(swipeableState.currentValue).isEqualTo("B")
        assertThat(scrollState.value).isGreaterThan(0)

        rule.onNodeWithTag(swipeableTag)
            .performTouchInput {
                down(Offset(x = 10f, y = 10f))
                moveBy(Offset(x = 0f, y = 1500f))
                up()
            }

        advanceClock()

        assertThat(swipeableState.currentValue).isEqualTo("A")
        assertThat(scrollState.value).isEqualTo(0)
    }

    @Test
    fun swipeable_nestedScroll_preFling() {
        lateinit var swipeableState: SwipeableState<String>
        lateinit var anchors: MutableState<Map<Float, String>>
        lateinit var scrollState: ScrollState
        rule.setContent {
            swipeableState = rememberSwipeableState("A")
            anchors = remember { mutableStateOf(mapOf(0f to "A", -1000f to "B")) }
            scrollState = rememberScrollState()
            Box(
                Modifier
                    .size(300.dp)
                    .nestedScroll(swipeableState.PreUpPostDownNestedScrollConnection)
                    .swipeable(
                        state = swipeableState,
                        anchors = anchors.value,
                        thresholds = { _, _ -> FixedThreshold(56.dp) },
                        orientation = Orientation.Horizontal
                    )
            ) {
                Column(
                    Modifier.fillMaxWidth().testTag(swipeableTag).verticalScroll(scrollState)
                ) {
                    repeat(100) {
                        Text(text = it.toString(), modifier = Modifier.requiredHeight(50.dp))
                    }
                }
            }
        }

        rule.runOnIdle {
            assertThat(swipeableState.currentValue).isEqualTo("A")
        }

        rule.onNodeWithTag(swipeableTag)
            .performTouchInput {
                swipeWithVelocity(
                    center,
                    center.copy(y = centerY - 500, x = centerX),
                    durationMillis = 50,
                    endVelocity = 20000f
                )
            }

        rule.runOnIdle {
            assertThat(swipeableState.currentValue).isEqualTo("B")
            // should eat all velocity, no internal scroll
            assertThat(scrollState.value).isEqualTo(0)
        }

        rule.onNodeWithTag(swipeableTag)
            .performTouchInput {
                swipeWithVelocity(
                    center,
                    center.copy(y = centerY + 500, x = centerX),
                    durationMillis = 50,
                    endVelocity = 20000f
                )
            }

        rule.runOnIdle {
            assertThat(swipeableState.currentValue).isEqualTo("A")
            assertThat(scrollState.value).isEqualTo(0)
        }
    }

    @Test
    fun swipeable_nestedScroll_postFlings() = runBlocking(AutoTestFrameClock()) {
        lateinit var swipeableState: SwipeableState<String>
        lateinit var anchors: MutableState<Map<Float, String>>
        lateinit var scrollState: ScrollState
        rule.setContent {
            swipeableState = rememberSwipeableState("B")
            anchors = remember { mutableStateOf(mapOf(0f to "A", -1000f to "B")) }
            scrollState = rememberScrollState(initial = 5000)
            Box(
                Modifier
                    .size(300.dp)
                    .nestedScroll(swipeableState.PreUpPostDownNestedScrollConnection)
                    .swipeable(
                        state = swipeableState,
                        anchors = anchors.value,
                        thresholds = { _, _ -> FixedThreshold(56.dp) },
                        orientation = Orientation.Horizontal
                    )
            ) {
                Column(
                    Modifier.fillMaxWidth().testTag(swipeableTag).verticalScroll(scrollState)
                ) {
                    repeat(100) {
                        Text(text = it.toString(), modifier = Modifier.requiredHeight(50.dp))
                    }
                }
            }
        }

        rule.awaitIdle()
        assertThat(swipeableState.currentValue).isEqualTo("B")
        assertThat(scrollState.value).isEqualTo(5000)

        rule.onNodeWithTag(swipeableTag)
            .performTouchInput {
                // swipe less than scrollState.value but with velocity to test that backdrop won't
                // move when receives, because it's at anchor
                swipeWithVelocity(
                    center,
                    center.copy(y = centerY + 1500, x = centerX),
                    durationMillis = 50,
                    endVelocity = 20000f
                )
            }

        rule.awaitIdle()
        assertThat(swipeableState.currentValue).isEqualTo("B")
        assertThat(scrollState.value).isEqualTo(0)
        // set value again to test overshoot
        scrollState.scrollBy(500f)

        rule.runOnIdle {
            assertThat(swipeableState.currentValue).isEqualTo("B")
            assertThat(scrollState.value).isEqualTo(500)
        }

        rule.onNodeWithTag(swipeableTag)
            .performTouchInput {
                // swipe more than scrollState.value so backdrop start receiving nested scroll
                swipeWithVelocity(
                    center,
                    center.copy(y = centerY + 1500, x = centerX),
                    durationMillis = 50,
                    endVelocity = 20000f
                )
            }

        rule.runOnIdle {
            assertThat(swipeableState.currentValue).isEqualTo("A")
            assertThat(scrollState.value).isEqualTo(0)
        }
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
        rule.mainClock.advanceTimeBy(100_000L)
    }

    private fun performSwipe(x: Float = 0f, y: Float = 0f, velocity: Float? = null) {
        rule.onNodeWithTag(swipeableTag).performTouchInput {
            val start = Offset(center.x - x / 2, center.y - y / 2)
            val end = Offset(center.x + x / 2, center.y + y / 2)
            if (velocity == null) swipe(start, end) else swipeWithVelocity(start, end, velocity)
        }
    }

    private fun setSwipeableContent(swipeableFactory: @Composable () -> Modifier) {
        rule.setMaterialContent {
            Box(modifier = Modifier.fillMaxSize().testTag(swipeableTag).then(swipeableFactory()))
        }
    }
}
