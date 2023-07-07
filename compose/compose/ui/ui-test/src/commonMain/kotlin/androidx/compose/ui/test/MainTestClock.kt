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

package androidx.compose.ui.test

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.internal.JvmDefaultWithCompatibility

/**
 * The clock that drives [frames][MonotonicFrameClock.withFrameNanos],
 * [recompositions][Recomposer] and [launched effects][LaunchedEffect] in compose tests.
 *
 * This clock is ultimately responsible for driving all recompositions, all subscribers to
 * [withFrameNanos][MonotonicFrameClock.withFrameNanos] (all compose animations) and all
 * coroutines launched with [LaunchedEffect][LaunchedEffect] (for example gesture detection). It
 * is important to realize that if this clock does not tick, recomposition will not happen and
 * animations are frozen. Equally important to realize is that measure, layout and draw passes are
 * _not_ driven by this clock. Instead, they are driven by the event loop of the platform, for
 * example the Choreographer on Android. That means that forwarding this clock will _not_
 * perform a measure, layout or draw pass, and vice versa, when this clock is paused measure,
 * layout and draw passes can still occur.
 *
 * Therefore, when setting [autoAdvance] to `false` and taking control over this clock, there are
 * several things to realize:
 * * Recomposition can only happen when a frame is produced by this clock, with one exception: the
 *   initial composition when calling setContent happens immediately.
 * * Callers of [withFrameNanos][MonotonicFrameClock.withFrameNanos] can only get a frame time when
 *   a frame is produced by this clock.
 * * If there is both a pending recomposition and an animation awaiting a
 *   [frame time][MonotonicFrameClock.withFrameNanos], ticking this clock will _first_ send the
 *   new frame time to the animation, and _then_ perform recomposition. Any state changes made by
 *   the animation will be seen by the recomposition.
 * * Because animations receive their [frame time][MonotonicFrameClock.withFrameNanos] _before_
 *   recomposition, an animation will not get its start time in the first frame after kicking it
 *   off by toggling a state variable. For example, with a frame time of 16ms; when you call
 *   [advanceTimeBy(32)][advanceTimeBy] after you toggled a state variable to kick off an
 *   animation, the animation's play time will still be at 0ms.
 *   The first frame is produced when the clock has advanced 16ms and will run a recomposition.
 *   During that recomposition the animation will be scheduled to start. When the clock has
 *   advanced another 16ms, the animation gets its first frame time and initialize the play time
 *   to `t=0`.
 * * Because animations request the next [frame][MonotonicFrameClock.withFrameNanos] during the
 *   current frame, calling [advanceTimeBy(160)][advanceTimeBy] while an animation is running will
 *   produce 10 frames of 16ms rather than 1 frame of 160ms (assuming a frame time of 16ms).
 *   Measure, layout and draw will not happen in between these frames. Be aware that some
 *   animations, like a slideIn or slideOut animation, are set up during a layout pass. If you
 *   start such an animation and advance time by two frames or more without allowing for a layout
 *   pass to happen, it will end immediately because it will be started before it is set up. For
 *   example, here you see how you can control a slide out animation:
 *
 * @sample androidx.compose.ui.test.samples.testSlideOut
 *
 * * After modifying a state variable, recomposition needs to happen to reflect the new state in
 *   the UI. Advancing the clock by [one frame][advanceTimeByFrame] will commit the changes and
 *   run exactly one recomposition.
 * * If, after any call to [advanceTimeBy], you want to assert anything related to layout
 *   positions (e.g. [assertWidthIsEqualTo]) or rendering (e.g. [captureToImage]), you will need a
 *   call to [waitForIdle][androidx.compose.ui.test.junit4.ComposeTestRule.waitForIdle] or
 *   [runOnIdle][androidx.compose.ui.test.junit4.ComposeTestRule.runOnIdle] to make sure that any
 *   triggered measure, layout or draw pass has been completed.
 * * If you change a state variable that is not read during composition, but for example during
 *   layout or draw, calling [advanceTimeBy] will not produce the desired update to the UI. Use
 *   [waitForIdle][androidx.compose.ui.test.junit4.ComposeTestRule.waitForIdle] for such cases.
 * * [delayed][kotlinx.coroutines.delay] [LaunchedEffect]s are resumed on their scheduled time.
 *   That means that code like `repeat(2) { delay(1000) }` will complete with a single call to
 *   [advanceTimeBy(2000)][advanceTimeBy].
 * * After modifying a state variable, the modified [snapshot][Snapshot] must be
 *   [committed][Snapshot.sendApplyNotifications] before the compositions that read that variable
 *   are invalidated. This is currently not done by the test harness, but by a platform dependent
 *   implementation. On Android, for example, a message is posted on the main thread to call
 *   `sendApplyNotifications` when a state variable is written (which conveniently runs before
 *   `advanceTimeByFrame` on Android), which means that if the variable is written during a call
 *   to [advanceTimeBy], the composition will only be invalidated after `advanceTimeBy` has
 *   finished, regardless of the time by which you advanced the clock. You may call
 *   `sendApplyNotifications` manually after modifying a state variable to invalidate the
 *   composition and force a recomposition within 16ms of the current clock time. For example,
 *   here you see how to use `sendApplyNotifications` and `advanceTimeBy`:
 *
 * @sample androidx.compose.ui.test.samples.testControlClock
 */
@JvmDefaultWithCompatibility
interface MainTestClock {
    /**
     * The current time of this clock in milliseconds.
     */
    val currentTime: Long

    /**
     * Whether the clock should be advanced by the testing framework while awaiting idleness in
     * order to process any pending work that is driven by this clock. This ensures that when the
     * app is [idle][androidx.compose.ui.test.junit4.ComposeTestRule.waitForIdle], there are no
     * more pending recompositions or ongoing animations.
     *
     * If [autoAdvance] is false, the clock is not advanced while awaiting idleness. Moreover,
     * having pending recompositions or animations is not taken as a sign of pending work
     * (non-idleness) when awaiting idleness, as waiting for a longer time will not make them
     * happen. Note that pending measure, layout or draw passes will still be awaited when
     * awaiting idleness and having [autoAdvance] set to false, as those passes are not driven
     * by this clock.
     *
     * By default this is true.
     */
    var autoAdvance: Boolean

    /**
     * [Advances][advanceTimeBy] the main clock by the duration of one frame.
     */
    fun advanceTimeByFrame()

    /**
     * Advances the clock by the given [duration][milliseconds]. The duration is rounded up to the
     * nearest multiple of the frame duration by default to always produce the same number of
     * frames regardless of the current time of the clock. Use [ignoreFrameDuration] to disable
     * this behavior. The frame duration is platform dependent. For example, on a JVM (Android and
     * Desktop) it is 16ms. Note that if [ignoreFrameDuration] is true, the last few milliseconds
     * that are advanced might not be observed by anyone, since most processes are only triggered
     * when a frame is produced.
     *
     * When using this method to advance the time by several frames in one invocation, measure,
     * layout and draw passes will not happen in between the produced frames. Multiple frames are
     * in general only produced when an animation is running. See [MainTestClock] for a more in
     * depth explanation of the behavior of your test when controlling the clock.
     *
     * It is recommended to set [autoAdvance] to false when using this method, but it is not
     * strictly necessary. When [autoAdvance] is true, using this method may or may not speed up
     * your test.
     *
     * @param milliseconds The minimal duration to advance the main clock by. Will be rounded up
     * to the nearest frame duration, unless [ignoreFrameDuration] is `true`.
     * @param ignoreFrameDuration Whether to avoid rounding up the [milliseconds] to the nearest
     * multiple of the frame duration. `false` by default.
     */
    fun advanceTimeBy(milliseconds: Long, ignoreFrameDuration: Boolean = false)

    /**
     * Advances the clock in increments of a [single frame][advanceTimeByFrame] until the given
     * [condition] is satisfied.
     *
     * Note that the condition should only rely on things that are driven by this clock. Measure,
     * layout and draw passes will not happen in between advancements of the clock while waiting
     * for the condition to become true. If your condition relies on the result of measure, layout
     * or draw, use [waitUntil][androidx.compose.ui.test.junit4.ComposeTestRule.waitUntil] instead.
     *
     * See [MainTestClock] for a thorough explanation of what is and what isn't going to happen as
     * a result of a call to `advanceTimeBy`.
     *
     * @param timeoutMillis The time after which this method throws an exception if the given
     * condition is not satisfied. This is the simulated time not the wall clock or cpu time.
     *
     * @throws ComposeTimeoutException the condition is not satisfied after [timeoutMillis].
     */
    fun advanceTimeUntil(timeoutMillis: Long = 1_000, condition: () -> Boolean)
}

/**
 * Thrown in cases where Compose test can't satisfy a condition in a defined time limit.
 */
class ComposeTimeoutException(message: String?) : Throwable(message)