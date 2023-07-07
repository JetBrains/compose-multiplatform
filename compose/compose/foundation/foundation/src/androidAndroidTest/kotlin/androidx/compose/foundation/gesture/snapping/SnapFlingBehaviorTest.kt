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

package androidx.compose.foundation.gesture.snapping

import androidx.compose.animation.SplineBasedFloatDecayAnimationSpec
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.FloatDecayAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorizedAnimationSpec
import androidx.compose.animation.core.generateDecayAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TestScrollMotionDurationScale
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.snapping.MinFlingVelocityDp
import androidx.compose.foundation.gestures.snapping.NoVelocity
import androidx.compose.foundation.gestures.snapping.SnapFlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.findClosestOffset
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalFoundationApi::class)
class SnapFlingBehaviorTest {
    @get:Rule
    val rule = createComposeRule()

    private val inspectSpringAnimationSpec = InspectSpringAnimationSpec(spring())
    private val inspectTweenAnimationSpec = InspectSpringAnimationSpec(tween(easing = LinearEasing))

    private val density: Density
        get() = rule.density

    @Test
    fun performFling_whenVelocityIsBelowThreshold_shouldShortSnap() {
        val testLayoutInfoProvider = TestLayoutInfoProvider()
        rule.setContent {
            val testFlingBehavior = rememberSnapFlingBehavior(testLayoutInfoProvider)
            VelocityEffect(testFlingBehavior, calculateVelocityThreshold() - 1)
        }

        rule.runOnIdle {
            assertEquals(0, testLayoutInfoProvider.calculateApproachOffsetCount)
        }
    }

    @Test
    fun performFling_whenVelocityIsAboveThreshold_shouldLongSnap() {
        val testLayoutInfoProvider = TestLayoutInfoProvider()
        rule.setContent {
            val testFlingBehavior = rememberSnapFlingBehavior(testLayoutInfoProvider)
            VelocityEffect(testFlingBehavior, calculateVelocityThreshold() + 1)
        }

        rule.runOnIdle {
            assertEquals(1, testLayoutInfoProvider.calculateApproachOffsetCount)
        }
    }

    @Test
    fun remainingScrollOffset_whenVelocityIsBelowThreshold_shouldRepresentShortSnapOffsets() {
        val testLayoutInfoProvider = TestLayoutInfoProvider()
        lateinit var testFlingBehavior: SnapFlingBehavior
        val scrollOffset = mutableListOf<Float>()
        rule.setContent {
            testFlingBehavior = rememberSnapFlingBehavior(testLayoutInfoProvider)
            VelocityEffect(
                testFlingBehavior,
                calculateVelocityThreshold() - 1
            ) { remainingScrollOffset ->
                scrollOffset.add(remainingScrollOffset)
            }
        }

        // Will Snap Back
        rule.runOnIdle {
            assertEquals(scrollOffset.first(), testLayoutInfoProvider.minOffset)
            assertEquals(scrollOffset.last(), 0f)
        }
    }

    @Test
    fun remainingScrollOffset_whenVelocityIsAboveThreshold_shouldRepresentLongSnapOffsets() {
        val testLayoutInfoProvider = TestLayoutInfoProvider()
        lateinit var testFlingBehavior: SnapFlingBehavior
        val scrollOffset = mutableListOf<Float>()
        rule.setContent {
            testFlingBehavior = rememberSnapFlingBehavior(testLayoutInfoProvider)
            VelocityEffect(
                testFlingBehavior,
                calculateVelocityThreshold() + 1
            ) { remainingScrollOffset ->
                scrollOffset.add(remainingScrollOffset)
            }
        }

        rule.runOnIdle {
            assertEquals(scrollOffset.first { it != 0f }, testLayoutInfoProvider.maxOffset)
            assertEquals(scrollOffset.last(), 0f)
        }
    }

    @Test
    fun remainingScrollOffset_longSnap_targetShouldChangeInAccordanceWithAnimation() {
        // Arrange
        val initialOffset = 250f
        val testLayoutInfoProvider = TestLayoutInfoProvider(approachOffset = initialOffset)
        lateinit var testFlingBehavior: SnapFlingBehavior
        val scrollOffset = mutableListOf<Float>()
        rule.mainClock.autoAdvance = false
        rule.setContent {
            testFlingBehavior = rememberSnapFlingBehavior(testLayoutInfoProvider)
            VelocityEffect(
                testFlingBehavior,
                calculateVelocityThreshold() + 1
            ) { remainingScrollOffset ->
                scrollOffset.add(remainingScrollOffset)
            }
        }

        // assert the initial value emitted by remainingScrollOffset was the one provider by the
        // snap layout info provider
        assertEquals(scrollOffset.first(), initialOffset)

        // Act: Advance until remainingScrollOffset grows again
        rule.mainClock.advanceTimeUntil {
            scrollOffset.size > 2 &&
                scrollOffset.last() > scrollOffset[scrollOffset.lastIndex - 1]
        }

        assertEquals(scrollOffset.last(), testLayoutInfoProvider.maxOffset)

        rule.mainClock.autoAdvance = true
        // Assert
        rule.runOnIdle {
            assertEquals(scrollOffset.last(), 0f)
        }
    }

    @Test
    fun performFling_afterSnappingVelocity_everythingWasConsumed_shouldReturnNoVelocity() {
        val testLayoutInfoProvider = TestLayoutInfoProvider()
        var afterFlingVelocity = 0f
        rule.setContent {
            val scrollableState = rememberScrollableState(consumeScrollDelta = { it })
            val testFlingBehavior = rememberSnapFlingBehavior(testLayoutInfoProvider)

            LaunchedEffect(Unit) {
                scrollableState.scroll {
                    afterFlingVelocity = with(testFlingBehavior) {
                        performFling(50000f)
                    }
                }
            }
        }

        rule.runOnIdle {
            assertEquals(NoVelocity, afterFlingVelocity)
        }
    }

    @Test
    fun performFling_afterSnappingVelocity_didNotConsumeAllScroll_shouldReturnRemainingVelocity() {
        val testLayoutInfoProvider = TestLayoutInfoProvider()
        var afterFlingVelocity = 0f
        rule.setContent {
            // Consume only half
            val scrollableState = rememberScrollableState(consumeScrollDelta = { it / 2f })
            val testFlingBehavior = rememberSnapFlingBehavior(testLayoutInfoProvider)

            LaunchedEffect(Unit) {
                scrollableState.scroll {
                    afterFlingVelocity = with(testFlingBehavior) {
                        performFling(50000f)
                    }
                }
            }
        }

        rule.runOnIdle {
            assertNotEquals(NoVelocity, afterFlingVelocity)
        }
    }

    @Test
    fun findClosestOffset_noFlingDirection_shouldReturnAbsoluteDistance() {
        val testLayoutInfoProvider = TestLayoutInfoProvider()
        val offset = findClosestOffset(0f, testLayoutInfoProvider, density)
        assertEquals(offset, MinOffset)
    }

    @Test
    fun findClosestOffset_flingDirection_shouldReturnCorrectBound() {
        val testLayoutInfoProvider = TestLayoutInfoProvider()
        val forwardOffset = findClosestOffset(1f, testLayoutInfoProvider, density)
        val backwardOffset = findClosestOffset(-1f, testLayoutInfoProvider, density)
        assertEquals(forwardOffset, MaxOffset)
        assertEquals(backwardOffset, MinOffset)
    }

    @Test
    fun approach_cannotDecay_useLowVelocityApproachAndSnap() {
        val testLayoutInfoProvider = TestLayoutInfoProvider(approachOffset = SnapStep * 5)
        var inspectSplineAnimationSpec: InspectSplineAnimationSpec? = null
        rule.setContent {
            val splineAnimationSpec = rememberInspectSplineAnimationSpec().also {
                inspectSplineAnimationSpec = it
            }
            val testFlingBehavior = rememberSnapFlingBehavior(
                snapLayoutInfoProvider = testLayoutInfoProvider,
                highVelocityApproachSpec = splineAnimationSpec.generateDecayAnimationSpec(),
                lowVelocityApproachSpec = inspectTweenAnimationSpec,
                snapAnimationSpec = inspectSpringAnimationSpec
            )
            VelocityEffect(testFlingBehavior, calculateVelocityThreshold() * 2)
        }

        rule.runOnIdle {
            assertEquals(0, inspectSplineAnimationSpec?.animationWasExecutions)
            assertEquals(1, inspectTweenAnimationSpec.animationWasExecutions)
            assertEquals(1, inspectSpringAnimationSpec.animationWasExecutions)
        }
    }

    @Test
    fun approach_canDecay_decayAndSnap() {
        val testLayoutInfoProvider = TestLayoutInfoProvider(maxOffset = 100f)
        var inspectSplineAnimationSpec: InspectSplineAnimationSpec? = null
        rule.setContent {
            val splineAnimationSpec = rememberInspectSplineAnimationSpec().also {
                inspectSplineAnimationSpec = it
            }
            val testFlingBehavior = rememberSnapFlingBehavior(
                snapLayoutInfoProvider = testLayoutInfoProvider,
                highVelocityApproachSpec = splineAnimationSpec.generateDecayAnimationSpec(),
                lowVelocityApproachSpec = inspectTweenAnimationSpec,
                snapAnimationSpec = inspectSpringAnimationSpec
            )
            VelocityEffect(testFlingBehavior, calculateVelocityThreshold() * 5)
        }

        rule.runOnIdle {
            assertEquals(1, inspectSplineAnimationSpec?.animationWasExecutions)
            assertEquals(1, inspectSpringAnimationSpec.animationWasExecutions)
            assertEquals(0, inspectTweenAnimationSpec.animationWasExecutions)
        }
    }

    @Test
    fun disableSystemAnimations_defaultFlingBehaviorShouldContinueToWork() {

        lateinit var defaultFlingBehavior: SnapFlingBehavior
        lateinit var scope: CoroutineScope
        val state = LazyListState()
        rule.setContent {
            scope = rememberCoroutineScope()
            defaultFlingBehavior = rememberSnapFlingBehavior(state) as SnapFlingBehavior

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                state = state,
                flingBehavior = defaultFlingBehavior as FlingBehavior
            ) {
                items(200) { Box(modifier = Modifier.size(20.dp)) }
            }
        }

        // Act: Stop clock and fling, one frame should not settle immediately.
        rule.mainClock.autoAdvance = false
        scope.launch {
            state.scroll {
                with(defaultFlingBehavior) { performFling(10000f) }
            }
        }
        rule.mainClock.advanceTimeByFrame()

        // Assert
        rule.runOnIdle {
            Truth.assertThat(state.firstVisibleItemIndex).isEqualTo(0)
        }

        rule.mainClock.autoAdvance = true

        val previousIndex = state.firstVisibleItemIndex

        // Simulate turning off system wide animation
        scope.launch {
            state.scroll {
                withContext(TestScrollMotionDurationScale(0f)) {
                    with(defaultFlingBehavior) { performFling(10000f) }
                }
            }
        }

        // Act: Stop clock and fling, one frame should not settle immediately.
        rule.mainClock.autoAdvance = false
        scope.launch {
            state.scroll {
                with(defaultFlingBehavior) { performFling(10000f) }
            }
        }
        rule.mainClock.advanceTimeByFrame()

        // Assert
        rule.runOnIdle {
            Truth.assertThat(state.firstVisibleItemIndex).isEqualTo(previousIndex)
        }

        rule.mainClock.autoAdvance = true

        // Assert: let it settle
        rule.runOnIdle {
            Truth.assertThat(state.firstVisibleItemIndex).isNotEqualTo(previousIndex)
        }
    }

    @Test
    fun defaultFlingBehavior_useScrollMotionDurationScale() {
        // Arrange
        var switchMotionDurationScale by mutableStateOf(false)
        lateinit var defaultFlingBehavior: SnapFlingBehavior
        lateinit var scope: CoroutineScope
        val state = LazyListState()
        rule.setContent {
            scope = rememberCoroutineScope()
            defaultFlingBehavior = rememberSnapFlingBehavior(state) as SnapFlingBehavior

            LazyRow(
                modifier = Modifier
                    .testTag("snappingList")
                    .fillMaxSize(),
                state = state,
                flingBehavior = defaultFlingBehavior as FlingBehavior
            ) {
                items(200) {
                    Box(modifier = Modifier.size(150.dp)) {
                        BasicText(text = it.toString())
                    }
                }
            }

            if (switchMotionDurationScale) {
                defaultFlingBehavior.motionScaleDuration = TestScrollMotionDurationScale(1f)
            } else {
                defaultFlingBehavior.motionScaleDuration = TestScrollMotionDurationScale(0f)
            }
        }

        // Act: Stop clock and fling, one frame should settle immediately.
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("snappingList").performTouchInput {
            swipeWithVelocity(centerRight, center, 10000f)
        }
        rule.mainClock.advanceTimeByFrame()

        // Assert
        rule.runOnIdle {
            Truth.assertThat(state.firstVisibleItemIndex).isGreaterThan(0)
        }

        // Arrange
        rule.mainClock.autoAdvance = true
        switchMotionDurationScale = true // Let animations run normally
        rule.waitForIdle()

        val previousIndex = state.firstVisibleItemIndex
        // Act: Stop clock and fling, one frame should not settle.
        rule.mainClock.autoAdvance = false
        scope.launch {
            state.scroll {
                with(defaultFlingBehavior) { performFling(10000f) }
            }
        }

        // Assert: First index hasn't changed because animation hasn't started
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            Truth.assertThat(state.firstVisibleItemIndex).isEqualTo(previousIndex)
        }
        rule.mainClock.autoAdvance = true

        // Wait for settling
        rule.runOnIdle {
            Truth.assertThat(state.firstVisibleItemIndex).isNotEqualTo(previousIndex)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VelocityEffect(
    testFlingBehavior: FlingBehavior,
    velocity: Float,
    onSettlingDistanceUpdated: (Float) -> Unit = {}
) {
    val scrollableState = rememberScrollableState(consumeScrollDelta = { it })
    LaunchedEffect(Unit) {
        scrollableState.scroll {
            with(testFlingBehavior as SnapFlingBehavior) {
                performFling(velocity, onSettlingDistanceUpdated)
            }
        }
    }
}

private class InspectSpringAnimationSpec(
    private val animation: AnimationSpec<Float>
) : AnimationSpec<Float> {

    var animationWasExecutions = 0

    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<Float, V>
    ): VectorizedAnimationSpec<V> {
        animationWasExecutions++
        return animation.vectorize(converter)
    }
}

private class InspectSplineAnimationSpec(
    private val splineBasedFloatDecayAnimationSpec: SplineBasedFloatDecayAnimationSpec
) : FloatDecayAnimationSpec by splineBasedFloatDecayAnimationSpec {

    private var valueFromNanosCalls = 0
    val animationWasExecutions: Int
        get() = valueFromNanosCalls / 2

    override fun getValueFromNanos(
        playTimeNanos: Long,
        initialValue: Float,
        initialVelocity: Float
    ): Float {

        if (playTimeNanos == 0L) {
            valueFromNanosCalls++
        }

        return splineBasedFloatDecayAnimationSpec.getValueFromNanos(
            playTimeNanos,
            initialValue,
            initialVelocity
        )
    }
}

@Composable
private fun rememberInspectSplineAnimationSpec(): InspectSplineAnimationSpec {
    val density = LocalDensity.current
    return remember {
        InspectSplineAnimationSpec(
            SplineBasedFloatDecayAnimationSpec(density)
        )
    }
}

@Composable
private fun calculateVelocityThreshold(): Float {
    val density = LocalDensity.current
    return with(density) { MinFlingVelocityDp.toPx() }
}

private const val SnapStep = 250f
private const val MinOffset = -200f
private const val MaxOffset = 300f

@OptIn(ExperimentalFoundationApi::class)

private class TestLayoutInfoProvider(
    val minOffset: Float = MinOffset,
    val maxOffset: Float = MaxOffset,
    val snapStep: Float = SnapStep,
    val approachOffset: Float = 0f
) : SnapLayoutInfoProvider {
    var calculateApproachOffsetCount = 0

    override fun Density.calculateSnapStepSize(): Float {
        return snapStep
    }

    override fun Density.calculateSnappingOffsetBounds(): ClosedFloatingPointRange<Float> {
        return minOffset.rangeTo(maxOffset)
    }

    override fun Density.calculateApproachOffset(initialVelocity: Float): Float {
        calculateApproachOffsetCount++
        return approachOffset
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun rememberSnapFlingBehavior(
    snapLayoutInfoProvider: SnapLayoutInfoProvider,
    highVelocityApproachSpec: DecayAnimationSpec<Float>,
    lowVelocityApproachSpec: AnimationSpec<Float>,
    snapAnimationSpec: AnimationSpec<Float>
): FlingBehavior {
    val density = LocalDensity.current
    return remember(
        snapLayoutInfoProvider,
        highVelocityApproachSpec
    ) {
        SnapFlingBehavior(
            snapLayoutInfoProvider = snapLayoutInfoProvider,
            lowVelocityAnimationSpec = lowVelocityApproachSpec,
            highVelocityAnimationSpec = highVelocityApproachSpec,
            snapAnimationSpec = snapAnimationSpec,
            density = density
        )
    }
}