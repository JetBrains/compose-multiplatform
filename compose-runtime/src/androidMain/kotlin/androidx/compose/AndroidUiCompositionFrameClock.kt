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

package androidx.compose

import android.view.Choreographer
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.coroutineContext

/**
 * A [CompositionFrameClock] driven by an [android.view.Choreographer].
 */
class AndroidUiCompositionFrameClock(
    val choreographer: Choreographer
) : CompositionFrameClock {

    override suspend fun <R> awaitFrameNanos(
        onFrame: (Long) -> R
    ): R {
        val uiDispatcher = coroutineContext[ContinuationInterceptor] as? AndroidUiDispatcher
        return suspendCancellableCoroutine { co ->
            // Important: this callback won't throw, and AndroidUiDispatcher counts on it.
            val callback = ChoreographerFrameCallback { frameTimeNanos ->
                co.resumeWith(runCatching { onFrame(frameTimeNanos) })
            }

            // If we're on an AndroidUiDispatcher then we post callback to happen *after*
            // the greedy trampoline dispatch is complete.
            // This means that onFrame will run on the current choreographer frame if one is
            // already in progress, but awaitFrameNanos will *not* resume until the frame
            // is complete. This prevents multiple calls to awaitFrameNanos immediately dispatching
            // on the same frame.

            if (uiDispatcher != null && uiDispatcher.choreographer == choreographer) {
                uiDispatcher.postFrameCallback(callback)
                co.invokeOnCancellation { uiDispatcher.removeFrameCallback(callback) }
            } else {
                choreographer.postFrameCallback(callback)
                co.invokeOnCancellation { choreographer.removeFrameCallback(callback) }
            }
        }
    }
}
