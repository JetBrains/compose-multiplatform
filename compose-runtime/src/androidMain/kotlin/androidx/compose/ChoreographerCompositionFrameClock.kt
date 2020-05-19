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

/**
 * A [CompositionFrameClock] driven by an [android.view.Choreographer].
 */
class ChoreographerCompositionFrameClock(
    val choreographer: Choreographer
) : CompositionFrameClock {
    override suspend fun <R> awaitFrameNanos(
        onFrame: (Long) -> R
    ): R {
        return suspendCancellableCoroutine { co ->
            val callback = ChoreographerFrameCallback { frameTimeNanos ->
                co.resumeWith(runCatching { onFrame(frameTimeNanos) })
            }
            choreographer.postFrameCallback(callback)
            co.invokeOnCancellation { choreographer.removeFrameCallback(callback) }
        }
    }
}
