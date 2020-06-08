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

// TODO(b/158105080): make part of ComposeRuntime
@file:OptIn(InternalComposeApi::class)
package androidx.compose

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.suspendCancellableCoroutine

private class SuspendingEffect(
    private val recomposer: Recomposer,
    private val block: suspend CompositionCoroutineScope.() -> Unit
) : CompositionLifecycleObserver {

    private var job: Job? = null

    override fun onEnter() {
        job?.cancel("Old job was still running!")
        job = recomposer.launchEffect(block)
    }

    override fun onLeave() {
        job?.cancel()
        job = null
    }
}

/**
 * A [CoroutineScope] used for launching [side effects][launchInComposition] of a composition
 * that also permits [awaiting][CompositionFrameClock.withFrameNanos] the next presentation
 * frame of the composition. This can be useful for performing the next action of an animation
 * while the effect is still present in the composition.
 */
// TODO Make this an interface once it doesn't experience compiler issues
abstract class CompositionCoroutineScope : CoroutineScope, CompositionFrameClock {
    // This method deliberately shadows the awaitFrame method from kotlinx-coroutines-android
    // to redirect usage to the CompositionFrameClock API in effect blocks.
    @Suppress("RedundantSuspendModifier")
    @Deprecated(
        "use CompositionFrameClock.awaitFrameNanos to await a composition frame",
        replaceWith = ReplaceWith("awaitFrameNanos()", "androidx.compose.awaitFrameNanos"),
        level = DeprecationLevel.ERROR
    )
    suspend fun awaitFrame(): Long = withFrameNanos { it }
}

/**
 * Suspends the current coroutine until the effect is **disposed** and the
 * [CompositionCoroutineScope] is cancelled, and invokes [onDispose] before resuming.
 * [awaitDispose] never resumes normally and will always throw either
 * [kotlinx.coroutines.CancellationException] or the exception that failed the current
 * [kotlinx.coroutines.Job].
 */
suspend fun CompositionCoroutineScope.awaitDispose(onDispose: () -> Unit = {}): Nothing = try {
    suspendCancellableCoroutine { /* Suspend until cancellation */ }
} finally {
    onDispose()
}

/**
 * Launch a suspending side effect when this composition is committed and cancel it
 * when [launchInComposition] leaves the composition. [block] will run in the **apply** scope of the
 * composition's [Recomposer], which is usually your UI's main thread.
 *
 * [block] will be launched **once** when this call enters the composition; recomposition will not
 * cause [block] to launch again. To re-launch a suspend function when inputs change, see the
 * other overloads of [launchInComposition] that accept input value parameters.
 */
@Composable
fun launchInComposition(
    block: suspend CompositionCoroutineScope.() -> Unit
) {
    @OptIn(ExperimentalComposeApi::class)
    val recomposer = currentComposer.recomposer
    remember { SuspendingEffect(recomposer, block) }
}

/**
 * Launch a suspending side effect when this composition is committed and cancel it
 * when [launchInComposition] leaves the composition. If [v1] has changed since the last
 * recomposition, cancel the currently running [block] and launch again. [block] will run in the
 * **apply** scope of the composition's [Recomposer], which is usually your UI's main thread.
 */
@Composable
fun launchInComposition(
    v1: Any?,
    block: suspend CompositionCoroutineScope.() -> Unit
) {
    @OptIn(ExperimentalComposeApi::class)
    val recomposer = currentComposer.recomposer
    remember(v1) { SuspendingEffect(recomposer, block) }
}

/**
 * Launch a suspending side effect when this composition is committed and cancel it
 * when [launchInComposition] leaves the composition. If [v1] or [v2] has changed since the last
 * recomposition, cancel the currently running [block] and launch again. By default [block] will
 * run in the **apply** scope of the composition's [Recomposer], which is usually your UI's main
 * thread.
 */
@Composable
fun launchInComposition(
    v1: Any?,
    v2: Any?,
    block: suspend CompositionCoroutineScope.() -> Unit
) {
    @OptIn(ExperimentalComposeApi::class)
    val recomposer = currentComposer.recomposer
    remember(v1, v2) { SuspendingEffect(recomposer, block) }
}

/**
 * Launch a suspending side effect when this composition is committed and cancel it
 * when [launchInComposition] leaves the composition. If [v1], [v2] or [v3] has changed since the
 * last recomposition, cancel the currently running [block] and launch again. By default [block]
 * will run in the **apply** scope of the composition's [Recomposer], which is usually your UI's
 * main thread.
 */
@Composable
fun launchInComposition(
    v1: Any?,
    v2: Any?,
    v3: Any?,
    block: suspend CompositionCoroutineScope.() -> Unit
) {
    @OptIn(ExperimentalComposeApi::class)
    val recomposer = currentComposer.recomposer
    remember(v1, v2, v3) { SuspendingEffect(recomposer, block) }
}

/**
 * Launch a suspending side effect when this composition is committed and cancel it
 * when [launchInComposition] leaves the composition. If [keys] have changed since the last
 * recomposition, cancel the currently running [block] and launch again. By default [block] will
 * run in the **apply** scope of the composition's [Recomposer], which is usually your UI's main
 * thread.
 */
@Composable
fun launchInComposition(
    vararg keys: Any?,
    block: suspend CompositionCoroutineScope.() -> Unit
) {
    @OptIn(ExperimentalComposeApi::class)
    val recomposer = currentComposer.recomposer
    remember(*keys) { SuspendingEffect(recomposer, block) }
}