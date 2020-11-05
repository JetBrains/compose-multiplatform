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

package androidx.ui.benchmark.test

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.animation.core.AnimationClockObservable
import androidx.compose.animation.core.AnimationClockObserver
import androidx.compose.animation.core.ManualAnimationClock
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@LargeTest
@RunWith(AndroidJUnit4::class)
class AnimationClockBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun benchmarkDegenerateCase() {
        val clock = ManualAnimationClock(0L)

        val observers = (0..200).map {
            if (it % 2 == 0)
                TestObserver()
            else
                ResubscribeObserver(clock)
        }

        observers.forEach {
            clock.subscribe(it)
        }

        var i = 1L
        benchmarkRule.measureRepeated {
            clock.clockTimeMillis = i++
        }
    }

    @Test
    fun benchmarkIdealCase() {
        val clock = ManualAnimationClock(0L)

        val observers = (0..300).map {
            TestObserver()
        }

        observers.forEach {
            clock.subscribe(it)
        }

        var i = 1L
        benchmarkRule.measureRepeated {
            clock.clockTimeMillis = i++
        }
    }
}

private class TestObserver : AnimationClockObserver {
    override fun onAnimationFrame(frameTimeMillis: Long) {}
}

private class ResubscribeObserver(val clock: AnimationClockObservable) : AnimationClockObserver {
    private var resubbedFrame = 0L
    override fun onAnimationFrame(frameTimeMillis: Long) {
        if (resubbedFrame != frameTimeMillis) {
            resubbedFrame = frameTimeMillis
            clock.unsubscribe(this)
            clock.subscribe(this)
        }
    }
}