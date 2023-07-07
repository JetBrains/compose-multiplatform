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

package androidx.testutils

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.Build
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * This rule allows test to control animation duration scale for tests.
 *
 * At the end of the test, it is reverted back to the original value that was read at the beginning
 * of the test.
 *
 * Use this class sparingly. It is bad to have tests run actual animations as they will both slow
 * down the test and also make it more likely to flake.
 */
abstract class AnimationDurationScaleRule : TestWatcher() {
    // this class is not an interface because project uses Java 7 so we cannot have static methods
    // in kotlin interfaces
    abstract fun setAnimationDurationScale(animationDurationScale: Float)

    companion object {
        /**
         * Creates a new [AnimationDurationScaleRule] rule that will apply to all tests in this
         * class.
         * If  the API level is less than 16, returns a no-op version.
         *
         * @param forcedAnimationDurationScale The new duration scale for all of the tests
         */
        @JvmStatic
        fun createForAllTests(
            forcedAnimationDurationScale: Float
        ) = create(forcedAnimationDurationScale)

        /**
         * Creates a new [AnimationDurationScaleRule] rule that will not apply to any tests unless
         * the test calls [AnimationDurationScaleRule.setAnimationDurationScale].
         *
         * If  the API level is less than 16, returns a no-op version.
         */
        @JvmStatic
        fun create() = this.create(null)

        internal fun create(
            forcedAnimationDurationScale: Float? = null
        ): AnimationDurationScaleRule {
            return if (Build.VERSION.SDK_INT >= 16) {
                AnimationDurationScaleRuleImpl(
                    forcedAnimationDurationScale
                )
            } else {
                NoOpAnimationDurationScaleRule()
            }
        }
    }
}

private class NoOpAnimationDurationScaleRule : AnimationDurationScaleRule() {
    override fun setAnimationDurationScale(animationDurationScale: Float) {
    }

    override fun apply(base: Statement, description: Description) = base
}

private class AnimationDurationScaleRuleImpl(
    /**
     * The new duration scale for the test
     */
    private val forcedAnimationDurationScale: Float?
) : AnimationDurationScaleRule() {
    /**
     * Reflect into the duration field and make it accessible.
     */
    private val durationSetter =
        ValueAnimator::class.java.getDeclaredMethod("setDurationScale", Float::class.java)
    @SuppressLint("DiscouragedPrivateApi")
    private val durationGetter =
        ValueAnimator::class.java.getDeclaredMethod("getDurationScale")

    /**
     * The duration scale at the beginning of the test so that we can re-use it later to reset.
     */
    private val originalDurationScale = durationGetter.invoke(null) as Float

    override fun setAnimationDurationScale(animationDurationScale: Float) {
        durationSetter.invoke(null, animationDurationScale)
    }

    private fun resetAnimationDuration() {
        durationSetter.invoke(null, originalDurationScale)
    }

    override fun starting(description: Description) {
        forcedAnimationDurationScale?.let {
            setAnimationDurationScale(it)
        }
    }

    override fun finished(description: Description) {
        resetAnimationDuration()
    }
}