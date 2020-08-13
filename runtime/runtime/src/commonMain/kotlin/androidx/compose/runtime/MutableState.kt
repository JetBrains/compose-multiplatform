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

package androidx.compose.runtime

import androidx.compose.runtime.snapshots.MutableSnapshot
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.snapshots.StateObject
import androidx.compose.runtime.snapshots.StateRecord
import androidx.compose.runtime.snapshots.readable
import androidx.compose.runtime.snapshots.takeSnapshot
import androidx.compose.runtime.snapshots.withCurrent
import androidx.compose.runtime.snapshots.writable
import kotlin.reflect.KProperty

/**
 * A composable used to introduce a state value of type [T] into a composition.
 *
 * This is useful when you have a value that you would like to locally mutate and use in the
 * context of a composition. Since the returned [MutableState] instance implements snapshot
 * changes to the [MutableState.value] property will be automatically tracked in composition and
 * schedule a recompose.
 *
 * The [MutableState] class can be used several different ways. For example, the most basic way
 * is to store the returned state value into a local immutable variable, and then set the
 * [MutableState.value] property on it.
 *
 * @sample androidx.compose.runtime.samples.SimpleStateSample
 *
 * @sample androidx.compose.runtime.samples.stateSample
 *
 * In this example, `LoginScreen` is recomposed every time the username and password of the model
 * updates, keeping the UI synchronized with the state.
 *
 * Additionally, you can destructure the [MutableState] object into a value and a "setter" function.
 *
 * @sample androidx.compose.runtime.samples.DestructuredStateSample
 *
 * Finally, the [MutableState] instance can be used as a variable delegate to a local mutable
 * variable.
 *
 * @sample androidx.compose.runtime.samples.DelegatedStateSample
 *
 * @param policy a policy to control how changes are handled in mutable snapshots.

 * @return An instance of [MutableState] that wraps the value.
 *
 * @see [stateFor]
 * @see [remember]
 * @see [SnapshotMutationPolicy]
 */
@Deprecated(
    "Replace with explicit use of remember {}",
    ReplaceWith("remember { mutableStateOf(init(), policy) }")
)
@Composable
inline fun <T> state(
    policy: SnapshotMutationPolicy<T> = structuralEqualityPolicy(),
    init: @ComposableContract(preventCapture = true) () -> T
) = remember { mutableStateOf(init(), policy) }

/**
 * An effect to introduce a state value of type [T] into a composition that will last as long as
 * the input [v1] does not change.
 *
 * This is useful when you have a value that you would like to locally mutate and use in the
 * context of a composition, and its value is scoped to another value and you want it to be reset
 * every time the other value changes.
 *
 * The returned [MutableState] instance implements snapshot so that changes to the
 * [MutableState.value] property will be automatically tracked in composition and schedule a
 * recompose.
 *
 * @param v1 An input value that, when changed, will cause the state to reset and [init] to be rerun
 * @param init A factory function to create the initial value of this state
 * @return An instance of [MutableState] that wraps the value.
 *
 * @sample androidx.compose.runtime.samples.observeUserSample
 *
 * @see [state]
 * @see [remember]
 */
@Deprecated(
    "Replace with explicit use of remember {}",
    ReplaceWith("remember(v1) { mutableStateOf(init()) }")
)
@Composable
inline fun <T, /*reified*/ V1> stateFor(
    v1: V1,
    init: @ComposableContract(preventCapture = true) () -> T
) = remember(v1) { mutableStateOf(init()) }

/**
 * An effect to introduce a state value of type [T] into a composition that will last as long as
 * the inputs [v1] and [v2] do not change.
 *
 * This is useful when you have a value that you would like to locally mutate and use in the
 * context of a composition, and its value is scoped to another value and you want it to be reset
 * every time the other value changes.
 *
 * The returned [MutableState] instance implements snapshots such that changes to the
 * [MutableState.value] property will be automatically tracked in composition and schedule a
 * recompose.
 *
 * @param v1 An input value that, when changed, will cause the state to reset and [init] to be rerun
 * @param v2 An input value that, when changed, will cause the state to reset and [init] to be rerun
 * @param init A factory function to create the initial value of this state
 * @return An instance of [MutableState] that wraps the value.
 *
 * @see [state]
 * @see [remember]
 */
@Composable
inline fun <T, reified V1, reified V2> stateFor(
    v1: V1,
    v2: V2,
    init: @ComposableContract(preventCapture = true) () -> T
) = remember(v1, v2) { mutableStateOf(init()) }

/**
 * An effect to introduce a state value of type [T] into a composition that will last as long as
 * the inputs [inputs] do not change.
 *
 * This is useful when you have a value that you would like to locally mutate and use in the
 * context of a composition, and its value is scoped to another value and you want it to be reset
 * every time the other value changes.
 *
 * The returned [MutableState] instance implements snapshots so that changes to the
 * [MutableState.value] property will be automatically tracked in composition and schedule a
 * recompose.
 *
 * @param inputs A set of inputs such that, when any of them have changed, will cause the state
 * to reset and [init] to be rerun
 * @param init A factory function to create the initial value of this state
 * @return An instance of [MutableState] that wraps the value.
 *
 * @see [state]
 * @see [remember]
 */
@Composable
inline fun <T> stateFor(
    vararg inputs: Any?,
    init: @ComposableContract(preventCapture = true) () -> T
) = remember(*inputs) { mutableStateOf(init()) }

/**
 * Return a new [MutableState] initialized with the passed in [value]
 *
 * The MutableState class is a single value holder whose reads and writes are observed by
 * Compose. Additionally, writes to it are transacted as part of the [Snapshot] system.
 * During composition, you will likely want to use the [state] and [stateFor] composables instead
 * of this factory function.
 *
 * @param value the initial value for the [MutableState]
 * @param policy a policy to controls how changes are handled in mutable snapshots.
 *
 * @see state
 * @see stateFor
 * @see State
 * @see MutableState
 * @see SnapshotMutationPolicy
 */
fun <T> mutableStateOf(
    value: T,
    policy: SnapshotMutationPolicy<T> = structuralEqualityPolicy()
): MutableState<T> = SnapshotMutableState(value, policy)

/**
 * Simple comparison callback using referential `===` equality
 */
@Deprecated(
    "areEquivalent callbacks have been replaced by MutableStateSnapshotPolicy",
    ReplaceWith(
        "referentialEqualityPolicy()",
        "androidx.compose.runtime.referentialEqualityPolicy"
    )
)
val ReferentiallyEqual = fun(old: Any?, new: Any?) = old === new

/**
 * Simple comparison callback using structural [Any.equals] equality
 */
@Deprecated(
    "areEquivalent callbacks have been replaced by MutableStateSnapshotPolicy",
    ReplaceWith(
        "structuralEqualityPolicy()",
        "androidx.compose.runtime.structuralEqualityPolicy"
    )
)
val StructurallyEqual = fun(old: Any?, new: Any?) = old == new

/**
 * Simple comparison callback that always returns false, for mutable objects that will be
 * compared with the same reference.
 *
 * In this case we cannot correctly compare for equality, and so we trust that something else
 * correctly triggered a recomposition.
 */
@Deprecated(
    "areEquivalent callbacks have been replaced by MutableStateSnapshotPolicy",
    ReplaceWith(
        "neverEqualPolicy()",
        "androidx.compose.runtime.neverEqualPolicy"
    )
)
val NeverEqual = fun(_: Any?, _: Any?) = false

/**
 * A value holder where reads to the [value] property during the execution of a [Composable]
 * function, the current [RecomposeScope] will be subscribed to changes of that value.
 *
 * @see [state]
 * @see [MutableState]
 * @see [mutableStateOf]
 */
@Stable
interface State<T> {
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
 * @see [state]
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
 * The ModelMutableState class is a single value holder whose reads and writes are observed by
 * Compose.
 *
 * Additionally, writes to it are transacted as part of the [Snapshot] system.
 * `state` and `stateFor` composables.
 *
 * @property value the wrapped value
 * @property policy a policy to control how changes are handled in a mutable snapshot.
 *
 * @see state
 * @see stateFor
 * @see mutableStateOf
 * @see SnapshotMutationPolicy
 */
@OptIn(androidx.compose.runtime.ExperimentalComposeApi::class)
private class SnapshotMutableState<T>(
    value: T,
    val policy: SnapshotMutationPolicy<T>
) : StateObject, MutableState<T> {
    @Suppress("UNCHECKED_CAST")
    override var value: T
        get() = next.readable(this).value
        set(value) = next.withCurrent {
            if (!policy.equivalent(it.value, value)) {
                next.writable(this) { this.value = value }
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
     * var (foo, setFoo) = remember { mutableStateOf(0) }
     * setFoo(123) // set
     * foo == 123 // get
     */
    override operator fun component1(): T = value

    override operator fun component2(): (T) -> Unit = { value = it }
}

/**
 * A policy to control how the result of [mutableStateOf] and [state] report and merge changes to
 * the state object.
 *
 * A mutation policy can be passed as an parameter to [state], [mutableStateOf], and [ambientOf].
 *
 * Typically, one of the stock policies should be used such as [referentialEqualityPolicy],
 * [structuralEqualityPolicy], or [neverEqualPolicy]. However, a custom mutation policy can be
 * created by implementing this interface, such as a counter policy,
 *
 * @sample androidx.compose.runtime.samples.counterSample
 */
interface SnapshotMutationPolicy<T> {
    /**
     * Determine if setting a state value's are equivalent and should be treated as equal. If
     * [equivalent] returns `true` the new value is not considered a change.
     */
    fun equivalent(a: T, b: T): Boolean

    /**
     * Merge conflicting changes in snapshots. This is only called if [current] and [applied] are
     * not [equivalent]. If a valid merged value can be calculated then it should be returned.
     *
     * For example, if the state object holds an immutable data class with multiple fields,
     * and [applied] has changed fields that are unmodified by [current] it might be valid to return
     * a new copy of the data class that combines that changes from both [current] and [applied]
     * allowing a snapshot to apply that would have otherwise failed.
     *
     * @sample androidx.compose.runtime.samples.counterSample
     */
    @ExperimentalComposeApi
    fun merge(previous: T, current: T, applied: T): T? = null
}

/**
 * A policy to treat values of a [MutableState] as equivalent if they are referentially (===) equal.
 *
 * Setting [MutableState.value] to its current referentially (===) equal value is not considered
 * a change. When applying a [MutableSnapshot], if the snapshot changes the value to the
 * equivalent value the parent snapshot has is not considered a conflict.
 */
@Suppress("UNCHECKED_CAST")
fun <T> referentialEqualityPolicy(): SnapshotMutationPolicy<T> =
    ReferentialEqualityPolicy as SnapshotMutationPolicy<T>

private object ReferentialEqualityPolicy : SnapshotMutationPolicy<Any?> {
    override fun equivalent(a: Any?, b: Any?) = a === b
}

/**
 * A policy to treat values of a [MutableState] as equivalent if they are structurally (==) equal.
 *
 * Setting [MutableState.value] to its current structurally (==) equal value is not considered
 * a change. When applying a [MutableSnapshot], if the snapshot changes the value to the
 * equivalent value the parent snapshot has is not considered a conflict.
 */
@Suppress("UNCHECKED_CAST")
fun <T> structuralEqualityPolicy(): SnapshotMutationPolicy<T> =
    StructuralEqualityPolicy as SnapshotMutationPolicy<T>

private object StructuralEqualityPolicy : SnapshotMutationPolicy<Any?> {
    override fun equivalent(a: Any?, b: Any?) = a == b
}

/**
 * A policy never treat values of a [MutableState] as equivalent.
 *
 * Setting [MutableState.value] will always be considered a change. When applying a
 * [MutableSnapshot] that changes the state will always conflict with other snapshots that change
 * the same state.
 */
@Suppress("UNCHECKED_CAST")
fun <T> neverEqualPolicy(): SnapshotMutationPolicy<T> =
    NeverEqualPolicy as SnapshotMutationPolicy<T>

private object NeverEqualPolicy : SnapshotMutationPolicy<Any?> {
    override fun equivalent(a: Any?, b: Any?) = false
}

/**
 * Create a instance of MutableList<T> that is observable and can be snapshot.
 *
 * @sample androidx.compose.runtime.samples.stateListSample
 *
 * @see mutableStateOf
 * @see mutableListOf
 * @see MutableList
 * @see takeSnapshot
 */
fun <T> mutableStateListOf() = SnapshotStateList<T>()

/**
 * Create an instance of MutableList<T> that is observable and can be snapshot.
 *
 * @see mutableStateOf
 * @see mutableListOf
 * @see MutableList
 * @see takeSnapshot
 */
fun <T> mutableStateListOf(vararg elements: T) =
    SnapshotStateList<T>().also { it.addAll(elements.toList()) }

/**
 * Create an instance of MutableList<T> from a collection that is observerable and can be snapshot.
 */
fun <T> Collection<T>.toMutableStateList() = SnapshotStateList<T>().also { it.addAll(this) }

/**
 * Create a instance of MutableSet<K, V> that is observable and can be snapshot.
 *
 * @sample androidx.compose.runtime.samples.stateMapSample
 *
 * @see mutableStateOf
 * @see mutableMapOf
 * @see MutableMap
 * @see takeSnapshot
 */
fun <K, V> mutableStateMapOf() = SnapshotStateMap<K, V>()

/**
 * Create a instance of MutableMap<K, V> that is observable and can be snapshot.
 *
 * @see mutableStateOf
 * @see mutableMapOf
 * @see MutableMap
 * @see takeSnapshot
 */
fun <K, V> mutableStateMapOf(vararg pairs: Pair<K, V>) =
    SnapshotStateMap<K, V>().apply { putAll(pairs.toMap()) }

/**
 * Create an instance of MutableMap<K, V> from a collection of pairs that is observable and can be
 * snapshot.
 */
fun <K, V> Iterable<Pair<K, V>>.toMutableStateMap() =
    SnapshotStateMap<K, V>().also { it.putAll(this.toMap()) }
