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

package androidx.compose.animation

import androidx.compose.animation.core.AnimationClockObservable
import androidx.compose.animation.core.AnimationClockObserver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DisposableAnimationClockTest {
    /**
     * Test that disposing a [DisposableAnimationClock] disconnects observers
     * from the upstream [AnimationClockObservable].
     */
    @Test
    fun animationClockDisposal() {
        val fakeClock = FakeAnimationClock()
        val disposableClock = DisposableAnimationClock(fakeClock)

        val received = mutableListOf<Long>()
        val observer = object : AnimationClockObserver {
            override fun onAnimationFrame(frameTimeMillis: Long) {
                received.add(frameTimeMillis)
            }
        }

        disposableClock.subscribe(observer)

        fakeClock.dispatchAnimationFrame(100)
        fakeClock.dispatchAnimationFrame(200)
        disposableClock.dispose()

        assertTrue("disposable clock reports disposed", disposableClock.isDisposed)
        assertFalse("root clock has observers after dispose", fakeClock.hasObservers)

        fakeClock.dispatchAnimationFrame(300)
        fakeClock.dispatchAnimationFrame(400)

        assertEquals("observer received expected values", listOf(100L, 200L), received)
    }
}

private class FakeAnimationClock : AnimationClockObservable {
    private val observers = mutableListOf<AnimationClockObserver>()

    override fun subscribe(observer: AnimationClockObserver) {
        observers.add(observer)
    }

    override fun unsubscribe(observer: AnimationClockObserver) {
        observers.remove(observer)
    }

    val hasObservers: Boolean get() = observers.isNotEmpty()

    fun dispatchAnimationFrame(frameTimeMillis: Long) {
        for (observer in observers) {
            observer.onAnimationFrame(frameTimeMillis)
        }
    }
}