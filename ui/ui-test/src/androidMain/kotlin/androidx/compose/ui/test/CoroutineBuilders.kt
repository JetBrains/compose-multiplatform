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

import androidx.compose.animation.core.ManualFrameClock
import androidx.compose.animation.core.MonotonicFrameAnimationClock
import androidx.compose.animation.core.advanceClockMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

/**
 * Runs a new coroutine and blocks the current thread interruptibly until it completes, passing a
 * new [ManualFrameClock] to the code [block]. This is intended to be used by tests instead of
 * [runBlocking] if they want to use a [ManualFrameClock].
 *
 * The clock will start at time 0L and should be driven manually from your test, from the
 * [main dispatcher][TestUiDispatcher.Main]. Pass the clock to the animation that you want to
 * control in your test, and then [advance][advanceClockMillis] it as necessary. After the block
 * has completed, the clock will be forwarded with 10 second increments until it has drained all
 * work that took frames from that clock. If the work never ends, this function never ends, so
 * make sure that all animations driven by this clock are finite.
 *
 * For example:
 * ```
 * @Test
 * fun myTest() = runWithManualClock { clock ->
 *     // set some compose content
 *     testRule.setContent {
 *         MyAnimation(animationClock = clock)
 *     }
 *     // advance the clock by 1 second
 *     withContext(TestUiDispatcher.Main) {
 *         clock.advanceClock(1000L)
 *     }
 *     // await composition(s)
 *     waitForIdle()
 *     // check if the animation is finished or not
 *     if (clock.hasAwaiters) {
 *         println("The animation is still running")
 *     } else {
 *         println("The animation is done")
 *     }
 * }
 * ```
 * Here, `MyAnimation` is an animation that takes frames from the `animationClock` passed to it.
 *
 * It is good practice to add the animation clock to the parameters of an animation state to
 * improve testability. For example, [DrawerState][androidx.compose.material.DrawerState] accepts
 * an animation clock in the form of [AnimationClockObservable][androidx.compose.animation.core
 * .AnimationClockObservable]. Wrap the [ManualFrameClock] in a [MonotonicFrameAnimationClock]
 * and pass the wrapped clock if you want to manually drive such animations.
 *
 * @param compatibleWithManualAnimationClock If set to `true`, and this clock is used in a
 * [MonotonicFrameAnimationClock], will make the MonotonicFrameAnimationClock behave the same
 * as [ManualAnimationClock][androidx.compose.animation.core.ManualAnimationClock] and send the
 * first frame immediately upon subscription. Avoid reliance on this if possible. `false` by
 * default.
 */
@ExperimentalTesting
fun <R> runBlockingWithManualClock(
    compatibleWithManualAnimationClock: Boolean = false,
    block: suspend CoroutineScope.(clock: ManualFrameClock) -> R
) {
    @Suppress("DEPRECATION")
    val clock = ManualFrameClock(0L, compatibleWithManualAnimationClock)
    return runBlocking(clock) {
        block(clock)
        while (clock.hasAwaiters) {
            clock.advanceClockMillis(10_000L)
            // Give awaiters the chance to await again
            yield()
        }
    }
}