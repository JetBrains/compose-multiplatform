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

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.StateObject
import androidx.compose.runtime.snapshots.StateRecord
import androidx.compose.runtime.snapshots.newWritableRecord
import androidx.compose.runtime.snapshots.readable
import androidx.compose.runtime.snapshots.sync

@OptIn(ExperimentalComposeApi::class)
private class DerivedSnapshotState<T>(private val calculation: () -> T) : StateObject, State<T> {
    private var first: ResultRecord<T> = ResultRecord()
    private class ResultRecord<T> : StateRecord() {
        var dependencies: HashSet<StateObject>? = null
        var result: T? = null
        var resultHash: Int = 0

        override fun assign(value: StateRecord) {
            @Suppress("UNCHECKED_CAST")
            val other = value as ResultRecord<T>
            dependencies = other.dependencies
            result = other.result
            resultHash = other.resultHash
        }

        override fun create(): StateRecord = ResultRecord<T>()

        fun isValid(snapshot: Snapshot): Boolean =
            result != null && resultHash == readableHash(snapshot)

        fun readableHash(snapshot: Snapshot): Int {
            var hash = 7
            val dependencies = sync { dependencies }
            if (dependencies != null)
                for (stateObject in dependencies) {
                    val record = stateObject.firstStateRecord.readable(stateObject, snapshot)
                    hash = 31 * hash + identityHashCode(record)
                    hash = 31 * hash + record.snapshotId
                }
            return hash
        }
    }

    private fun value(snapshot: Snapshot, calculation: () -> T): T {
        val readable = first.readable(this, snapshot)
        if (readable.isValid(snapshot)) {
            @Suppress("UNCHECKED_CAST")
            return readable.result as T
        }
        val newDependencies = HashSet<StateObject>()
        val result = Snapshot.observe(
            {
                if (it is StateObject) newDependencies.add(it)
            },
            null, calculation
        )

        sync {
            val writable = first.newWritableRecord(this, snapshot)
            writable.dependencies = newDependencies
            writable.resultHash = writable.readableHash(snapshot)
            writable.result = result
        }

        snapshot.notifyObjectsInitialized()

        return result
    }

    override val firstStateRecord: StateRecord get() = first

    override fun prependStateRecord(value: StateRecord) {
        @Suppress("UNCHECKED_CAST")
        first = value as ResultRecord<T>
    }

    override val value: T get() = value(Snapshot.current, calculation)
}

/**
 * Creates a [State] object whose [State.value] is the result of [calculation]. The result of
 * calculation will be cached in such a way that calling [State.value] repeatedly will not cause
 * [calculation] to be executed multiple times, but reading [State.value] will cause all [State]
 * objects that got read during the [calculation] to be read in the current [Snapshot], meaning
 * that this will correctly subscribe to the derived state objects if the value is being read in
 * an observed context such as a [Composable] function.
 *
 * @sample androidx.compose.runtime.samples.DerivedStateSample
 *
 * @param calculation the calculation to create the value this state object represents.
 */
fun <T> derivedStateOf(calculation: () -> T): State<T> = DerivedSnapshotState(calculation)