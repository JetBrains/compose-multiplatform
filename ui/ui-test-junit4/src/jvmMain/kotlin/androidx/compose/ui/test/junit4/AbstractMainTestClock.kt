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

package androidx.compose.ui.test.junit4

import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.MainTestClock
import kotlin.math.ceil
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler

@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class AbstractMainTestClock(
    private val testScheduler: TestCoroutineScheduler,
    private val frameDelayMillis: Long,
    private val runOnUiThread: (action: () -> Unit) -> Unit
) : MainTestClock {

    override val currentTime: Long
        get() = testScheduler.currentTime

    override var autoAdvance: Boolean = true

    override fun advanceTimeByFrame() {
        advanceDispatcher(frameDelayMillis)
    }

    override fun advanceTimeBy(milliseconds: Long, ignoreFrameDuration: Boolean) {
        val actualDelay = if (ignoreFrameDuration) {
            milliseconds
        } else {
            ceil(
                milliseconds.toDouble() / frameDelayMillis
            ).toLong() * frameDelayMillis
        }
        advanceDispatcher(actualDelay)
    }

    override fun advanceTimeUntil(timeoutMillis: Long, condition: () -> Boolean) {
        val startTime = currentTime
        runOnUiThread {
            while (!condition()) {
                advanceDispatcher(frameDelayMillis)
                if (currentTime - startTime > timeoutMillis) {
                    throw ComposeTimeoutException(
                        "Condition still not satisfied after $timeoutMillis ms"
                    )
                }
            }
        }
    }

    private fun advanceDispatcher(millis: Long) {
        runOnUiThread {
            testScheduler.advanceTimeBy(millis)

            // Since coroutines 1.6.0
            // `advanceTimeBy` doesn't run the tasks that are scheduled at exactly
            // `currentTime + delayTimeMillis`. See `advanceTimeBy`.
            // Therefore we also call `runCurrent` as it's done in TestCoroutineDispatcher
            testScheduler.runCurrent()
        }
    }
}