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

package androidx.compose.ui.test

import androidx.compose.animation.core.InternalAnimationApi
import androidx.compose.animation.core.rootAnimationClockFactory
import androidx.test.espresso.IdlingResource
import androidx.compose.ui.test.android.AndroidTestAnimationClock
import androidx.compose.ui.test.android.registerTestClock
import androidx.compose.ui.test.android.unregisterTestClock
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [TestRule] to monitor and take over the animation clock in the composition. It substitutes
 * the ambient animation clock provided at the root of the composition tree with a
 * [TestAnimationClock] and registers it with [registerTestClock].
 *
 * Usually you don't need to create this rule by yourself, it is done for you in
 * [ComposeTestRule]. If you don't use [ComposeTestRule], use this rule in your test and make
 * sure it is run _before_ your activity is created.
 *
 * If your app provides a custom animation clock somewhere in your composition, make sure to have
 * it implement [TestAnimationClock] and register it with [registerTestClock]. Alternatively,
 * if you use Espresso you can create your own [IdlingResource] to let Espresso await your
 * animations. Otherwise, built in steps that make sure the UI is stable when performing actions
 * or assertions will fail to work.
 */
internal class AndroidAnimationClockTestRule : AnimationClockTestRule {

    /** Backing property for [clock] */
    private val _clock = AndroidTestAnimationClock()

    /**
     * The ambient animation clock that is provided at the root of the composition tree.
     * Typically, apps will only use this clock. If your app provides another clock in the tree,
     * make sure to let it implement [TestAnimationClock] and register it with
     * [registerTestClock].
     */
    override val clock: TestAnimationClock get() = _clock

    /**
     * Convenience property for calling [`clock.isPaused`][TestAnimationClock.isPaused]
     */
    override val isPaused: Boolean get() = clock.isPaused

    /**
     * Convenience method for calling [`clock.pauseClock()`][TestAnimationClock.pauseClock]
     */
    override fun pauseClock() = clock.pauseClock()

    /**
     * Convenience method for calling [`clock.resumeClock()`][TestAnimationClock.resumeClock]
     */
    override fun resumeClock() = clock.resumeClock()

    /**
     * Convenience method for calling [`clock.advanceClock()`][TestAnimationClock.advanceClock]
     */
    override fun advanceClock(milliseconds: Long) = clock.advanceClock(milliseconds)

    override fun apply(base: Statement, description: Description?): Statement {
        return AnimationClockStatement(base)
    }

    @OptIn(InternalAnimationApi::class)
    private inner class AnimationClockStatement(private val base: Statement) : Statement() {
        override fun evaluate() {
            val oldFactory = rootAnimationClockFactory
            registerTestClock(clock)
            rootAnimationClockFactory = { clock }
            try {
                base.evaluate()
            } finally {
                try {
                    _clock.dispose()
                } finally {
                    rootAnimationClockFactory = oldFactory
                    unregisterTestClock(clock)
                }
            }
        }
    }
}

actual fun createAnimationClockRule(): AnimationClockTestRule =
    AndroidAnimationClockTestRule()
