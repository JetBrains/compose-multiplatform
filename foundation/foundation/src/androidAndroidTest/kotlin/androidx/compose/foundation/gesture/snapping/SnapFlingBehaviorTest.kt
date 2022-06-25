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
import androidx.compose.animation.core.FloatDecayAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorizedAnimationSpec
import androidx.compose.animation.core.generateDecayAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.snapping.MinFlingVelocityDp
import androidx.compose.foundation.gestures.snapping.NoVelocity
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.findClosestOffset
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.test.assertEquals
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
    fun performFling_afterSnappingVelocity_shouldReturnNoVelocity() {
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
    fun findClosestOffset_noFlingDirection_shouldReturnAbsoluteDistance() {
        val testLayoutInfoProvider = TestLayoutInfoProvider()
        val offset = findClosestOffset(0f, testLayoutInfoProvider)
        assertEquals(offset, MinOffset)
    }

    @Test
    fun findClosestOffset_flingDirection_shouldReturnCorrectBound() {
        val testLayoutInfoProvider = TestLayoutInfoProvider()
        val forwardOffset = findClosestOffset(1f, testLayoutInfoProvider)
        val backwardOffset = findClosestOffset(-1f, testLayoutInfoProvider)
        assertEquals(forwardOffset, MaxOffset)
        assertEquals(backwardOffset, MinOffset)
    }

    @Test
    fun approach_cannotDecay_justSnap() {
        val testLayoutInfoProvider = TestLayoutInfoProvider(approachOffset = SnapStep * 5)
        var inspectSplineAnimationSpec: InspectSplineAnimationSpec? = null
        rule.setContent {
            val splineAnimationSpec = rememberInspectSplineAnimationSpec().also {
                inspectSplineAnimationSpec = it
            }
            val testFlingBehavior = rememberSnapFlingBehavior(
                snapLayoutInfoProvider = testLayoutInfoProvider,
                approachAnimationSpec = splineAnimationSpec.generateDecayAnimationSpec()
            )
            VelocityEffect(testFlingBehavior, calculateVelocityThreshold() * 2)
        }

        rule.runOnIdle {
            assertEquals(0, inspectSplineAnimationSpec?.animationWasExecutions)
        }
    }

    @Test
    fun approach_canDecayWithoutHalfStep_decayAndSnap() {
        val testLayoutInfoProvider = TestLayoutInfoProvider(maxOffset = 100f)
        var inspectSplineAnimationSpec: InspectSplineAnimationSpec? = null
        rule.setContent {
            val splineAnimationSpec = rememberInspectSplineAnimationSpec().also {
                inspectSplineAnimationSpec = it
            }
            val testFlingBehavior = rememberSnapFlingBehavior(
                snapLayoutInfoProvider = testLayoutInfoProvider,
                approachAnimationSpec = splineAnimationSpec.generateDecayAnimationSpec(),
                snapAnimationSpec = inspectSpringAnimationSpec
            )
            VelocityEffect(testFlingBehavior, calculateVelocityThreshold() * 5)
        }

        rule.runOnIdle {
            assertEquals(1, inspectSplineAnimationSpec?.animationWasExecutions)
            assertEquals(1, inspectSpringAnimationSpec.animationWasExecutions)
        }
    }

    @Test
    fun approach_canDecayWithHalfStep_doubleDecayAndSnap() {
        val testLayoutInfoProvider = TestLayoutInfoProvider(maxOffset = 300f, snapStep = 400f)
        var inspectSplineAnimationSpec: InspectSplineAnimationSpec? = null
        rule.setContent {
            val splineAnimationSpec = rememberInspectSplineAnimationSpec().also {
                inspectSplineAnimationSpec = it
            }
            val testFlingBehavior = rememberSnapFlingBehavior(
                snapLayoutInfoProvider = testLayoutInfoProvider,
                approachAnimationSpec = splineAnimationSpec.generateDecayAnimationSpec(),
                snapAnimationSpec = inspectSpringAnimationSpec
            )

            VelocityEffect(testFlingBehavior, calculateVelocityThreshold() * 3)
        }

        rule.runOnIdle {
            assertEquals(2, inspectSplineAnimationSpec?.animationWasExecutions)
            assertEquals(1, inspectSpringAnimationSpec.animationWasExecutions)
        }
    }
}

@Composable
private fun VelocityEffect(testFlingBehavior: FlingBehavior, velocity: Float) {
    val scrollableState = rememberScrollableState(consumeScrollDelta = { it })
    LaunchedEffect(Unit) {
        scrollableState.scroll {
            with(testFlingBehavior) {
                performFling(velocity)
            }
        }
    }
}

private class InspectSpringAnimationSpec(
    private val springSpec: SpringSpec<Float>
) : AnimationSpec<Float> by springSpec {

    var animationWasExecutions = 0

    override fun <V : AnimationVector> vectorize(
        converter: TwoWayConverter<Float, V>
    ): VectorizedAnimationSpec<V> {
        animationWasExecutions++
        return springSpec.vectorize(converter)
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

    override val snapStepSize: Float
        get() = snapStep

    override fun calculateSnappingOffsetBounds(): ClosedFloatingPointRange<Float> {
        return minOffset.rangeTo(maxOffset)
    }

    override fun calculateApproachOffset(initialVelocity: Float): Float {
        calculateApproachOffsetCount++
        return approachOffset
    }
}