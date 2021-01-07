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

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Schedule [effect] to run when the current composition completes successfully and applies
 * changes. [SideEffect] can be used to apply side effects to objects managed by the
 * composition that are not backed by [snapshots][androidx.compose.runtime.snapshots.Snapshot] so
 * as not to leave those objects in an inconsistent state if the current composition operation
 * fails.
 *
 * [effect] will always be run on the composition's apply dispatcher and appliers are never run
 * concurrent with themselves, one another, applying changes to the composition tree, or running
 * [CompositionLifecycleObserver] event callbacks. [SideEffect]s are always run after
 * [CompositionLifecycleObserver] event callbacks.
 *
 * A [SideEffect] runs after **every** recomposition. To launch an ongoing task spanning
 * potentially many recompositions, see [LaunchedEffect]. To manage an event subscription or other
 * object lifecycle, see [DisposableEffect].
 */
@Composable
@ComposableContract(restartable = false)
fun SideEffect(
    effect: () -> Unit
) {
    currentComposer.recordSideEffect(effect)
}

/**
 * Receiver scope for [DisposableEffect] that offers the [onDispose] clause that should be
 * the last statement in any call to [DisposableEffect].
 */
class DisposableEffectScope {
    /**
     * Provide [onDisposeEffect] to the [DisposableEffect] to run when it leaves the composition
     * or its key changes.
     */
    inline fun onDispose(
        crossinline onDisposeEffect: () -> Unit
    ): DisposableEffectDisposable = object : DisposableEffectDisposable {
        override fun dispose() {
            onDisposeEffect()
        }
    }
}

interface DisposableEffectDisposable {
    fun dispose()
}

private val InternalDisposableEffectScope = DisposableEffectScope()

private class DisposableEffectImpl(
    private val effect: DisposableEffectScope.() -> DisposableEffectDisposable
) : CompositionLifecycleObserver {
    private var onDispose: DisposableEffectDisposable? = null

    override fun onEnter() {
        onDispose = InternalDisposableEffectScope.effect()
    }

    override fun onLeave() {
        onDispose?.dispose()
        onDispose = null
    }
}

private const val DisposableEffectNoParamError =
    "DisposableEffect must provide one or more 'key' parameters that define the identity of " +
        "the DisposableEffect and determine when its previous effect should be disposed and " +
        "a new effect started for the new key."

private const val LaunchedEffectNoParamError =
    "LaunchedEffect must provide one or more 'key' parameters that define the identity of " +
        "the LaunchedEffect and determine when its previous effect coroutine should be cancelled " +
        "and a new effect launched for the new key."

@Composable
@ComposableContract(restartable = false)
@Suppress("DeprecatedCallableAddReplaceWith", "UNUSED_PARAMETER")
@Deprecated(DisposableEffectNoParamError, level = DeprecationLevel.ERROR)
fun DisposableEffect(
    effect: DisposableEffectScope.() -> DisposableEffectDisposable
): Unit = error(DisposableEffectNoParamError)

/**
 * A side effect of composition that must run for any new unique value of [key1] and must be
 * reversed or cleaned up if [key1] changes or if the [DisposableEffect] leaves the composition.
 *
 * A [DisposableEffect]'s _key_ is a value that defines the identity of the
 * [DisposableEffect]. If a key changes, the [DisposableEffect] must
 * [dispose][DisposableEffectScope.onDispose] its current [effect] and reset by calling [effect]
 * again. Examples of keys include:
 *
 * * Observable objects that the effect subscribes to
 * * Unique request parameters to an operation that must cancel and retry if those parameters change
 *
 * [DisposableEffect] may be used to initialize or subscribe to a key and reinitialize
 * when a different key is provided, performing cleanup for the old operation before
 * initializing the new. For example:
 *
 * @sample androidx.compose.runtime.samples.disposableEffectSample
 *
 * A [DisposableEffect] **must** include an [onDispose][DisposableEffectScope.onDispose] clause
 * as the final statement in its [effect] block. If your operation does not require disposal
 * it might be a [SideEffect] instead, or a [LaunchedEffect] if it launches a coroutine that should
 * be managed by the composition.
 *
 * There is guaranteed to be one call to [dispose][DisposableEffectScope.onDispose] for every call
 * to [effect]. Both [effect] and [dispose][DisposableEffectScope.onDispose] will always be run
 * on the composition's apply dispatcher and appliers are never run concurrent with themselves,
 * one another, applying changes to the composition tree, or running [CompositionLifecycleObserver]
 * event callbacks.
 */
@Composable
@ComposableContract(restartable = false)
fun DisposableEffect(
    key1: Any?,
    effect: DisposableEffectScope.() -> DisposableEffectDisposable
) {
    remember(key1) { DisposableEffectImpl(effect) }
}

/**
 * A side effect of composition that must run for any new unique value of [key1] or [key2]
 * and must be reversed or cleaned up if [key1] or [key2] changes, or if the
 * [DisposableEffect] leaves the composition.
 *
 * A [DisposableEffect]'s _key_ is a value that defines the identity of the
 * [DisposableEffect]. If a key changes, the [DisposableEffect] must
 * [dispose][DisposableEffectScope.onDispose] its current [effect] and reset by calling [effect]
 * again. Examples of keys include:
 *
 * * Observable objects that the effect subscribes to
 * * Unique request parameters to an operation that must cancel and retry if those parameters change
 *
 * [DisposableEffect] may be used to initialize or subscribe to a key and reinitialize
 * when a different key is provided, performing cleanup for the old operation before
 * initializing the new. For example:
 *
 * @sample androidx.compose.runtime.samples.disposableEffectSample
 *
 * A [DisposableEffect] **must** include an [onDispose][DisposableEffectScope.onDispose] clause
 * as the final statement in its [effect] block. If your operation does not require disposal
 * it might be a [SideEffect] instead, or a [LaunchedEffect] if it launches a coroutine that should
 * be managed by the composition.
 *
 * There is guaranteed to be one call to [dispose][DisposableEffectScope.onDispose] for every call
 * to [effect]. Both [effect] and [dispose][DisposableEffectScope.onDispose] will always be run
 * on the composition's apply dispatcher and appliers are never run concurrent with themselves,
 * one another, applying changes to the composition tree, or running [CompositionLifecycleObserver]
 * event callbacks.
 */
@Composable
@ComposableContract(restartable = false)
fun DisposableEffect(
    key1: Any?,
    key2: Any?,
    effect: DisposableEffectScope.() -> DisposableEffectDisposable
) {
    remember(key1, key2) { DisposableEffectImpl(effect) }
}

/**
 * A side effect of composition that must run for any new unique value of [key1], [key2]
 * or [key3] and must be reversed or cleaned up if [key1], [key2] or [key3]
 * changes, or if the [DisposableEffect] leaves the composition.
 *
 * A [DisposableEffect]'s _key_ is a value that defines the identity of the
 * [DisposableEffect]. If a key changes, the [DisposableEffect] must
 * [dispose][DisposableEffectScope.onDispose] its current [effect] and reset by calling [effect]
 * again. Examples of keys include:
 *
 * * Observable objects that the effect subscribes to
 * * Unique request parameters to an operation that must cancel and retry if those parameters change
 *
 * [DisposableEffect] may be used to initialize or subscribe to a key and reinitialize
 * when a different key is provided, performing cleanup for the old operation before
 * initializing the new. For example:
 *
 * @sample androidx.compose.runtime.samples.disposableEffectSample
 *
 * A [DisposableEffect] **must** include an [onDispose][DisposableEffectScope.onDispose] clause
 * as the final statement in its [effect] block. If your operation does not require disposal
 * it might be a [SideEffect] instead, or a [LaunchedEffect] if it launches a coroutine that should
 * be managed by the composition.
 *
 * There is guaranteed to be one call to [dispose][DisposableEffectScope.onDispose] for every call
 * to [effect]. Both [effect] and [dispose][DisposableEffectScope.onDispose] will always be run
 * on the composition's apply dispatcher and appliers are never run concurrent with themselves,
 * one another, applying changes to the composition tree, or running [CompositionLifecycleObserver]
 * event callbacks.
 */
@Composable
@ComposableContract(restartable = false)
fun DisposableEffect(
    key1: Any?,
    key2: Any?,
    key3: Any?,
    effect: DisposableEffectScope.() -> DisposableEffectDisposable
) {
    remember(key1, key2, key3) { DisposableEffectImpl(effect) }
}

/**
 * A side effect of composition that must run for any new unique value of [keys] and must
 * be reversed or cleaned up if any [keys] change or if the [DisposableEffect] leaves the
 * composition.
 *
 * A [DisposableEffect]'s _key_ is a value that defines the identity of the
 * [DisposableEffect]. If a key changes, the [DisposableEffect] must
 * [dispose][DisposableEffectScope.onDispose] its current [effect] and reset by calling [effect]
 * again. Examples of keys include:
 *
 * * Observable objects that the effect subscribes to
 * * Unique request parameters to an operation that must cancel and retry if those parameters change
 *
 * [DisposableEffect] may be used to initialize or subscribe to a key and reinitialize
 * when a different key is provided, performing cleanup for the old operation before
 * initializing the new. For example:
 *
 * @sample androidx.compose.runtime.samples.disposableEffectSample
 *
 * A [DisposableEffect] **must** include an [onDispose][DisposableEffectScope.onDispose] clause
 * as the final statement in its [effect] block. If your operation does not require disposal
 * it might be a [SideEffect] instead, or a [LaunchedEffect] if it launches a coroutine that should
 * be managed by the composition.
 *
 * There is guaranteed to be one call to [dispose][DisposableEffectScope.onDispose] for every call
 * to [effect]. Both [effect] and [dispose][DisposableEffectScope.onDispose] will always be run
 * on the composition's apply dispatcher and appliers are never run concurrent with themselves,
 * one another, applying changes to the composition tree, or running [CompositionLifecycleObserver]
 * event callbacks.
 */
@Composable
@ComposableContract(restartable = false)
@Suppress("ArrayReturn")
fun DisposableEffect(
    vararg keys: Any?,
    effect: DisposableEffectScope.() -> DisposableEffectDisposable
) {
    remember(*keys) { DisposableEffectImpl(effect) }
}

internal class LaunchedEffectImpl(
    parentCoroutineContext: CoroutineContext,
    private val task: suspend CoroutineScope.() -> Unit
) : CompositionLifecycleObserver {

    private val scope = CoroutineScope(parentCoroutineContext)
    private var job: Job? = null

    override fun onEnter() {
        job?.cancel("Old job was still running!")
        job = scope.launch(block = task)
    }

    override fun onLeave() {
        job?.cancel()
        job = null
    }
}

/**
 * When [LaunchedEffect] enters the composition it will launch [block] into the composition's
 * [CoroutineContext]. The coroutine will be [cancelled][Job.cancel] when the [LaunchedEffect]
 * leaves the composition.
 *
 * It is an error to call [LaunchedEffect] without at least one `key` parameter.
 */
@Deprecated(LaunchedEffectNoParamError, level = DeprecationLevel.ERROR)
@Suppress("DeprecatedCallableAddReplaceWith", "UNUSED_PARAMETER")
@Composable
fun LaunchedEffect(
    block: suspend CoroutineScope.() -> Unit
): Unit = error(LaunchedEffectNoParamError)

/**
 * When [LaunchedEffect] enters the composition it will launch [block] into the composition's
 * [CoroutineContext]. The coroutine will be [cancelled][Job.cancel] and **re-launched** when
 * [LaunchedEffect] is recomposed with a different [key1]. The coroutine will be
 * [cancelled][Job.cancel] when the [LaunchedEffect] leaves the composition.
 *
 * This function should **not** be used to (re-)launch ongoing tasks in response to callback
 * events by way of storing callback data in [MutableState] passed to [key1]. Instead, see
 * [rememberCoroutineScope] to obtain a [CoroutineScope] that may be used to launch ongoing jobs
 * scoped to the composition in response to event callbacks.
 */
@Composable
@ComposableContract(restartable = false)
fun LaunchedEffect(
    key1: Any?,
    block: suspend CoroutineScope.() -> Unit
) {
    val applyContext = currentComposer.applyCoroutineContext
    remember(key1) { LaunchedEffectImpl(applyContext, block) }
}

/**
 * When [LaunchedEffect] enters the composition it will launch [block] into the composition's
 * [CoroutineContext]. The coroutine will be [cancelled][Job.cancel] and **re-launched** when
 * [LaunchedEffect] is recomposed with a different [key1] or [key2]. The coroutine will be
 * [cancelled][Job.cancel] when the [LaunchedEffect] leaves the composition.
 *
 * This function should **not** be used to (re-)launch ongoing tasks in response to callback
 * events by way of storing callback data in [MutableState] passed to [key]. Instead, see
 * [rememberCoroutineScope] to obtain a [CoroutineScope] that may be used to launch ongoing jobs
 * scoped to the composition in response to event callbacks.
 */
@Composable
@ComposableContract(restartable = false)
fun LaunchedEffect(
    key1: Any?,
    key2: Any?,
    block: suspend CoroutineScope.() -> Unit
) {
    val applyContext = currentComposer.applyCoroutineContext
    remember(key1, key2) { LaunchedEffectImpl(applyContext, block) }
}

/**
 * When [LaunchedEffect] enters the composition it will launch [block] into the composition's
 * [CoroutineContext]. The coroutine will be [cancelled][Job.cancel] and **re-launched** when
 * [LaunchedEffect] is recomposed with a different [key1], [key2] or [key3].
 * The coroutine will be [cancelled][Job.cancel] when the [LaunchedEffect] leaves the composition.
 *
 * This function should **not** be used to (re-)launch ongoing tasks in response to callback
 * events by way of storing callback data in [MutableState] passed to [key]. Instead, see
 * [rememberCoroutineScope] to obtain a [CoroutineScope] that may be used to launch ongoing jobs
 * scoped to the composition in response to event callbacks.
 */
@Composable
@ComposableContract(restartable = false)
fun LaunchedEffect(
    key1: Any?,
    key2: Any?,
    key3: Any?,
    block: suspend CoroutineScope.() -> Unit
) {
    val applyContext = currentComposer.applyCoroutineContext
    remember(key1, key2, key3) { LaunchedEffectImpl(applyContext, block) }
}

/**
 * When [LaunchedEffect] enters the composition it will launch [block] into the composition's
 * [CoroutineContext]. The coroutine will be [cancelled][Job.cancel] and **re-launched** when
 * [LaunchedEffect] is recomposed with any different [keys]. The coroutine will be
 * [cancelled][Job.cancel] when the [LaunchedEffect] leaves the composition.
 *
 * This function should **not** be used to (re-)launch ongoing tasks in response to callback
 * events by way of storing callback data in [MutableState] passed to [key]. Instead, see
 * [rememberCoroutineScope] to obtain a [CoroutineScope] that may be used to launch ongoing jobs
 * scoped to the composition in response to event callbacks.
 */
@Composable
@ComposableContract(restartable = false)
@Suppress("ArrayReturn")
fun LaunchedEffect(
    vararg keys: Any?,
    block: suspend CoroutineScope.() -> Unit
) {
    val applyContext = currentComposer.applyCoroutineContext
    remember(*keys) { LaunchedEffectImpl(applyContext, block) }
}
