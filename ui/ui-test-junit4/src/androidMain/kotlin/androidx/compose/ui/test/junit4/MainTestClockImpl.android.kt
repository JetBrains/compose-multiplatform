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

import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.MainTestClock
import androidx.compose.ui.test.TestMonotonicFrameClock
import androidx.compose.ui.test.frameDelayMillis
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlin.math.ceil

@OptIn(ExperimentalCoroutinesApi::class)
internal class MainTestClockImpl(
    private val testDispatcher: TestCoroutineDispatcher,
    private val frameClock: TestMonotonicFrameClock
) : MainTestClock {

    override val currentTime: Long
        get() = testDispatcher.currentTime

    override var autoAdvance: Boolean = true

    internal val hasAwaiters = frameClock.hasAwaiters

    override fun advanceTimeByFrame() {
        advanceDispatcher(frameClock.frameDelayMillis)
    }

    override fun advanceTimeBy(milliseconds: Long, ignoreFrameDuration: Boolean) {
        val actualDelay = if (ignoreFrameDuration) {
            milliseconds
        } else {
            ceil(
                milliseconds.toDouble() / frameClock.frameDelayMillis
            ).toLong() * frameClock.frameDelayMillis
        }
        advanceDispatcher(actualDelay)
    }

    override fun advanceTimeUntil(timeoutMillis: Long, condition: () -> Boolean) {
        val startTime = currentTime
        runOnUiThread {
            while (!condition()) {
                advanceDispatcher(frameClock.frameDelayMillis)
                if (currentTime - startTime > timeoutMillis) {
                    throw ComposeTimeoutException(
                        "Condition still not satisfied after $timeoutMillis ms"
                    )
                }
            }
        }
    }

    internal fun advanceDispatcher(millis: Long) {
        runOnUiThread {
            testDispatcher.advanceTimeBy(millis)
        }
    }
}