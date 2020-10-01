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

@file:OptIn(ExperimentalTypeInference::class)

package androidx.compose.runtime

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.CoroutineContext
import kotlin.experimental.ExperimentalTypeInference

/**
 * Receiver scope for use with [produceState].
 */
interface ProduceStateScope<T> : MutableState<T>, CoroutineScope {
    /**
     * Await the disposal of this producer whether it left the composition,
     * the source changed, or an error occurred. Always runs [onDispose] before resuming.
     *
     * This method is useful when configuring callback-based state producers that do not suspend,
     * for example:
     *
     * @sample androidx.compose.runtime.samples.produceStateAwaitDisposeSample
     */
    suspend fun awaitDispose(onDispose: () -> Unit): Nothing
}

private class ProduceStateScopeImpl<T>(
    state: MutableState<T>,
    override val coroutineContext: CoroutineContext
) : ProduceStateScope<T>, MutableState<T> by state {

    override suspend fun awaitDispose(onDispose: () -> Unit): Nothing {
        try {
            suspendCancellableCoroutine<Nothing> { }
        } finally {
            onDispose()
        }
    }
}

/**
 * Return an observable [snapshot][androidx.compose.runtime.snapshots.Snapshot] [State] that
 * produces values over time without a defined data source.
 *
 * [producer] is launched when [produceState] enters the composition and is cancelled when
 * [produceState] leaves the composition. [producer] should use [ProduceStateScope.value]
 * to set new values on the returned [State].
 *
 * The returned [State] conflates values; no change will be observable if
 * [ProduceStateScope.value] is used to set a value that is [equal][Any.equals] to its old value,
 * and observers may only see the latest value if several values are set in rapid succession.
 *
 * [produceState] may be used to observe either suspending or non-suspending sources of external
 * data, for example:
 *
 * @sample androidx.compose.runtime.samples.produceStateSample
 *
 * @sample androidx.compose.runtime.samples.produceStateAwaitDisposeSample
 */
@Composable
fun <T> produceState(
    initialValue: T,
    @BuilderInference producer: suspend ProduceStateScope<T>.() -> Unit
): State<T> {
    val result = remember { mutableStateOf(initialValue) }
    LaunchedTask {
        ProduceStateScopeImpl(result, coroutineContext).producer()
    }
    return result
}

/**
 * Return an observable [snapshot][androidx.compose.runtime.snapshots.Snapshot] [State] that
 * produces values over time from [source].
 *
 * [producer] is launched when [produceState] enters the composition and is cancelled when
 * [produceState] leaves the composition. If [source] changes, a running [producer] will be
 * cancelled and re-launched for the new source. [producer] should use [ProduceStateScope.value]
 * to set new values on the returned [State].
 *
 * The returned [State] conflates values; no change will be observable if
 * [ProduceStateScope.value] is used to set a value that is [equal][Any.equals] to its old value,
 * and observers may only see the latest value if several values are set in rapid succession.
 *
 * [produceState] may be used to observe either suspending or non-suspending sources of external
 * data, for example:
 *
 * @sample androidx.compose.runtime.samples.produceStateSample
 *
 * @sample androidx.compose.runtime.samples.produceStateAwaitDisposeSample
 */
@Composable
fun <T> produceState(
    initialValue: T,
    source: Any?,
    @BuilderInference producer: suspend ProduceStateScope<T>.() -> Unit
): State<T> {
    val result = remember { mutableStateOf(initialValue) }
    LaunchedTask(source) {
        ProduceStateScopeImpl(result, coroutineContext).producer()
    }
    return result
}

/**
 * Return an observable [snapshot][androidx.compose.runtime.snapshots.Snapshot] [State] that
 * produces values over time from [source1] and [source2].
 *
 * [producer] is launched when [produceState] enters the composition and is cancelled when
 * [produceState] leaves the composition. If [source1] or [source2] change, a running [producer]
 * will be cancelled and re-launched for the new source. [producer] should use
 * [ProduceStateScope.value] to set new values on the returned [State].
 *
 * The returned [State] conflates values; no change will be observable if
 * [ProduceStateScope.value] is used to set a value that is [equal][Any.equals] to its old value,
 * and observers may only see the latest value if several values are set in rapid succession.
 *
 * [produceState] may be used to observe either suspending or non-suspending sources of external
 * data, for example:
 *
 * @sample androidx.compose.runtime.samples.produceStateSample
 *
 * @sample androidx.compose.runtime.samples.produceStateAwaitDisposeSample
 */
@Composable
fun <T> produceState(
    initialValue: T,
    source1: Any?,
    source2: Any?,
    @BuilderInference producer: suspend ProduceStateScope<T>.() -> Unit
): State<T> {
    val result = remember { mutableStateOf(initialValue) }
    LaunchedTask(source1, source2) {
        ProduceStateScopeImpl(result, coroutineContext).producer()
    }
    return result
}

/**
 * Return an observable [snapshot][androidx.compose.runtime.snapshots.Snapshot] [State] that
 * produces values over time from [source1], [source2] and [source3].
 *
 * [producer] is launched when [produceState] enters the composition and is cancelled when
 * [produceState] leaves the composition. If [source1], [source2] or [source3] change, a running
 * [producer] will be cancelled and re-launched for the new source. [producer should use
 * [ProduceStateScope.value] to set new values on the returned [State].
 *
 * The returned [State] conflates values; no change will be observable if
 * [ProduceStateScope.value] is used to set a value that is [equal][Any.equals] to its old value,
 * and observers may only see the latest value if several values are set in rapid succession.
 *
 * [produceState] may be used to observe either suspending or non-suspending sources of external
 * data, for example:
 *
 * @sample androidx.compose.runtime.samples.produceStateSample
 *
 * @sample androidx.compose.runtime.samples.produceStateAwaitDisposeSample
 */
@Composable
fun <T> produceState(
    initialValue: T,
    source1: Any?,
    source2: Any?,
    source3: Any?,
    @BuilderInference producer: suspend ProduceStateScope<T>.() -> Unit
): State<T> {
    val result = remember { mutableStateOf(initialValue) }
    LaunchedTask(source1, source2, source3) {
        ProduceStateScopeImpl(result, coroutineContext).producer()
    }
    return result
}

/**
 * Return an observable [snapshot][androidx.compose.runtime.snapshots.Snapshot] [State] that
 * produces values over time from [sources].
 *
 * [producer] is launched when [produceState] enters the composition and is cancelled when
 * [produceState] leaves the composition. If [sources] change, a running [producer] will be
 * cancelled and re-launched for the new source. [producer] should use [ProduceStateScope.value]
 * to set new values on the returned [State].
 *
 * The returned [State] conflates values; no change will be observable if
 * [ProduceStateScope.value] is used to set a value that is [equal][Any.equals] to its old value,
 * and observers may only see the latest value if several values are set in rapid succession.
 *
 * [produceState] may be used to observe either suspending or non-suspending sources of external
 * data, for example:
 *
 * @sample androidx.compose.runtime.samples.produceStateSample
 *
 * @sample androidx.compose.runtime.samples.produceStateAwaitDisposeSample
 */
@Composable
fun <T> produceState(
    initialValue: T,
    vararg sources: Any?,
    @BuilderInference producer: suspend ProduceStateScope<T>.() -> Unit
): State<T> {
    val result = remember { mutableStateOf(initialValue) }
    LaunchedTask(*sources) {
        ProduceStateScopeImpl(result, coroutineContext).producer()
    }
    return result
}
