/*
 * Copyright 2019 The Android Open Source Project
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
import androidx.compose.runtime.snapshots.SnapshotMutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.snapshots.StateObject
import androidx.compose.runtime.snapshots.StateRecord
import androidx.compose.runtime.snapshots.overwritable
import androidx.compose.runtime.snapshots.readable
import androidx.compose.runtime.snapshots.withCurrent
// Explicit imports for jvm annotations needed in common source sets.
import kotlin.jvm.JvmName
import kotlin.jvm.JvmMultifileClass
import kotlin.reflect.KProperty

/**
 * Return a new [MutableState] initialized with the passed in [value]
 *
 * The MutableState class is a single value holder whose reads and writes are observed by
 * Compose. Additionally, writes to it are transacted as part of the [Snapshot] system.
 *
 * @param value the initial value for the [MutableState]
 * @param policy a policy to controls how changes are handled in mutable snapshots.
 *
 * @sample androidx.compose.runtime.samples.SimpleStateSample
 * @sample androidx.compose.runtime.samples.DestructuredStateSample
 * @sample androidx.compose.runtime.samples.observeUserSample
 * @sample androidx.compose.runtime.samples.stateSample
 *
 * @see State
 * @see MutableState
 * @see SnapshotMutationPolicy
 */
fun <T> mutableStateOf(
    value: T,
    policy: SnapshotMutationPolicy<T> = structuralEqualityPolicy()
): MutableState<T> = createSnapshotMutableState(value, policy)

/**
 * A value holder where reads to the [value] property during the execution of a [Composable]
 * function, the current [RecomposeScope] will be subscribed to changes of that value.
 *
 * @see [MutableState]
 * @see [mutableStateOf]
 */
@Stable
interface State<out T> {
    val value: T
}

/**
 * Permits property delegation of `val`s using `by` for [State].
 *
 * @sample androidx.compose.runtime.samples.DelegatedReadOnlyStateSample
 */
@Suppress("NOTHING_TO_INLINE")
inline operator fun <T> State<T>.getValue(thisObj: Any?, property: KProperty<*>): T = value

/**
 * A mutable value holder where reads to the [value] property during the execution of a [Composable]
 * function, the current [RecomposeScope] will be subscribed to changes of that value. When the
 * [value] property is written to and changed, a recomposition of any subscribed [RecomposeScope]s
 * will be scheduled. If [value] is written to with the same value, no recompositions will be
 * scheduled.
 *
 * @see [State]
 * @see [mutableStateOf]
 */
@Stable
interface MutableState<T> : State<T> {
    override var value: T
    operator fun component1(): T
    operator fun component2(): (T) -> Unit
}

/**
 * Permits property delegation of `var`s using `by` for [MutableState].
 *
 * @sample androidx.compose.runtime.samples.DelegatedStateSample
 */
@Suppress("NOTHING_TO_INLINE")
inline operator fun <T> MutableState<T>.setValue(thisObj: Any?, property: KProperty<*>, value: T) {
    this.value = value
}

/**
 * Returns platform specific implementation based on [SnapshotMutableStateImpl].
 */
internal expect fun <T> createSnapshotMutableState(
    value: T,
    policy: SnapshotMutationPolicy<T>
): SnapshotMutableState<T>

/**
 * A single value holder whose reads and writes are observed by Compose.
 *
 * Additionally, writes to it are transacted as part of the [Snapshot] system.
 *
 * @param value the wrapped value
 * @param policy a policy to control how changes are handled in a mutable snapshot.
 *
 * @see mutableStateOf
 * @see SnapshotMutationPolicy
 */
internal open class SnapshotMutableStateImpl<T>(
    value: T,
    override val policy: SnapshotMutationPolicy<T>
) : StateObject, SnapshotMutableState<T> {
    @Suppress("UNCHECKED_CAST")
    override var value: T
        get() = next.readable(this).value
        set(value) = next.withCurrent {
            if (!policy.equivalent(it.value, value)) {
                next.overwritable(this, it) { this.value = value }
            }
        }

    private var next: StateStateRecord<T> = StateStateRecord(value)

    override val firstStateRecord: StateRecord
        get() = next

    override fun prependStateRecord(value: StateRecord) {
        @Suppress("UNCHECKED_CAST")
        next = value as StateStateRecord<T>
    }

    @Suppress("UNCHECKED_CAST")
    override fun mergeRecords(
        previous: StateRecord,
        current: StateRecord,
        applied: StateRecord
    ): StateRecord? {
        val previousRecord = previous as StateStateRecord<T>
        val currentRecord = current as StateStateRecord<T>
        val appliedRecord = applied as StateStateRecord<T>
        return if (policy.equivalent(currentRecord.value, appliedRecord.value))
            current
        else {
            val merged = policy.merge(
                previousRecord.value,
                currentRecord.value,
                appliedRecord.value
            )
            if (merged != null) {
                appliedRecord.create().also {
                    (it as StateStateRecord<T>).value = merged
                }
            } else {
                null
            }
        }
    }

    override fun toString(): String = next.withCurrent {
        "MutableState(value=${it.value})@${hashCode()}"
    }

    private class StateStateRecord<T>(myValue: T) : StateRecord() {
        override fun assign(value: StateRecord) {
            @Suppress("UNCHECKED_CAST")
            this.value = (value as StateStateRecord<T>).value
        }

        override fun create(): StateRecord = StateStateRecord(value)

        var value: T = myValue
    }

    /**
     * The componentN() operators allow state objects to be used with the property destructuring
     * syntax
     *
     * ```
     * var (foo, setFoo) = remember { mutableStateOf(0) }
     * setFoo(123) // set
     * foo == 123 // get
     * ```
     */
    override operator fun component1(): T = value

    override operator fun component2(): (T) -> Unit = { value = it }

    /**
     * A function used by the debugger to display the value of the current value of the mutable
     * state object without triggering read observers.
     */
    @Suppress("unused")
    val debuggerDisplayValue: T
        @JvmName("getDebuggerDisplayValue")
        get() = next.withCurrent { it }.value
}

/**
 * Create a instance of [MutableList]<T> that is observable and can be snapshot.
 *
 * @sample androidx.compose.runtime.samples.stateListSample
 *
 * @see mutableStateOf
 * @see mutableListOf
 * @see MutableList
 * @see Snapshot.takeSnapshot
 */
fun <T> mutableStateListOf() = SnapshotStateList<T>()

/**
 * Create an instance of [MutableList]<T> that is observable and can be snapshot.
 *
 * @see mutableStateOf
 * @see mutableListOf
 * @see MutableList
 * @see Snapshot.takeSnapshot
 */
fun <T> mutableStateListOf(vararg elements: T) =
    SnapshotStateList<T>().also { it.addAll(elements.toList()) }

/**
 * Create an instance of [MutableList]<T> from a collection that is observable and can be
 * snapshot.
 */
fun <T> Collection<T>.toMutableStateList() = SnapshotStateList<T>().also { it.addAll(this) }

/**
 * Create a instance of [MutableMap]<K, V> that is observable and can be snapshot.
 *
 * @sample androidx.compose.runtime.samples.stateMapSample
 *
 * @see mutableStateOf
 * @see mutableMapOf
 * @see MutableMap
 * @see Snapshot.takeSnapshot
 */
fun <K, V> mutableStateMapOf() = SnapshotStateMap<K, V>()

/**
 * Create a instance of [MutableMap]<K, V> that is observable and can be snapshot.
 *
 * @see mutableStateOf
 * @see mutableMapOf
 * @see MutableMap
 * @see Snapshot.takeSnapshot
 */
fun <K, V> mutableStateMapOf(vararg pairs: Pair<K, V>) =
    SnapshotStateMap<K, V>().apply { putAll(pairs.toMap()) }

/**
 * Create an instance of [MutableMap]<K, V> from a collection of pairs that is observable and can be
 * snapshot.
 */
@Suppress("unused")
fun <K, V> Iterable<Pair<K, V>>.toMutableStateMap() =
    SnapshotStateMap<K, V>().also { it.putAll(this.toMap()) }

/**
 * [remember] a [mutableStateOf] [newValue] and update its value to [newValue] on each
 * recomposition of the [rememberUpdatedState] call.
 *
 * [rememberUpdatedState] should be used when parameters or values computed during composition
 * are referenced by a long-lived lambda or object expression. Recomposition will update the
 * resulting [State] without recreating the long-lived lambda or object, allowing that object to
 * persist without cancelling and resubscribing, or relaunching a long-lived operation that may
 * be expensive or prohibitive to recreate and restart.
 * This may be common when working with [DisposableEffect] or [LaunchedEffect], for example:
 *
 * @sample androidx.compose.runtime.samples.rememberUpdatedStateSampleWithDisposableEffect
 *
 * [LaunchedEffect]s often describe state machines that should not be reset and restarted if a
 * parameter or event callback changes, but they should have the current value available when
 * needed. For example:
 *
 * @sample androidx.compose.runtime.samples.rememberUpdatedStateSampleWithLaunchedTask
 *
 * By using [rememberUpdatedState] a composable function can update these operations in progress.
 */
@Composable
fun <T> rememberUpdatedState(newValue: T): State<T> = remember {
    mutableStateOf(newValue)
}.apply { value = newValue }
