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

package androidx.compose.ui.platform

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.yield
import kotlin.coroutines.CoroutineContext

/**
 * Dispatch frame after call of [scheduleFrame]
 *
 * Frames executed not more frequent than [framesPerSecond]
 */
class FrameDispatcher(
    private val onFrame: suspend (nanoTime: Long) -> Unit,
    private val framesPerSecond: () -> Float,
    private val nanoTime: () -> Long = System::nanoTime,
    context: CoroutineContext = Dispatchers.Swing
) {
    private var needFrame = CompletableDeferred<Unit>()

    private val job = GlobalScope.launch(context) {
        while (true) {
            needFrame.await()
            needFrame = CompletableDeferred()

            val frameNanoTime = nanoTime()
            onFrame(frameNanoTime)

            val elapsed = nanoTime() - frameNanoTime
            val refreshRate = framesPerSecond()
            val singleFrameNanos = (1_000_000_000 / refreshRate).toLong()
            val needToWaitMillis = maxOf(0, singleFrameNanos - elapsed) / 1_000_000
            delayOrYield(needToWaitMillis)
        }
    }

    private suspend fun delayOrYield(millis: Long) {
        if (millis > 0) {
            delay(millis)
        } else {
            yield()
        }
    }

    fun cancel() {
        job.cancel()
    }

    fun scheduleFrame() {
        needFrame.complete(Unit)
    }
}