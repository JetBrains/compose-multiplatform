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

package androidx.compose.runtime

/**
 * A [MonotonicFrameClock] wrapper that can be [pause]d and [resume]d.
 *
 * A paused clock will not dispatch [withFrameNanos] events until it is resumed.
 * Pausing a clock does **not** stop or change the frame times reported to [withFrameNanos] calls;
 * the clock times reported will always remain consistent with [frameClock].
 *
 * [PausableMonotonicFrameClock] should be used in cases where frames should not be produced
 * under some conditions, such as when a window hosting a UI is not currently visible.
 * As clock times are not altered from the source [frameClock], animations in progress may
 * be fully complete by the time the clock is resumed and a new frame is produced.
 */
class PausableMonotonicFrameClock(
    private val frameClock: MonotonicFrameClock
) : MonotonicFrameClock {
    private val latch = Latch()

    /**
     * `true` if this clock is currently [paused][pause] or `false` if this clock is currently
     * [resumed][resume]. A PausableMonotonicFrameClock is not paused at construction time.
     */
    val isPaused: Boolean
        get() = !latch.isOpen

    /**
     * Pause the generation of frames. Pausing a clock that is already paused has no effect.
     * While the clock is paused any calls to [withFrameNanos] will suspend until the clock is
     * resumed before delegating to the wrapped [frameClock]'s [withFrameNanos] method.
     * Call [resume] to resume generating frames.
     */
    fun pause() {
        latch.closeLatch()
    }

    /**
     * Resume the generation of frames. Any queued calls to [withFrameNanos] will resume and
     * delegate to the wrapped [frameClock]'s [withFrameNanos] method.
     */
    fun resume() {
        latch.openLatch()
    }

    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
        latch.await()
        return frameClock.withFrameNanos(onFrame)
    }
}
