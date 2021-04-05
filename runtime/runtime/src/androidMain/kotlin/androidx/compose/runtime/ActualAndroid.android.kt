/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.runtime

import android.os.Looper
import android.view.Choreographer
import androidx.compose.runtime.snapshots.SnapshotMutableState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

internal actual object Trace {
    actual fun beginSection(name: String): Any? {
        android.os.Trace.beginSection(name)
        return null
    }

    actual fun endSection(token: Any?) {
        android.os.Trace.endSection()
    }
}

internal actual typealias CheckResult = androidx.annotation.CheckResult

/**
 * This is an inaccurate implementation that will only be used when running linked against
 * Android SDK stubs in host-side tests. A real implementation should synchronize with the
 * device's default display's vsync rate.
 */
private object SdkStubsFallbackFrameClock : MonotonicFrameClock {
    private const val DefaultFrameDelay = 16L // milliseconds

    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R =
        withContext(Dispatchers.Main) {
            delay(DefaultFrameDelay)
            onFrame(System.nanoTime())
        }
}

private object DefaultChoreographerFrameClock : MonotonicFrameClock {
    private val choreographer = runBlocking(Dispatchers.Main.immediate) {
        Choreographer.getInstance()
    }

    override suspend fun <R> withFrameNanos(
        onFrame: (frameTimeNanos: Long) -> R
    ): R = suspendCancellableCoroutine<R> { co ->
        val callback = Choreographer.FrameCallback { frameTimeNanos ->
            co.resumeWith(runCatching { onFrame(frameTimeNanos) })
        }
        choreographer.postFrameCallback(callback)
        co.invokeOnCancellation { choreographer.removeFrameCallback(callback) }
    }
}

// For local testing
private const val DisallowDefaultMonotonicFrameClock = false

@Deprecated(
    "MonotonicFrameClocks are not globally applicable across platforms. " +
        "Use an appropriate local clock."
)
actual val DefaultMonotonicFrameClock: MonotonicFrameClock by lazy {
    if (DisallowDefaultMonotonicFrameClock) error("Disallowed use of DefaultMonotonicFrameClock")

    // When linked against Android SDK stubs and running host-side tests, APIs such as
    // Looper.getMainLooper() that will never return null on a real device will return null.
    // This branch offers an alternative solution.
    if (Looper.getMainLooper() != null) DefaultChoreographerFrameClock
    else SdkStubsFallbackFrameClock
}

internal actual fun <T> createSnapshotMutableState(
    value: T,
    policy: SnapshotMutationPolicy<T>
): SnapshotMutableState<T> = ParcelableSnapshotMutableState(value, policy)
