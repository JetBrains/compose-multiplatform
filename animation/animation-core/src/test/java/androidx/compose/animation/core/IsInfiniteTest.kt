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

package androidx.compose.animation.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class IsInfiniteTest {
    @Test
    fun testTweenIsFinite() {
        val tweenSpec = tween<Float>()
        assertThat(tweenSpec.vectorize().isInfinite).isFalse()
        assertThat(tweenSpec.asAnimation().isInfinite).isFalse()
    }

    @Test
    fun testSnapIsFinite() {
        val snapSpec = snap<Float>()
        assertThat(snapSpec.vectorize().isInfinite).isFalse()
        assertThat(snapSpec.asAnimation().isInfinite).isFalse()
    }

    @Test
    fun testKeyFramesIsFinite() {
        val keyFramesSpec = keyframes<Float> { durationMillis = 100 }
        assertThat(keyFramesSpec.vectorize().isInfinite).isFalse()
        assertThat(keyFramesSpec.asAnimation().isInfinite).isFalse()
    }

    @Test
    fun testSpringIsFinite() {
        val springSpec = spring<Float>()
        val animation = springSpec.asAnimation()
        assertThat(springSpec.vectorize().isInfinite).isFalse()
        assertThat(animation.isInfinite).isFalse()
    }

    @Test
    fun testFiniteRepeatableIsFinite() {
        val spring = repeatable(10, tween<Float>())
        assertThat(spring.vectorize().isInfinite).isFalse()
        assertThat(spring.asAnimation().isInfinite).isFalse()
    }

    @Test
    fun testInfiniteRepeatableIsInfinite() {
        val spring = infiniteRepeatable(tween<Float>())
        assertThat(spring.vectorize().isInfinite).isTrue()
        assertThat(spring.asAnimation().isInfinite).isTrue()
    }

    @Test
    fun testExponentialDecayAnimationIsFinite() {
        val decaySpec = exponentialDecay<Float>()
        assertThat(decaySpec.asAnimation().isInfinite).isFalse()
    }

    @Test
    fun testDecayAnimationIsFinite() {
        val decaySpec = FloatExponentialDecaySpec()
        assertThat(decaySpec.asAnimation().isInfinite).isFalse()
    }

    private fun AnimationSpec<Float>.vectorize(): VectorizedAnimationSpec<AnimationVector1D> {
        return vectorize(Float.VectorConverter)
    }

    private fun AnimationSpec<Float>.asAnimation(): Animation<Float, AnimationVector1D> {
        return TargetBasedAnimation(vectorize(), Float.VectorConverter, 0f, 0f)
    }

    private fun DecayAnimationSpec<Float>.asAnimation(): Animation<Float, AnimationVector1D> {
        return DecayAnimation(this, Float.VectorConverter, 0f, 0f)
    }

    private fun FloatDecayAnimationSpec.asAnimation(): Animation<Float, AnimationVector1D> {
        return DecayAnimation(this, 0f)
    }
}
