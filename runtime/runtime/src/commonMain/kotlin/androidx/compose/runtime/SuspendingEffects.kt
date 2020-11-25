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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

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
    val applyContext = composer.applyCoroutineContext
    CoroutineScope(applyContext + Job(applyContext[Job]) + coroutineContext)
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
 * initiated by composition, see [LaunchedEffect].
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

/**
 * When [LaunchedTask] enters the composition it will launch [block] into the composition's
 * [CoroutineContext]. The coroutine will be [cancelled][Job.cancel] when the [LaunchedTask]
 * leaves the composition.
 *
 * To cancel and re-launch a task when input parameters change, see the overloads of
 * [LaunchedTask] that accept additional key parameters.
 */
@Composable
@Deprecated("Renamed to LaunchedEffect; no subject params not permitted")
fun LaunchedTask(
    block: suspend CoroutineScope.() -> Unit
) {
    val applyContext = currentComposer.applyCoroutineContext
    remember { LaunchedEffectImpl(applyContext, block) }
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
@Deprecated("Renamed to LaunchedEffect", ReplaceWith("LaunchedEffect(key, block)"))
fun LaunchedTask(
    key: Any?,
    block: suspend CoroutineScope.() -> Unit
) {
    LaunchedEffect(key, block)
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
@Deprecated("Renamed to LaunchedEffect", ReplaceWith("LaunchedEffect(key1, key2, block)"))
fun LaunchedTask(
    key1: Any?,
    key2: Any?,
    block: suspend CoroutineScope.() -> Unit
) {
    LaunchedEffect(key1, key2, block)
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
@Deprecated("Renamed to LaunchedEffect", ReplaceWith("LaunchedEffect(key1, key2, key3, block)"))
fun LaunchedTask(
    key1: Any?,
    key2: Any?,
    key3: Any?,
    block: suspend CoroutineScope.() -> Unit
) {
    LaunchedEffect(key1, key2, key3, block)
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
@Deprecated("Renamed to LaunchedEffect", ReplaceWith("LaunchedEffect(subjects = keys, block)"))
fun LaunchedTask(
    vararg keys: Any?,
    block: suspend CoroutineScope.() -> Unit
) {
    @Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
    LaunchedEffect(subjects = keys, block)
}
