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

import androidx.compose.runtime.collection.IdentityArrayMap
import androidx.compose.runtime.external.kotlinx.collections.immutable.PersistentList
import androidx.compose.runtime.external.kotlinx.collections.immutable.persistentListOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.StateObject
import androidx.compose.runtime.snapshots.StateRecord
import androidx.compose.runtime.snapshots.current
import androidx.compose.runtime.snapshots.fastForEach
import androidx.compose.runtime.snapshots.newWritableRecord
import androidx.compose.runtime.snapshots.sync
import androidx.compose.runtime.snapshots.withCurrent
// Explicit imports for these needed in common source sets.
import kotlin.jvm.JvmName
import kotlin.jvm.JvmMultifileClass
import kotlin.math.min

/**
 * A [State] that is derived from one or more other states.
 *
 * @see derivedStateOf
 */
internal interface DerivedState<T> : State<T> {
    /**
     * The value of the derived state retrieved without triggering a notification to read observers.
     */
    val currentValue: T

    /**
     * A list of the dependencies used to produce [value] or [currentValue].
     *
     * The [dependencies] list can be used to determine when a [StateObject] appears in the apply
     * observer set, if the state could affect value of this derived state.
     */
    val dependencies: Array<Any?>

    /**
     * Mutation policy that controls how changes are handled after state dependencies update.
     * If the policy is `null`, the derived state update is triggered regardless of the value
     * produced and it is up to observer to invalidate it correctly.
     */
    val policy: SnapshotMutationPolicy<T>?
}

private typealias DerivedStateObservers = Pair<(DerivedState<*>) -> Unit, (DerivedState<*>) -> Unit>

private val derivedStateObservers = SnapshotThreadLocal<PersistentList<DerivedStateObservers>>()

private val calculationBlockNestedLevel = SnapshotThreadLocal<Int>()

private class DerivedSnapshotState<T>(
    private val calculation: () -> T,
    override val policy: SnapshotMutationPolicy<T>?
) : StateObject, DerivedState<T> {
    private var first: ResultRecord<T> = ResultRecord()

    class ResultRecord<T> : StateRecord() {
        companion object {
            val Unset = Any()
        }

        var dependencies: IdentityArrayMap<StateObject, Int>? = null
        var result: Any? = Unset
        var resultHash: Int = 0

        override fun assign(value: StateRecord) {
            @Suppress("UNCHECKED_CAST")
            val other = value as ResultRecord<T>
            dependencies = other.dependencies
            result = other.result
            resultHash = other.resultHash
        }

        override fun create(): StateRecord = ResultRecord<T>()

        fun isValid(derivedState: DerivedState<*>, snapshot: Snapshot): Boolean =
            result !== Unset && resultHash == readableHash(derivedState, snapshot)

        fun readableHash(derivedState: DerivedState<*>, snapshot: Snapshot): Int {
            var hash = 7
            val dependencies = sync { dependencies }
            if (dependencies != null) {
                notifyObservers(derivedState) {
                    dependencies.forEach { stateObject, readLevel ->
                        if (readLevel != 1) {
                            return@forEach
                        }

                        if (stateObject is DerivedSnapshotState<*>) {
                            // eagerly access the parent derived states without recording the
                            // read
                            // that way we can be sure derived states in deps were recalculated,
                            // and are updated to the last values
                            stateObject.refresh(stateObject.firstStateRecord, snapshot)
                        }

                        // Find the first record without triggering an observer read.
                        val record = current(stateObject.firstStateRecord, snapshot)

                        hash = 31 * hash + identityHashCode(record)
                        hash = 31 * hash + record.snapshotId
                    }
                }
            }
            return hash
        }
    }

    fun refresh(record: StateRecord, snapshot: Snapshot) {
        @Suppress("UNCHECKED_CAST")
        currentRecord(record as ResultRecord<T>, snapshot, false, calculation)
    }

    private fun currentRecord(
        readable: ResultRecord<T>,
        snapshot: Snapshot,
        forceDependencyReads: Boolean,
        calculation: () -> T
    ): ResultRecord<T> {
        if (readable.isValid(this, snapshot)) {
            // If the dependency is not recalculated, emulate nested state reads
            // for correct invalidation later
            if (forceDependencyReads) {
                notifyObservers(this) {
                    val dependencies = readable.dependencies
                    val invalidationNestedLevel = calculationBlockNestedLevel.get() ?: 0
                    dependencies?.forEach { dependency, nestedLevel ->
                        calculationBlockNestedLevel.set(nestedLevel + invalidationNestedLevel)
                        snapshot.readObserver?.invoke(dependency)
                    }
                    calculationBlockNestedLevel.set(invalidationNestedLevel)
                }
            }
            return readable
        }
        val nestedCalculationLevel = calculationBlockNestedLevel.get() ?: 0

        val newDependencies = IdentityArrayMap<StateObject, Int>()
        val result = notifyObservers(this) {
            calculationBlockNestedLevel.set(nestedCalculationLevel + 1)

            val result = Snapshot.observe(
                {
                    if (it === this)
                        error("A derived state calculation cannot read itself")
                    if (it is StateObject) {
                        val readNestedLevel = calculationBlockNestedLevel.get()!!
                        newDependencies[it] = min(
                            readNestedLevel - nestedCalculationLevel,
                            newDependencies[it] ?: Int.MAX_VALUE
                        )
                    }
                },
                null, calculation
            )

            calculationBlockNestedLevel.set(nestedCalculationLevel)
            result
        }

        val record = sync {
            val currentSnapshot = Snapshot.current

            if (
                readable.result !== ResultRecord.Unset &&
                @Suppress("UNCHECKED_CAST")
                policy?.equivalent(result, readable.result as T) == true
            ) {
                readable.dependencies = newDependencies
                readable.resultHash = readable.readableHash(this, currentSnapshot)
                readable
            } else {
                val writable = first.newWritableRecord(this, currentSnapshot)
                writable.dependencies = newDependencies
                writable.resultHash = writable.readableHash(this, currentSnapshot)
                writable.result = result
                writable
            }
        }

        if (nestedCalculationLevel == 0) {
            Snapshot.notifyObjectsInitialized()
        }

        return record
    }

    override val firstStateRecord: StateRecord get() = first

    override fun prependStateRecord(value: StateRecord) {
        @Suppress("UNCHECKED_CAST")
        first = value as ResultRecord<T>
    }

    override val value: T
        get() {
            // Unlike most state objects, the record list of a derived state can change during a read
            // because reading updates the cache. To account for this, instead of calling readable,
            // which sends the read notification, the read observer is notified directly and current
            // value is used instead which doesn't notify. This allow the read observer to read the
            // value and only update the cache once.
            Snapshot.current.readObserver?.invoke(this)
            return first.withCurrent {
                @Suppress("UNCHECKED_CAST")
                currentRecord(it, Snapshot.current, true, calculation).result as T
            }
        }

    override val currentValue: T
        get() = first.withCurrent {
            @Suppress("UNCHECKED_CAST")
            currentRecord(it, Snapshot.current, false, calculation).result as T
        }

    override val dependencies: Array<Any?>
        get() = first.withCurrent {
            val record = currentRecord(it, Snapshot.current, false, calculation)
            @Suppress("UNCHECKED_CAST")
            record.dependencies?.keys ?: emptyArray()
        }

    override fun toString(): String = first.withCurrent {
        "DerivedState(value=${displayValue()})@${hashCode()}"
    }

    /**
     * A function used by the debugger to display the value of the current value of the mutable
     * state object without triggering read observers.
     */
    @Suppress("unused")
    val debuggerDisplayValue: T?
        @JvmName("getDebuggerDisplayValue")
        get() = first.withCurrent {
            @Suppress("UNCHECKED_CAST")
            if (it.isValid(this, Snapshot.current))
                it.result as T
            else null
        }

    private fun displayValue(): String {
        first.withCurrent {
            if (it.isValid(this, Snapshot.current)) {
                return it.result.toString()
            }
            return "<Not calculated>"
        }
    }
}

private inline fun <R> notifyObservers(derivedState: DerivedState<*>, block: () -> R): R {
    val observers = derivedStateObservers.get() ?: persistentListOf()
    observers.fastForEach { (start, _) -> start(derivedState) }
    return try {
        block()
    } finally {
        observers.fastForEach { (_, done) -> done(derivedState) }
    }
}

/**
 * Creates a [State] object whose [State.value] is the result of [calculation]. The result of
 * calculation will be cached in such a way that calling [State.value] repeatedly will not cause
 * [calculation] to be executed multiple times, but reading [State.value] will cause all [State]
 * objects that got read during the [calculation] to be read in the current [Snapshot], meaning
 * that this will correctly subscribe to the derived state objects if the value is being read in
 * an observed context such as a [Composable] function.
 * Derived states without mutation policy trigger updates on each dependency change. To avoid
 * invalidation on update, provide suitable [SnapshotMutationPolicy] through [derivedStateOf]
 * overload.
 *
 * @sample androidx.compose.runtime.samples.DerivedStateSample
 *
 * @param calculation the calculation to create the value this state object represents.
 */
fun <T> derivedStateOf(
    calculation: () -> T,
): State<T> = DerivedSnapshotState(calculation, null)

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
 * @param policy mutation policy to control when changes to the [calculation] result trigger update.
 * @param calculation the calculation to create the value this state object represents.
 */
fun <T> derivedStateOf(
    policy: SnapshotMutationPolicy<T>,
    calculation: () -> T,
): State<T> = DerivedSnapshotState(calculation, policy)

/**
 * Observe the recalculations performed by any derived state that is recalculated during the
 * execution of [block]. [start] is called before a calculation starts and [done] is called
 * after the started calculation is complete.
 *
 * @param start a lambda called before every calculation of a derived state is in [block].
 * @param done a lambda that is called after the state passed to [start] is recalculated.
 * @param block the block of code to observe.
 */
internal fun <R> observeDerivedStateRecalculations(
    start: (derivedState: State<*>) -> Unit,
    done: (derivedState: State<*>) -> Unit,
    block: () -> R
) {
    val previous = derivedStateObservers.get()
    try {
        derivedStateObservers.set(
            (derivedStateObservers.get() ?: persistentListOf()).add(
                start to done
            )
        )
        block()
    } finally {
        derivedStateObservers.set(previous)
    }
}
