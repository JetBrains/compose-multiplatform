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
package androidx.compose.runtime

import androidx.compose.runtime.dispatch.MonotonicFrameClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

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
 * that also permits [awaiting][MonotonicFrameClock.withFrameNanos] the next presentation
 * frame of the composition. This can be useful for performing the next action of an animation
 * while the effect is still present in the composition.
 */
// TODO Make this an interface once it doesn't experience compiler issues
abstract class CompositionCoroutineScope : CoroutineScope, MonotonicFrameClock {
    // This method deliberately shadows the awaitFrame method from kotlinx-coroutines-android
    // to redirect usage to the CompositionFrameClock API in effect blocks.
    @Suppress("RedundantSuspendModifier")
    @Deprecated(
        "use CompositionFrameClock.awaitFrameNanos to await a composition frame",
        replaceWith = ReplaceWith("awaitFrameNanos()", "androidx.compose.runtime.awaitFrameNanos"),
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
 * when [launchInComposition] leaves the composition. If [key] has changed since the last
 * recomposition, cancel the currently running [block] and launch again. [block] will run in the
 * **apply** scope of the composition's [Recomposer], which is usually your UI's main thread.
 *
 * This function should **not** be used to (re-)launch ongoing tasks in response to callback
 * events by way of storing callback data in [MutableState] passed to [key]. Instead, see
 * [rememberCoroutineScope] to obtain a [CoroutineScope] that may be used to launch ongoing jobs
 * scoped to the composition in response to event callbacks.
 */
@Composable
fun launchInComposition(
    key: Any?,
    block: suspend CompositionCoroutineScope.() -> Unit
) {
    @OptIn(ExperimentalComposeApi::class)
    val recomposer = currentComposer.recomposer
    remember(key) { SuspendingEffect(recomposer, block) }
}

/**
 * Launch a suspending side effect when this composition is committed and cancel it
 * when [launchInComposition] leaves the composition. If [key1] or [key2] has changed since the last
 * recomposition, cancel the currently running [block] and launch again. By default [block] will
 * run in the **apply** scope of the composition's [Recomposer], which is usually your UI's main
 * thread.
 *
 * This function should **not** be used to (re-)launch ongoing tasks in response to callback
 * events by way of storing callback data in [MutableState] passed to [key1] or [key2]. Instead, see
 * [rememberCoroutineScope] to obtain a [CoroutineScope] that may be used to launch ongoing jobs
 * scoped to the composition in response to event callbacks.
 */
@Composable
fun launchInComposition(
    key1: Any?,
    key2: Any?,
    block: suspend CompositionCoroutineScope.() -> Unit
) {
    @OptIn(ExperimentalComposeApi::class)
    val recomposer = currentComposer.recomposer
    remember(key1, key2) { SuspendingEffect(recomposer, block) }
}

/**
 * Launch a suspending side effect when this composition is committed and cancel it
 * when [launchInComposition] leaves the composition. If [key1], [key2] or [key3] has changed since
 * the last recomposition, cancel the currently running [block] and launch again. By default [block]
 * will run in the **apply** scope of the composition's [Recomposer], which is usually your UI's
 * main thread.
 *
 * This function should **not** be used to (re-)launch ongoing tasks in response to callback
 * events by way of storing callback data in [MutableState] passed to [key1], [key2] or [key3].
 * Instead, see [rememberCoroutineScope] to obtain a [CoroutineScope] that may be used to launch
 * ongoing jobs scoped to the composition in response to event callbacks.
 */
@Composable
fun launchInComposition(
    key1: Any?,
    key2: Any?,
    key3: Any?,
    block: suspend CompositionCoroutineScope.() -> Unit
) {
    @OptIn(ExperimentalComposeApi::class)
    val recomposer = currentComposer.recomposer
    remember(key1, key2, key3) { SuspendingEffect(recomposer, block) }
}

/**
 * Launch a suspending side effect when this composition is committed and cancel it
 * when [launchInComposition] leaves the composition. If [keys] have changed since the last
 * recomposition, cancel the currently running [block] and launch again. By default [block] will
 * run in the **apply** scope of the composition's [Recomposer], which is usually your UI's main
 * thread.
 *
 * This function should **not** be used to (re-)launch ongoing tasks in response to callback
 * events by way of storing callback data in [MutableState] passed to [keys]. Instead, see
 * [rememberCoroutineScope] to obtain a [CoroutineScope] that may be used to launch ongoing jobs
 * scoped to the composition in response to event callbacks.
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

@PublishedApi
internal class CompositionScopedCoroutineScopeCanceller(
    val coroutineScope: CoroutineScope
) : CompositionLifecycleObserver {
    override fun onLeave() {
        coroutineScope.cancel()
    }
}

@PublishedApi
@OptIn(ExperimentalComposeApi::class)
internal fun createCompositionCoroutineScope(
    coroutineContext: CoroutineContext,
    composer: Composer<*>
) = if (coroutineContext[Job] != null) {
    CoroutineScope(Job().apply {
        completeExceptionally(IllegalArgumentException("CoroutineContext supplied to " +
                "rememberCoroutineScope may not include a parent job"))
    })
} else {
    val applyContext = composer.recomposer.applyingCoroutineContext
    if (applyContext == null) {
        CoroutineScope(Job().apply {
            completeExceptionally(IllegalStateException("cannot create a new composition " +
                    "coroutine scope - Composition is not active"))
        })
    } else CoroutineScope(applyContext + Job(applyContext[Job]) + coroutineContext)
}

/**
 * Return a [CoroutineScope] bound to this point in the composition using the optional
 * [CoroutineContext] provided by [getContext]. [getContext] will only be called once and the same
 * [CoroutineScope] instance will be returned across recompositions.
 *
 * This scope will be [cancelled][CoroutineScope.cancel] when this call leaves the composition.
 * The [CoroutineContext] returned by [getContext] may not contain a [Job] as this scope is
 * considered to be a child of the composition.
 *
 * The default dispatcher of this scope if one is not provided by the context returned by
 * [getContext] will be the applying dispatcher of the composition's [Recomposer].
 *
 * Use this scope to launch jobs in response to callback events such as clicks or other user
 * interaction where the response to that event needs to unfold over time and be cancelled if the
 * composable managing that process leaves the composition. Jobs should never be launched into
 * **any** coroutine scope as a side effect of composition itself. For scoped ongoing jobs
 * initiated by composition, see [launchInComposition].
 *
 * This function will not throw if preconditions are not met, as composable functions do not yet
 * fully support exceptions. Instead the returned scope's [CoroutineScope.coroutineContext] will
 * contain a failed [Job] with the associated exception and will not be capable of launching
 * child jobs.
 */
@Composable
inline fun rememberCoroutineScope(
    getContext: () -> CoroutineContext = { EmptyCoroutineContext }
): CoroutineScope {
    val composer = currentComposer
    val wrapper = remember {
        CompositionScopedCoroutineScopeCanceller(
            createCompositionCoroutineScope(getContext(), composer)
        )
    }
    return wrapper.coroutineScope
}
