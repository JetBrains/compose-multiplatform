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

package androidx.compose.animation.core

import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

@RunWith(JUnit4::class)
class AnimationClockTest {
    private lateinit var clock: ManualAnimationClock

    @Before
    fun setup() {
        clock = ManualAnimationClock(0L)
    }

    @Test
    fun testRemovalBeforeAdd() {
        val recordedFrameTimes = mutableListOf<Long>()
        val observer = object : AnimationClockObserver {
            override fun onAnimationFrame(frameTimeMillis: Long) {
                recordedFrameTimes.add(frameTimeMillis)
            }
        }

        clock.clockTimeMillis = 0L
        clock.clockTimeMillis = 1L
        clock.unsubscribe(observer)
        clock.subscribe(observer)
        // observer should record 1L
        clock.clockTimeMillis = 2L
        // observer should record 2L
        clock.unsubscribe(observer)
        clock.clockTimeMillis = 3L
        clock.clockTimeMillis = 4L
        clock.subscribe(observer)
        // observer should record 4L
        clock.clockTimeMillis = 5L
        // observer should record 5L
        clock.clockTimeMillis = 6L
        // observer should record 6L
        clock.unsubscribe(observer)
        clock.clockTimeMillis = 7L
        clock.subscribe(observer)
        clock.unsubscribe(observer)
        clock.unsubscribe(observer)
        clock.subscribe(observer)
        clock.clockTimeMillis = 8L

        val expectedRecording = listOf(1L, 2L, 4L, 5L, 6L, 7L, 7L, 8L)
        assertEquals(expectedRecording, recordedFrameTimes)
    }

    @Test
    fun testResubscriptionObserverOrder() {
        var callIndex = 0

        var testACallIndex = -1
        var testBCallIndex = -1
        var testCCallIndex = -1

        val testObserverA = testObserver {
            testACallIndex = callIndex++
        }
        val testObserverB = testObserver {
            testBCallIndex = callIndex++
        }
        val testObserverC = testObserver {
            testCCallIndex = callIndex++
        }

        clock.subscribe(testObserverA)
        clock.subscribe(testObserverB)
        clock.subscribe(testObserverC)

        clock.clockTimeMillis = 1L

        // Starts at 3 since subscribe calls callback in ManualAnimationClock
        assertEquals(3, testACallIndex)
        assertEquals(4, testBCallIndex)
        assertEquals(5, testCCallIndex)

        clock.unsubscribe(testObserverB)
        clock.subscribe(testObserverB)

        clock.clockTimeMillis = 2L

        assertEquals(7, testACallIndex)
        assertEquals(9, testBCallIndex)
        assertEquals(8, testCCallIndex)
    }
}

private fun ignoreFirstFrameObserver(block: (Long) -> Unit): AnimationClockObserver {
    return object : IgnoreFirstFrameObserver() {
        override fun onNonFirstAnimationFrame(frameTimeMillis: Long) {
            block(frameTimeMillis)
        }
    }
}

private abstract class IgnoreFirstFrameObserver : AnimationClockObserver {
    abstract fun onNonFirstAnimationFrame(frameTimeMillis: Long)
    private var firstFrameSkipped = false
    override fun onAnimationFrame(frameTimeMillis: Long) {
        if (firstFrameSkipped) {
            onNonFirstAnimationFrame(frameTimeMillis)
        } else {
            firstFrameSkipped = true
        }
    }
}

private fun testObserver(onAnimationFrame: (Long) -> Unit = {}): AnimationClockObserver =
    object : AnimationClockObserver {
        override fun onAnimationFrame(frameTimeMillis: Long) {
            onAnimationFrame.invoke(frameTimeMillis)
        }
    }

private fun blockWithContinuation(block: (Continuation<Unit>) -> Unit) = runBlocking {
    suspendCoroutine(block)
}