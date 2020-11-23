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

import androidx.compose.animation.core.AnimationClockObservable
import androidx.compose.animation.core.InternalAnimationApi
import androidx.compose.animation.core.MonotonicFrameAnimationClock
import androidx.compose.animation.core.rootAnimationClockFactory
import androidx.compose.ui.test.ExperimentalTesting
import androidx.compose.ui.test.TestAnimationClock
import androidx.compose.ui.test.junit4.android.ComposeIdlingResource
import kotlinx.coroutines.CoroutineScope
import org.junit.runner.Description
import org.junit.runners.model.Statement

@ExperimentalTesting
internal class MonotonicFrameClockTestRule(
    private val composeIdlingResource: ComposeIdlingResource
) : AnimationClockTestRule {

    private lateinit var _clock: InternalClock
    override val clock: TestAnimationClock get() = _clock

    override fun apply(base: Statement, description: Description?): Statement {
        return AnimationClockStatement(base)
    }

    private fun getOrCreateClock(scope: CoroutineScope): TestAnimationClock {
        if (!this::_clock.isInitialized) {
            _clock = InternalClock(MonotonicFrameAnimationClock(scope))
            composeIdlingResource.registerTestClock(_clock)
        }
        return _clock
    }

    @OptIn(InternalAnimationApi::class)
    private inner class AnimationClockStatement(private val base: Statement) : Statement() {
        override fun evaluate() {
            val oldFactory = rootAnimationClockFactory
            rootAnimationClockFactory = { getOrCreateClock(it) }
            try {
                base.evaluate()
            } finally {
                rootAnimationClockFactory = oldFactory
                composeIdlingResource.unregisterTestClock(clock)
            }
        }
    }

    class InternalClock(
        private val clock: MonotonicFrameAnimationClock
    ) : TestAnimationClock, AnimationClockObservable by clock {
        override val isIdle: Boolean get() = !clock.hasObservers
        override val isPaused: Boolean get() = false

        override fun pauseClock(): Unit = throw UnsupportedOperationException()
        override fun resumeClock(): Unit = throw UnsupportedOperationException()
        override fun advanceClock(milliseconds: Long): Unit = throw UnsupportedOperationException()
    }
}
