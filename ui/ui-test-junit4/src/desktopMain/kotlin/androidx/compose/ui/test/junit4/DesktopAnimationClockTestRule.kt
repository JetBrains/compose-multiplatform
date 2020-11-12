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

package androidx.compose.ui.test.junit4

import androidx.compose.animation.core.AnimationClockObserver
import androidx.compose.animation.core.InternalAnimationApi
import androidx.compose.ui.test.TestAnimationClock
import org.junit.runner.Description
import org.junit.runners.model.Statement

internal class DesktopTestAnimationClock : TestAnimationClock {
    override val isIdle: Boolean
        get() = TODO("Not yet implemented")

    override fun pauseClock() {
        TODO("Not yet implemented")
    }

    override fun resumeClock() {
        TODO("Not yet implemented")
    }

    override val isPaused: Boolean
        get() = TODO("Not yet implemented")

    override fun advanceClock(milliseconds: Long) {
        TODO("Not yet implemented")
    }

    override fun subscribe(observer: AnimationClockObserver) {
        TODO("Not yet implemented")
    }

    override fun unsubscribe(observer: AnimationClockObserver) {
        TODO("Not yet implemented")
    }
}

internal class DesktopAnimationClockTestRule : AnimationClockTestRule {

    override val clock: TestAnimationClock get() = DesktopTestAnimationClock()

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
            base.evaluate()
        }
    }
}

actual fun createAnimationClockRule(): AnimationClockTestRule =
    DesktopAnimationClockTestRule()