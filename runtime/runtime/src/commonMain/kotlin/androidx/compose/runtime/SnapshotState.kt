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

@file:OptIn(ExperimentalTypeInference::class)
package androidx.compose.runtime

import androidx.compose.runtime.snapshots.MutableSnapshot
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotMutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.snapshots.StateObject
import androidx.compose.runtime.snapshots.StateRecord
import androidx.compose.runtime.snapshots.newWritableRecord
import androidx.compose.runtime.snapshots.overwritable
import androidx.compose.runtime.snapshots.readable
import androidx.compose.runtime.snapshots.sync
import androidx.compose.runtime.snapshots.withCurrent
import androidx.compose.runtime.snapshots.writable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.experimental.ExperimentalTypeInference
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
 * A policy to control how the result of [mutableStateOf] report and merge changes to
 * the state object.
 *
 * A mutation policy can be passed as an parameter to [mutableStateOf], and [compositionLocalOf].
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

    override fun toString() = "ReferentialEqualityPolicy"
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

    override fun toString() = "StructuralEqualityPolicy"
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

    override fun toString() = "NeverEqualPolicy"
}

/**
 * Create a instance of MutableList<T> that is observable and can be snapshot.
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
 * Create an instance of MutableList<T> that is observable and can be snapshot.
 *
 * @see mutableStateOf
 * @see mutableListOf
 * @see MutableList
 * @see Snapshot.takeSnapshot
 */
fun <T> mutableStateListOf(vararg elements: T) =
    SnapshotStateList<T>().also { it.addAll(elements.toList()) }

/**
 * Create an instance of MutableList<T> from a collection that is observerable and can be snapshot.
 */
fun <T> Collection<T>.toMutableStateList() = SnapshotStateList<T>().also { it.addAll(this) }

/**
 * Create a instance of MutableMap<K, V> that is observable and can be snapshot.
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
 * Create a instance of MutableMap<K, V> that is observable and can be snapshot.
 *
 * @see mutableStateOf
 * @see mutableMapOf
 * @see MutableMap
 * @see Snapshot.takeSnapshot
 */
fun <K, V> mutableStateMapOf(vararg pairs: Pair<K, V>) =
    SnapshotStateMap<K, V>().apply { putAll(pairs.toMap()) }

/**
 * Create an instance of MutableMap<K, V> from a collection of pairs that is observable and can be
 * snapshot.
 */
@Suppress("unused")
fun <K, V> Iterable<Pair<K, V>>.toMutableStateMap() =
    SnapshotStateMap<K, V>().also { it.putAll(this.toMap()) }

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
     * @sample androidx.compose.runtime.samples.ProduceStateAwaitDispose
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
 * @sample androidx.compose.runtime.samples.ProduceState
 *
 * @sample androidx.compose.runtime.samples.ProduceStateAwaitDispose
 */
@Composable
fun <T> produceState(
    initialValue: T,
    @BuilderInference producer: suspend ProduceStateScope<T>.() -> Unit
): State<T> {
    val result = remember { mutableStateOf(initialValue) }
    LaunchedEffect(Unit) {
        ProduceStateScopeImpl(result, coroutineContext).producer()
    }
    return result
}

/**
 * Return an observable [snapshot][androidx.compose.runtime.snapshots.Snapshot] [State] that
 * produces values over time from [key1].
 *
 * [producer] is launched when [produceState] enters the composition and is cancelled when
 * [produceState] leaves the composition. If [key1] changes, a running [producer] will be
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
 * @sample androidx.compose.runtime.samples.ProduceState
 *
 * @sample androidx.compose.runtime.samples.ProduceStateAwaitDispose
 */
@Composable
fun <T> produceState(
    initialValue: T,
    key1: Any?,
    @BuilderInference producer: suspend ProduceStateScope<T>.() -> Unit
): State<T> {
    val result = remember { mutableStateOf(initialValue) }
    LaunchedEffect(key1) {
        ProduceStateScopeImpl(result, coroutineContext).producer()
    }
    return result
}

/**
 * Return an observable [snapshot][androidx.compose.runtime.snapshots.Snapshot] [State] that
 * produces values over time from [key1] and [key2].
 *
 * [producer] is launched when [produceState] enters the composition and is cancelled when
 * [produceState] leaves the composition. If [key1] or [key2] change, a running [producer]
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
 * @sample androidx.compose.runtime.samples.ProduceState
 *
 * @sample androidx.compose.runtime.samples.ProduceStateAwaitDispose
 */
@Composable
fun <T> produceState(
    initialValue: T,
    key1: Any?,
    key2: Any?,
    @BuilderInference producer: suspend ProduceStateScope<T>.() -> Unit
): State<T> {
    val result = remember { mutableStateOf(initialValue) }
    LaunchedEffect(key1, key2) {
        ProduceStateScopeImpl(result, coroutineContext).producer()
    }
    return result
}

/**
 * Return an observable [snapshot][androidx.compose.runtime.snapshots.Snapshot] [State] that
 * produces values over time from [key1], [key2] and [key3].
 *
 * [producer] is launched when [produceState] enters the composition and is cancelled when
 * [produceState] leaves the composition. If [key1], [key2] or [key3] change, a running
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
 * @sample androidx.compose.runtime.samples.ProduceState
 *
 * @sample androidx.compose.runtime.samples.ProduceStateAwaitDispose
 */
@Composable
fun <T> produceState(
    initialValue: T,
    key1: Any?,
    key2: Any?,
    key3: Any?,
    @BuilderInference producer: suspend ProduceStateScope<T>.() -> Unit
): State<T> {
    val result = remember { mutableStateOf(initialValue) }
    LaunchedEffect(key1, key2, key3) {
        ProduceStateScopeImpl(result, coroutineContext).producer()
    }
    return result
}

/**
 * Return an observable [snapshot][androidx.compose.runtime.snapshots.Snapshot] [State] that
 * produces values over time from [keys].
 *
 * [producer] is launched when [produceState] enters the composition and is cancelled when
 * [produceState] leaves the composition. If [keys] change, a running [producer] will be
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
 * @sample androidx.compose.runtime.samples.ProduceState
 *
 * @sample androidx.compose.runtime.samples.ProduceStateAwaitDispose
 */
@Composable
fun <T> produceState(
    initialValue: T,
    vararg keys: Any?,
    @BuilderInference producer: suspend ProduceStateScope<T>.() -> Unit
): State<T> {
    val result = remember { mutableStateOf(initialValue) }
    @Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
    LaunchedEffect(keys = keys) {
        ProduceStateScopeImpl(result, coroutineContext).producer()
    }
    return result
}

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

    // This channel may not block or lose data on an offer call.
    val appliedChanges = Channel<Set<Any>>(Channel.UNLIMITED)

    // Register the apply observer before running for the first time
    // so that we don't miss updates.
    val unregisterApplyObserver = Snapshot.registerApplyObserver { changed, _ ->
        appliedChanges.offer(changed)
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
                changedObjects = appliedChanges.poll() ?: break
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