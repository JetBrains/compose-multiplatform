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
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@Suppress("DEPRECATION")
@Deprecated("No replacement; removing CompositionCoroutineScope in migration to LaunchedTask")
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
@Deprecated("No replacement; LaunchedTask uses CoroutineScope as a receiver directly")
interface CompositionCoroutineScope : CoroutineScope {
    // This method deliberately shadows the awaitFrame method from kotlinx-coroutines-android
    // to redirect usage to the CompositionFrameClock API in effect blocks.
    @Suppress("RedundantSuspendModifier", "DeprecatedCallableAddReplaceWith")
    @Deprecated(
        "use withFrameNanos to perform work on a composition frame",
        level = DeprecationLevel.ERROR
    )
    suspend fun awaitFrame(): Long = error("awaitFrame should not be used; use withFrameNanos")
}

/**
 * Suspends the current coroutine until the effect is **disposed** and the
 * [CompositionCoroutineScope] is cancelled, and invokes [onDispose] before resuming.
 * [awaitDispose] never resumes normally and will always throw either
 * [kotlinx.coroutines.CancellationException] or the exception that failed the current
 * [kotlinx.coroutines.Job].
 */
@Suppress("unused", "DeprecatedCallableAddReplaceWith", "DEPRECATION")
@Deprecated("No replacement; LaunchedTask uses CoroutineScope as a receiver directly")
suspend fun CompositionCoroutineScope.awaitDispose(onDispose: () -> Unit = {}): Nothing = try {
    suspendCancellableCoroutine<Nothing> { /* Suspend until cancellation */ }
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
@Suppress("DEPRECATION")
@Deprecated(
    "Renamed to LaunchedTask; custom scope removed",
    replaceWith = ReplaceWith("LaunchedTask(block)", "androidx.compose.runtime.LaunchedTask")
)
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
@Suppress("DEPRECATION")
@Deprecated(
    "Renamed to LaunchedTask; custom scope removed",
    replaceWith = ReplaceWith("LaunchedTask(key, block)", "androidx.compose.runtime.LaunchedTask")
)
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
@Suppress("DEPRECATION")
@Deprecated(
    "Renamed to LaunchedTask; custom scope removed",
    replaceWith = ReplaceWith(
        "LaunchedTask(key1, key2, block)",
        "androidx.compose.runtime.LaunchedTask"
    )
)
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
@Suppress("DEPRECATION")
@Deprecated(
    "Renamed to LaunchedTask; custom scope removed",
    replaceWith = ReplaceWith(
        "LaunchedTask(key1, key2, key3, block)",
        "androidx.compose.runtime.LaunchedTask"
    )
)
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
@Suppress("DEPRECATION")
@Deprecated(
    "Renamed to LaunchedTask; custom scope removed",
    replaceWith = ReplaceWith(
        "LaunchedTask(*keys, block)",
        "androidx.compose.runtime.LaunchedTask"
    )
)
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
    CoroutineScope(
        Job().apply {
            completeExceptionally(
                IllegalArgumentException(
                    "CoroutineContext supplied to " +
                        "rememberCoroutineScope may not include a parent job"
                )
            )
        }
    )
} else {
    val applyContext = composer.recomposer.applyingCoroutineContext
    if (applyContext == null) {
        CoroutineScope(
            Job().apply {
                completeExceptionally(
                    IllegalStateException(
                        "cannot create a new composition " +
                            "coroutine scope - Composition is not active"
                    )
                )
            }
        )
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

private class LaunchedTaskImpl(
    private val recomposer: Recomposer,
    private val task: suspend CoroutineScope.() -> Unit
) : CompositionLifecycleObserver {

    private var job: Job? = null

    override fun onEnter() {
        job?.cancel("Old job was still running!")
        val scope = CoroutineScope(recomposer.applyingCoroutineContext
            ?: error("cannot launch LaunchedTask - Recomposer is not running"))
        job = scope.launch(block = task)
    }

    override fun onLeave() {
        job?.cancel()
        job = null
    }
}

/**
 * When [LaunchedTask] enters the composition it will launch [block] into the composition's
 * [CoroutineContext]. The coroutine will be [cancelled][Job.cancel] when the [LaunchedTask]
 * leaves the composition.
 *
 * To cancel and re-launch a task when input parameters change, see the overloads of
 * [LaunchedTask] that accept additional key parameters.
 */
@Composable
fun LaunchedTask(
    block: suspend CoroutineScope.() -> Unit
) {
    @OptIn(ExperimentalComposeApi::class)
    val recomposer = currentComposer.recomposer
    remember { LaunchedTaskImpl(recomposer, block) }
}

/**
 * When [LaunchedTask] enters the composition it will launch [block] into the composition's
 * [CoroutineContext]. The coroutine will be [cancelled][Job.cancel] and **re-launched** when
 * [LaunchedTask] is recomposed with a different [key]. The coroutine will be
 * [cancelled][Job.cancel] when the [LaunchedTask] leaves the composition.
 *
 * This function should **not** be used to (re-)launch ongoing tasks in response to callback
 * events by way of storing callback data in [MutableState] passed to [key]. Instead, see
 * [rememberCoroutineScope] to obtain a [CoroutineScope] that may be used to launch ongoing jobs
 * scoped to the composition in response to event callbacks.
 */
@Composable
fun LaunchedTask(
    key: Any?,
    block: suspend CoroutineScope.() -> Unit
) {
    @OptIn(ExperimentalComposeApi::class)
    val recomposer = currentComposer.recomposer
    remember(key) { LaunchedTaskImpl(recomposer, block) }
}

/**
 * When [LaunchedTask] enters the composition it will launch [block] into the composition's
 * [CoroutineContext]. The coroutine will be [cancelled][Job.cancel] and **re-launched** when
 * [LaunchedTask] is recomposed with a different [key]. The coroutine will be
 * [cancelled][Job.cancel] when the [LaunchedTask] leaves the composition.
 *
 * This function should **not** be used to (re-)launch ongoing tasks in response to callback
 * events by way of storing callback data in [MutableState] passed to [key]. Instead, see
 * [rememberCoroutineScope] to obtain a [CoroutineScope] that may be used to launch ongoing jobs
 * scoped to the composition in response to event callbacks.
 */
@Composable
fun LaunchedTask(
    key1: Any?,
    key2: Any?,
    block: suspend CoroutineScope.() -> Unit
) {
    @OptIn(ExperimentalComposeApi::class)
    val recomposer = currentComposer.recomposer
    remember(key1, key2) { LaunchedTaskImpl(recomposer, block) }
}

/**
 * When [LaunchedTask] enters the composition it will launch [block] into the composition's
 * [CoroutineContext]. The coroutine will be [cancelled][Job.cancel] and **re-launched** when
 * [LaunchedTask] is recomposed with a different [key]. The coroutine will be
 * [cancelled][Job.cancel] when the [LaunchedTask] leaves the composition.
 *
 * This function should **not** be used to (re-)launch ongoing tasks in response to callback
 * events by way of storing callback data in [MutableState] passed to [key]. Instead, see
 * [rememberCoroutineScope] to obtain a [CoroutineScope] that may be used to launch ongoing jobs
 * scoped to the composition in response to event callbacks.
 */
@Composable
fun LaunchedTask(
    key1: Any?,
    key2: Any?,
    key3: Any?,
    block: suspend CoroutineScope.() -> Unit
) {
    @OptIn(ExperimentalComposeApi::class)
    val recomposer = currentComposer.recomposer
    remember(key1, key2, key3) { LaunchedTaskImpl(recomposer, block) }
}

/**
 * When [LaunchedTask] enters the composition it will launch [block] into the composition's
 * [CoroutineContext]. The coroutine will be [cancelled][Job.cancel] and **re-launched** when
 * [LaunchedTask] is recomposed with a different [key]. The coroutine will be
 * [cancelled][Job.cancel] when the [LaunchedTask] leaves the composition.
 *
 * This function should **not** be used to (re-)launch ongoing tasks in response to callback
 * events by way of storing callback data in [MutableState] passed to [key]. Instead, see
 * [rememberCoroutineScope] to obtain a [CoroutineScope] that may be used to launch ongoing jobs
 * scoped to the composition in response to event callbacks.
 */
@Composable
fun LaunchedTask(
    vararg keys: Any?,
    block: suspend CoroutineScope.() -> Unit
) {
    @OptIn(ExperimentalComposeApi::class)
    val recomposer = currentComposer.recomposer
    remember(*keys) { LaunchedTaskImpl(recomposer, block) }
}
