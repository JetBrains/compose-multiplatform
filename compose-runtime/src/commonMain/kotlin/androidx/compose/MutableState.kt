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

package androidx.compose

import androidx.compose.frames.AbstractRecord
import androidx.compose.frames.Framed
import androidx.compose.frames.Record
import androidx.compose.frames._created
import androidx.compose.frames.readable
import androidx.compose.frames.withCurrent
import androidx.compose.frames.writable
import kotlin.reflect.KProperty

/**
 * A composable used to introduce a state value of type [T] into a composition.
 *
 * This is useful when you have a value that you would like to locally mutate and use in the context of a composition. Since
 * the returned [MutableState] instance implements [Framed], changes to the [MutableState.value]
 * property will be automatically tracked in composition and schedule a recompose.
 *
 * The [MutableState] class can be used several different ways. For example, the most basic way is to store the returned state
 * value into a local immutable variable, and then set the [MutableState.value] property on it.
 *
 * @sample androidx.compose.samples.SimpleStateSample
 *
 * @sample androidx.compose.samples.stateSample
 *
 * In this example, `LoginScreen` is recomposed every time the username and password of the
 * model updates, keeping the UI synchronized with the state.
 *
 * Additionally, you can destructure the [MutableState] object into a value and a "setter" function.
 *
 * @sample androidx.compose.samples.DestructuredStateSample
 *
 * Finally, the [MutableState] instance can be used as a variable delegate to a local mutable variable.
 *
 * @sample androidx.compose.samples.DelegatedStateSample
 *
 * @param areEquivalent a callback to compare the previous and new instance of [T] when
 * [MutableState.value] is written to. If this returns true, then no recomposition will be
 * scheduled. See [ReferentiallyEqual] and [StructurallyEqual] for simple implementations.
 * @param init A factory function to create the initial value of this state
 * @return An instance of [MutableState] that wraps the value.
 *
 * @see [stateFor]
 * @see [remember]
 */
@Composable
inline fun <T> state(
    noinline areEquivalent: (old: T, new: T) -> Boolean = ReferentiallyEqual,
    init: () -> T
) = remember { mutableStateOf(init(), areEquivalent) }

/**
 * An effect to introduce a state value of type [T] into a composition that will last as long as the input [v1] does not change.
 *
 * This is useful when you have a value that you would like to locally mutate and use in the context of a composition, and its
 * value is scoped to another value and you want it to be reset every time the other value changes.
 *
 * The returned [MutableState] instance implements [Framed] so that changes to the
 * [MutableState.value] property will be automatically tracked in composition and schedule a
 * recompose.
 *
 * @param v1 An input value that, when changed, will cause the state to reset and [init] to be rerun
 * @param init A factory function to create the initial value of this state
 * @return An instance of [MutableState] that wraps the value.
 *
 * @sample androidx.compose.samples.observeUserSample
 *
 * @see [state]
 * @see [remember]
 */
@Composable
inline fun <T, /*reified*/ V1> stateFor(v1: V1, init: () -> T) =
    remember(v1) { mutableStateOf(init()) }

/**
 * An effect to introduce a state value of type [T] into a composition that will last as long as the inputs [v1] and [v2] do not change.
 *
 * This is useful when you have a value that you would like to locally mutate and use in the context of a composition, and its
 * value is scoped to another value and you want it to be reset every time the other value changes.
 *
 * The returned [MutableState] instance implements [Framed] such that changes to the
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
    init: () -> T
) = remember(v1, v2) { mutableStateOf(init()) }

/**
 * An effect to introduce a state value of type [T] into a composition that will last as long as the inputs [inputs] do not change.
 *
 * This is useful when you have a value that you would like to locally mutate and use in the context of a composition, and its
 * value is scoped to another value and you want it to be reset every time the other value changes.
 *
 * The returned [MutableState] instance implements [Framed] so that changes to the
 * [MutableState.value] property will be automatically tracked in composition and schedule a
 * recompose.
 *
 * @param inputs A set of inputs such that, when any of them have changed, will cause the state to reset and [init] to be rerun
 * @param init A factory function to create the initial value of this state
 * @return An instance of [MutableState] that wraps the value.
 *
 * @see [state]
 * @see [remember]
 */
@Composable
inline fun <T> stateFor(vararg inputs: Any?, init: () -> T) =
    remember(*inputs) { mutableStateOf(init()) }

/**
 * Return a new [MutableState] initialized with the passed in [value]
 *
 * The MutableState class is a single value holder whose reads and writes are observed by
 * Compose. Additionally, writes to it are transacted as part of the [Framed] transaction system.
 * During composition, you will likely want to use the `state` and `stateFor` composables instead
 * of this factory function.
 *
 * @param value the initial value for the [MutableState]
 * @param areEquivalent a callback to compare the previous and new instance of [value] when
 * it is written to. If this returns true, then no recomposition will be scheduled. See
 * [ReferentiallyEqual] and [StructurallyEqual] for simple implementations.
 *
 * @see state
 * @see stateFor
 * @see State
 * @see MutableState
 */
fun <T> mutableStateOf(
    value: T,
    areEquivalent: (old: T, new: T) -> Boolean = ReferentiallyEqual
): MutableState<T> = FramedMutableState(value, areEquivalent)

/**
 * Simple comparison callback using referential `===` equality
 */
val ReferentiallyEqual = fun(old: Any?, new: Any?) = old === new

/**
 * Simple comparison callback using structural [Any.equals] equality
 */
val StructurallyEqual = fun(old: Any?, new: Any?) = old == new

/**
 * Simple comparison callback that always returns false, for mutable objects that will be
 * compared with the same reference.
 *
 * In this case we cannot correctly compare for equality, and so we trust that something else
 * correctly triggered a recomposition.
 */
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
 * @sample androidx.compose.samples.DelegatedReadOnlyStateSample
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
 * @sample androidx.compose.samples.DelegatedStateSample
 */
@Suppress("NOTHING_TO_INLINE")
inline operator fun <T> MutableState<T>.setValue(thisObj: Any?, property: KProperty<*>, value: T) {
    this.value = value
}

/**
 * The ModelMutableState class is a single value holder whose reads and writes are observed by
 * Compose.
 * Additionally, writes to it are transacted as part of the [Framed] transaction system.
 * `state` and `stateFor` composables.
 *
 * @property value the wrapped value
 * @property areEquivalent function used for comparing old and new [value]s to determine whether
 * to trigger a recomposition or not.
 *
 * @see [state]
 * @see [stateFor]
 * @see [mutableStateOf]
 */
private class FramedMutableState<T>(
    value: T,
    val areEquivalent: (old: T, new: T) -> Boolean
) : Framed, MutableState<T> {
    /* NOTE(lmr): When this module is compiled with IR, we will need to remove the below Framed implementation */

    @Suppress("UNCHECKED_CAST")
    override var value: T
        get() = next.readable(this).value
        set(value) = next.withCurrent {
            if (!areEquivalent(it.value, value)) {
                next.writable(this).value = value
            }
        }

    private var next: StateRecord<T> = StateRecord(value)

    init {
        _created(this)
    }

    override val firstFrameRecord: Record
        get() = next

    override fun prependFrameRecord(value: Record) {
        value.next = next
        @Suppress("UNCHECKED_CAST")
        next = value as StateRecord<T>
    }

    private class StateRecord<T>(myValue: T) : AbstractRecord() {
        override fun assign(value: Record) {
            @Suppress("UNCHECKED_CAST")
            this.value = (value as StateRecord<T>).value
        }

        override fun create(): Record =
            StateRecord(value)

        var value: T = myValue
    }

    /**
     * The componentN() operators allow state objects to be used with the property destructuring syntax
     *
     * var (foo, setFoo) = state { 0 }
     * setFoo(123) // set
     * foo == 123 // get
     */
    override operator fun component1(): T = value

    override operator fun component2(): (T) -> Unit = { value = it }
}
