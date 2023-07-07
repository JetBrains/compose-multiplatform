/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.animation.core

import com.google.common.truth.Truth.assertThat
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PhysicsAnimationTest {

    @Test
    fun velocityCalculation() {
        val animation = FloatSpringSpec()

        val start = 200f
        val end = 500f
        val playTime = 150L

        val velocity = animation.getVelocityFromMillis(playTime, start, end, 0f)
        val expectedVelocity = animation.toSpring(end).updateValues(start, 0f, playTime).velocity
        assertThat(velocity).isEqualTo(expectedVelocity)
    }

    @Test
    fun velocityCalculationForInts() {

        val start = 200
        val end = 500
        val playTime = 150L
        val animation = TargetBasedAnimation(
            spring(), Int.VectorConverter, start, end, 0
        )

        val velocity = animation.getVelocityFromMillis(playTime)

        val expectedVelocity = FloatSpringSpec().toSpring(end)
            .updateValues(start.toFloat(), 0f, playTime).velocity.toInt()
        assertThat(velocity).isEqualTo(expectedVelocity)
    }

    @Test
    fun animationWithoutRangePreservesVelocity() {

        // first animation that will be interrupted after 150 ms
        val start1 = 200f
        val end1 = 500f
        val interruptionTime = 150L

        val animation = TargetBasedAnimation(
            spring(), Float.VectorConverter, start1, end1, 0f
        )

        val interruptionValue = animation.getValueFromMillis(interruptionTime)
        val interruptionVelocity = animation.getVelocityFromMillis(interruptionTime)

        // second animation will go from interruptionValue to interruptionValue with
        // applying the velocity from the first interrupted animation.
        val start2 = interruptionValue
        val end2 = interruptionValue
        val startVelocity2 = interruptionVelocity

        val animation2 = TargetBasedAnimation(
            spring(), Float.VectorConverter, start2, end2, startVelocity2
        )
        // let's verify values after 15 ms of the second animation
        val playTime = 15L
        val resultValue = animation2.getValueFromMillis(playTime)
        val resultVelocity = animation2.getVelocityFromMillis(playTime)

        val motion = FloatSpringSpec().toSpring(end2).updateValues(
            start2,
            interruptionVelocity,
            playTime
        )
        val expectedValue = motion.value
        val expectedVelocity = motion.velocity

        assertThat(resultValue).isEqualTo(expectedValue)
        assertThat(resultVelocity).isEqualTo(expectedVelocity)
    }

    @Test
    fun testCriticallydampedDuration() {
        val startValue = 100f
        val endValue = 0f
        val startVelocity = 3000f
        val stiffness = 100f
        val delta = 1.0

        val criticalSpec = FloatSpringSpec(
            dampingRatio = 1f,
            stiffness = stiffness,
            visibilityThreshold = 1f
        )
        val criticalWrapper = TargetBasedAnimation(
            criticalSpec,
            initialValue = startValue,
            targetValue = endValue,
            initialVelocity = startVelocity,
            typeConverter = Float.VectorConverter
        )

        assertEquals(
            estimateAnimationDurationMillis(
                stiffness = stiffness.toDouble(),
                dampingRatio = 1.0,
                initialVelocity = startVelocity.toDouble(),
                initialDisplacement = startValue.toDouble(),
                delta = delta
            ) /* = 811 ms*/,
            criticalWrapper.durationMillis
        )
    }

    @Test
    fun testOverdampedDuration() {
        val startValue = 100f
        val endValue = 0f
        val startVelocity = 3000f
        val stiffness = 100f
        val delta = 1.0

        val overSpec = FloatSpringSpec(
            dampingRatio = 5f,
            stiffness = stiffness,
            visibilityThreshold = 1f
        )
        val overWrapper = TargetBasedAnimation(
            overSpec,
            initialValue = startValue,
            targetValue = endValue,
            initialVelocity = startVelocity,
            typeConverter = Float.VectorConverter
        )

        assertEquals(
            estimateAnimationDurationMillis(
                stiffness = stiffness.toDouble(),
                dampingRatio = 5.0,
                initialVelocity = startVelocity.toDouble(),
                initialDisplacement = startValue.toDouble(),
                delta = delta
            ) /* = 4830 ms*/,
            overWrapper.durationMillis
        )
    }

    @Test
    fun testUnderdampedDuration() {
        val startValue = 100f
        val endValue = 0f
        val startVelocity = 3000f
        val stiffness = 100f
        val delta = 1.0

        val underSpec = FloatSpringSpec(
            dampingRatio = 0.5f,
            stiffness = stiffness,
            visibilityThreshold = 1f
        )
        val underWrapper = TargetBasedAnimation(
            underSpec,
            initialValue = startValue,
            targetValue = endValue,
            initialVelocity = startVelocity,
            typeConverter = Float.VectorConverter
        )

        assertEquals(
            estimateAnimationDurationMillis(
                stiffness = stiffness.toDouble(),
                dampingRatio = 0.5,
                initialVelocity = startVelocity.toDouble(),
                initialDisplacement = startValue.toDouble(),
                delta = delta
            ) /* = 1206 ms*/,
            underWrapper.durationMillis
        )
    }

    @Test
    fun testEndSnapping() {
        TargetBasedAnimation(
            spring(),
            Float.VectorConverter,
            0f,
            100f,
            0f
        ).also { animation ->
            assertEquals(0f, animation.getVelocityVectorFromMillis(animation.durationMillis).value)
            assertEquals(100f, animation.getValueFromMillis(animation.durationMillis))
        }
    }

    @Test
    fun testSpringVectorAnimationDuration() {
        data class ClassToAnimate(var one: Float, var two: Float, var three: Float)

        val converter =
            TwoWayConverter<ClassToAnimate, AnimationVector3D>(
                convertToVector = { it ->
                    AnimationVector(it.one, it.two, it.three)
                },
                convertFromVector = { it ->
                    ClassToAnimate(it.v1, it.v2, it.v3)
                }
            )
        val springVectorAnimation = VectorizedSpringSpec(
            visibilityThreshold = converter.convertToVector(ClassToAnimate(1f, 2f, 3f))
        )
        val floatAnimation = FloatSpringSpec(visibilityThreshold = 1f)

        val springVectorDuration = springVectorAnimation.getDurationMillis(
            AnimationVector(100f, 100f, 100f),
            AnimationVector(0f, 0f, 0f),
            AnimationVector(0f, 0f, 0f)
        )
        val floatAnimationDuration = floatAnimation.getDurationMillis(
            100f, 0f, 0f
        )

        // Vector duration should be the longest of all the sub animations
        // In this case it should be the one with the lowest threshold.
        assertEquals(springVectorDuration, floatAnimationDuration)
    }

    @Test
    fun testSpringVectorAnimationValues() {
        data class ClassToAnimate(var one: Float, var two: Float, var three: Float)

        val converter =
            TwoWayConverter<ClassToAnimate, AnimationVector3D>(
                convertToVector = { it ->
                    AnimationVector(it.one, it.two, it.three)
                },
                convertFromVector = { it ->
                    ClassToAnimate(it.v1, it.v2, it.v3)
                }
            )
        val springVectorAnimation = VectorizedSpringSpec(
            visibilityThreshold = converter.convertToVector(ClassToAnimate(1f, 2f, 3f))
        )
        val floatAnimation1 = VectorizedSpringSpec(visibilityThreshold = AnimationVector(1f))
        val floatAnimation2 = VectorizedSpringSpec(visibilityThreshold = AnimationVector(2f))
        val floatAnimation3 = VectorizedSpringSpec(visibilityThreshold = AnimationVector(3f))

        val duration = springVectorAnimation.getDurationMillis(
            AnimationVector(100f, 100f, 100f),
            AnimationVector(0f, 0f, 0f),
            AnimationVector(0f, 0f, 0f)
        )

        for (time in 0L until duration) {
            val springVector = springVectorAnimation.getValueFromMillis(
                time,
                AnimationVector(100f, 100f, 100f),
                AnimationVector(0f, 0f, 0f),
                AnimationVector(0f, 0f, 0f)
            )
            val float1 = floatAnimation1.getValueFromMillis(
                time,
                AnimationVector(100f),
                AnimationVector(0f),
                AnimationVector(0f)
            )
            val float2 = floatAnimation2.getValueFromMillis(
                time,
                AnimationVector(100f),
                AnimationVector(0f),
                AnimationVector(0f)
            )
            val float3 = floatAnimation3.getValueFromMillis(
                time,
                AnimationVector(100f),
                AnimationVector(0f),
                AnimationVector(0f)
            )
            assertEquals(float1.value, springVector.v1, epsilon)
            assertEquals(float2.value, springVector.v2, epsilon)
            assertEquals(float3.value, springVector.v3, epsilon)
        }
    }

    private fun VectorizedAnimationSpec<AnimationVector1D>.toAnimation(
        startValue: Float,
        startVelocity: Float,
        endValue: Float
    ): Animation<Float, AnimationVector1D> {
        return TargetBasedAnimation(
            animationSpec = this,
            initialValue = startValue,
            targetValue = endValue,
            initialVelocityVector = AnimationVector(startVelocity),
            typeConverter = Float.VectorConverter
        )
    }

    private fun FloatSpringSpec.toSpring(endValue: Number) =
        SpringSimulation(endValue.toFloat()).also {
            it.dampingRatio = dampingRatio
            it.stiffness = stiffness
        }
}