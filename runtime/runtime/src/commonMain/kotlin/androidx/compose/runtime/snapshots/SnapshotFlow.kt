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

package androidx.compose.runtime.snapshots

import androidx.compose.runtime.ExperimentalComposeApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

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
@ExperimentalComposeApi
fun <T> snapshotFlow(
    block: () -> T
): Flow<T> = flow {
    // Objects read the last time block was run
    val readSet = mutableSetOf<Any>()
    val readObserver: (Any) -> Unit = { readSet.add(it) }

    // This channel may not block or lose data on an offer call.
    val appliedChanges = Channel<Set<Any>>(Channel.UNLIMITED)

    // Register the apply observer before running for the first time
    // so that we don't miss updates.
    val unregisterApplyObserver = Snapshot.registerApplyObserver { changed, _ ->
        appliedChanges.offer(changed)
    }

    try {
        var lastValue = takeSnapshot(readObserver).run {
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
                changedObjects = appliedChanges.poll() ?: break
            }

            if (found) {
                readSet.clear()
                val newValue = takeSnapshot(readObserver).run {
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
        unregisterApplyObserver()
    }
}

/**
 * Return `true` if there are any elements shared between `this` and [other]
 */
private fun <T> Set<T>.intersects(other: Set<T>): Boolean =
    if (size < other.size) any { it in other } else other.any { it in this }

/**
 * Take a [MutableSnapshot] and run [block] within it. When [block] returns successfully,
 * attempt to [MutableSnapshot.apply] the snapshot. Returns the result of [block] or throws
 * [SnapshotApplyConflictException] if snapshot changes attempted by [block] could not be applied.
 *
 * Prior to returning, any changes made to snapshot state (e.g. state holders returned by
 * [androidx.compose.runtime.mutableStateOf] are not visible to other threads. When
 * [withMutableSnapshot] returns successfully those changes will be made visible to other threads
 * and any snapshot observers (e.g. [snapshotFlow]) will be notified of changes.
 *
 * [block] must not suspend if [withMutableSnapshot] is called from a suspend function.
 */
// TODO: determine a good way to prevent/discourage suspending in an inlined [block]
@ExperimentalComposeApi
inline fun <R> withMutableSnapshot(
    block: () -> R
): R = takeMutableSnapshot().run {
    try {
        enter(block).also { apply().check() }
    } catch (t: Throwable) {
        dispose()
        throw t
    }
}
