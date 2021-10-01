/*
 * Copyright 2021 The Android Open Source Project
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

@file:JvmName("SnapshotStateKt")
@file:JvmMultifileClass
package androidx.compose.runtime

import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
// Explicit imports for these needed in common source sets.
import kotlin.jvm.JvmName
import kotlin.jvm.JvmMultifileClass

/**
 * Collects values from this [StateFlow] and represents its latest value via [State].
 * The [StateFlow.value] is used as an initial value. Every time there would be new value posted
 * into the [StateFlow] the returned [State] will be updated causing recomposition of every
 * [State.value] usage.
 *
 * @sample androidx.compose.runtime.samples.StateFlowSample
 *
 * @param context [CoroutineContext] to use for collecting.
 */
@Suppress("StateFlowValueCalledInComposition")
@Composable
fun <T> StateFlow<T>.collectAsState(
    context: CoroutineContext = EmptyCoroutineContext
): State<T> = collectAsState(value, context)

/**
 * Collects values from this [Flow] and represents its latest value via [State]. Every time there
 * would be new value posted into the [Flow] the returned [State] will be updated causing
 * recomposition of every [State.value] usage.
 *
 * @sample androidx.compose.runtime.samples.FlowWithInitialSample
 *
 * @param context [CoroutineContext] to use for collecting.
 */
@Composable
fun <T : R, R> Flow<T>.collectAsState(
    initial: R,
    context: CoroutineContext = EmptyCoroutineContext
): State<R> = produceState(initial, this, context) {
    if (context == EmptyCoroutineContext) {
        collect { value = it }
    } else withContext(context) {
        collect { value = it }
    }
}

/**
 * Create a [Flow] from observable [Snapshot] state. (e.g. state holders returned by
 * [mutableStateOf][androidx.compose.runtime.mutableStateOf].)
 *
 * [snapshotFlow] creates a [Flow] that runs [block] when collected and emits the result,
 * recording any snapshot state that was accessed. While collection continues, if a new [Snapshot]
 * is applied that changes state accessed by [block], the flow will run [block] again,
 * re-recording the snapshot state that was accessed.
 * If the result of [block] is not [equal to][Any.equals] the previous result, the flow will emit
 * that new result. (This behavior is similar to that of
 * [Flow.distinctUntilChanged][kotlinx.coroutines.flow.distinctUntilChanged].) Collection will
 * continue indefinitely unless it is explicitly cancelled or limited by the use of other [Flow]
 * operators.
 *
 * @sample androidx.compose.runtime.samples.snapshotFlowSample
 *
 * [block] is run in a **read-only** [Snapshot] and may not modify snapshot data. If [block]
 * attempts to modify snapshot data, flow collection will fail with [IllegalStateException].
 *
 * [block] may run more than once for equal sets of inputs or only once after many rapid
 * snapshot changes; it should be idempotent and free of side effects.
 *
 * When working with [Snapshot] state it is useful to keep the distinction between **events** and
 * **state** in mind. [snapshotFlow] models snapshot changes as events, but events **cannot** be
 * effectively modeled as observable state. Observable state is a lossy compression of the events
 * that produced that state.
 *
 * An observable **event** happens at a point in time and is discarded. All registered observers
 * at the time the event occurred are notified. All individual events in a stream are assumed
 * to be relevant and may build on one another; repeated equal events have meaning and therefore
 * a registered observer must observe all events without skipping.
 *
 * Observable **state** raises change events when the state changes from one value to a new,
 * unequal value. State change events are **conflated;** only the most recent state matters.
 * Observers of state changes must therefore be **idempotent;** given the same state value the
 * observer should produce the same result. It is valid for a state observer to both skip
 * intermediate states as well as run multiple times for the same state and the result should
 * be the same.
 */
fun <T> snapshotFlow(
    block: () -> T
): Flow<T> = flow {
    // Objects read the last time block was run
    val readSet = mutableSetOf<Any>()
    val readObserver: (Any) -> Unit = { readSet.add(it) }

    // This channel may not block or lose data on a trySend call.
    val appliedChanges = Channel<Set<Any>>(Channel.UNLIMITED)

    // Register the apply observer before running for the first time
    // so that we don't miss updates.
    val unregisterApplyObserver = Snapshot.registerApplyObserver { changed, _ ->
        appliedChanges.trySend(changed)
    }

    try {
        var lastValue = Snapshot.takeSnapshot(readObserver).run {
            try {
                enter(block)
            } finally {
                dispose()
            }
        }
        emit(lastValue)

        while (true) {
            var found = false
            var changedObjects = appliedChanges.receive()

            // Poll for any other changes before running block to minimize the number of
            // additional times it runs for the same data
            while (true) {
                // Assumption: readSet will typically be smaller than changed
                found = found || readSet.intersects(changedObjects)
                changedObjects = appliedChanges.tryReceive().getOrNull() ?: break
            }

            if (found) {
                readSet.clear()
                val newValue = Snapshot.takeSnapshot(readObserver).run {
                    try {
                        enter(block)
                    } finally {
                        dispose()
                    }
                }

                if (newValue != lastValue) {
                    lastValue = newValue
                    emit(newValue)
                }
            }
        }
    } finally {
        unregisterApplyObserver.dispose()
    }
}

/**
 * Return `true` if there are any elements shared between `this` and [other]
 */
private fun <T> Set<T>.intersects(other: Set<T>): Boolean =
    if (size < other.size) any { it in other } else other.any { it in this }
