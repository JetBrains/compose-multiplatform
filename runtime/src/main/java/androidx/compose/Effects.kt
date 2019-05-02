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

@file:Suppress("unused")

package androidx.compose

import android.view.Choreographer
import androidx.annotation.CheckResult
import androidx.compose.annotations.Hide
import androidx.compose.frames.AbstractRecord
import androidx.compose.frames.Framed
import androidx.compose.frames.Record
import androidx.compose.frames._created
import androidx.compose.frames.readable
import androidx.compose.frames.writable
import kotlin.reflect.KProperty

/**
 * This is just a sentinel object that represents the absence of an explicit key being defined. This is necessary because
 * we want `null` to be a valid key, and not the absence of one.
 */
private val absentKey = object {}

/**
 * This creates a composite key of any value to be used as the key for the group of an effect
 */
private fun joinKey(left: Any?, right: Any?): Any = JoinedKey(left, right)

/**
 * The Effect class is really an opaque class that holds a block of executable code that is meant to be executed positionally in the
 * context of a composition. Since this block is positional, it must also know how to construct its own "key".
 *
 * NOTE: Effects are equivalent to [Composable] functions that are able to have a return value, where the [unaryPlus] operator
 * is the manner in which it is "invoked". Effect will likely go away in favor of unifying with [Composable].
 *
 * @see [Composable]
 */
@EffectsDsl
class Effect<T> internal constructor(
    /**
     * This is a lambda that gets executed in the context of this effect. In most cases, this is where the logic of the effect goes.
     */
    @PublishedApi
    internal val block: Effect<T>.() -> T,

    /**
     * This is an optional parameter to store a key or combined key that will be used when the effect gets positionally memoized.
     */
    private val dataKey: Any? = absentKey
) {

    /**
     * Effects need to interact with the ViewComposer in order to work, however most custom effects will simply compose themselves
     * from the primitives, as opposed to using the ViewComposer directly.
     */
    @Hide
    lateinit var context: Composer<*>

    /**
     * We add a composer here with type Unit so that it blocks people from invoking composables where there is a
     * receiver scope of type Effect<T>
     *
     * @suppress
     */
    @Hide
    val composer = Unit

    /**
     * This method constructs the key of the effect. This is done primarily just as a performance optimization for the common
     * case of `dataKey` not being defined, which should be the common case
     */
    @Hide
    fun constructKey(key: Int) = if (dataKey === absentKey) key else joinKey(
        key,
        dataKey
    )

    /**
     * This is the call that "unwraps" the effect into a value, and participates in positional memoization. It is important
     * that this call gets inlined, as it is where the `sourceLocation()` gets used and does the positional memoization.
     *
     * The interesting thing here is that we see that an Effect is really little more than a controlled way of creating
     * a group in the slot table.
     */
    @Suppress("NOTHING_TO_INLINE")
    @Hide
    /* inline */ fun resolve(
        composerContext: Composer<*>,
        key: Int = sourceLocation()
    ): T = with(composerContext) {
        this@Effect.context = this
        startGroup(constructKey(key))
        val result = block()
        endGroup()
        return result
    }

    /**
     * The unaryPlus operator, in the scope of an effect, is an alias to resolving the effect
     */
    @Suppress("NOTHING_TO_INLINE")
    /* inline */ operator fun <V> Effect<V>.unaryPlus(): V = resolve(this@Effect.context)
}

/**
 * This is the public version of the Effect constructor. It is meant to be used to compose effects together to create custom effects.
 *
 * For example, a custom `observeUser` Effect might look something like this:
 *
 *    fun observeUser(userId: Int) = effectOf<User?> {
 *        val user = +stateFor<User?>(userId) { null }
 *        +onCommit(userId) {
 *            val subscription = UserAPI.subscribeToUser(userId) {
 *                user.value = it
 *            }
 *            onDispose {
 *                subscription.unsubscribe()
 *            }
 *        }
 *        user.value
 *    }
 *
 * @param block the executable block of code that returns the value of the effect, run in the context of the Effect
 */
fun <T> effectOf(block: Effect<T>.() -> T): Effect<T> =
    Effect(block)

/**
 * A CommitScope represents an object that executes some code and has a cleanup in the context of the Composition lifecycle.
 * It has an "onDispose" operation to cleanup anything that it created whenever it leaves the composition.
 */
@EffectsDsl
interface CommitScope {
    /**
     * Provide a lambda which will be executed as this CommitScope leaves the composition. It will be executed only once. Use this to
     * schedule cleanup for anything that you construct during the CommitScope's creation.
     *
     * @param callback A callback to be executed when this CommitScope leaves the composition.
     */
    fun onDispose(callback: () -> Unit)
}

/**
 * For convenience, this is just an empty lambda that we will call on CommitScopes where the user has not defined an
 * onDispose. Saving this into a constant saves us an allocation on every initialization
 */
private val emptyDispose: () -> Unit = {}

@PublishedApi
internal class PreCommitScopeImpl(
    internal val onCommit: CommitScope.() -> Unit
) : CommitScope, CompositionLifecycleObserver {
    private var disposeCallback = emptyDispose

    override fun onDispose(callback: () -> Unit) {
        assert(disposeCallback === emptyDispose) {
            "onDispose(...) should only be called once"
        }
        disposeCallback = callback
    }

    override fun onEnter() {
        onCommit(this)
    }

    override fun onLeave() {
        disposeCallback()
    }
}

@PublishedApi
internal class PostCommitScopeImpl(
    internal val onCommit: CommitScope.() -> Unit
) : CommitScope, CompositionLifecycleObserver, Choreographer.FrameCallback {

    private var disposeCallback = emptyDispose
    private var hasRun = false

    override fun onDispose(callback: () -> Unit) {
        assert(disposeCallback === emptyDispose) {
            "onDispose(...) should only be called once"
        }
        disposeCallback = callback
    }

    override fun doFrame(frameTimeNanos: Long) {
        hasRun = true
        onCommit(this)
    }

    override fun onEnter() {
        // TODO(lmr): we should eventually move this to an expect/actual "scheduler" of some sort
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun onLeave() {
        // If `onCommit` hasn't executed yet, we should not call `onDispose`. We should document
        // somewhere the invariants we intend to have around call order for these.
        if (hasRun) {
            disposeCallback()
        } else {
            Choreographer.getInstance().removeFrameCallback(this)
        }
    }
}

/**
 * The key effect is a primitive effect that allows for an effect to have a custom group key. This allows for effects to be
 * associated with data. If you are constructing effects based on data such as lists or collections, keys can be used to help
 * Compose determine which effects should be removed or added.  Any other effects can be created inside of the block of the
 * key effect.
 *
 * Example:
 *
 *     for (el in elements)
 *       val selected = +key(el.id) { +state { false } }
 *       ListItem(item=el, selected=selected)
 *
 * @param v1 The value to use as the key. This will be compared to its previous value using `Object.equals`
 * @param block The block to execute other effects in
 */
@CheckResult(suggest = "+")
fun <T, V1> key(v1: V1, block: Effect<T>.() -> T) =
    Effect(block, v1)

/**
 * The key effect is a primitive effect that allows for an effect to have a custom group key. This allows for effects to be
 * associated with data. If you are constructing effects based on data such as lists or collections, keys can be used to help
 * Compose determine which effects should be removed or added.  Any other effects can be created inside of the block of the
 * key effect.
 *
 * A compound key will be created from both [v1] and [v2].
 *
 * Example:
 *
 *     for (el in elements)
 *       val selected = +key(el.id, parentId) { +state { false } }
 *       ListItem(item=el, selected=selected)
 *
 * @param v1 The first value to use as a key. This will be compared to its previous value using `Object.equals`
 * @param v2 The second value to use as a key. This will be compared to its previous value using `Object.equals`
 * @param block The block to execute other effects in
 */
@CheckResult(suggest = "+")
fun <T, V1, V2> key(v1: V1, v2: V2, block: Effect<T>.() -> T) =
    Effect(block, joinKey(v1, v2))

/**
 * The key effect is a primitive effect that allows for an effect to have a custom group key. This allows for effects to be
 * associated with data. If you are constructing effects based on data such as lists or collections, keys can be used to help
 * Compose determine which effects should be removed or added.  Any other effects can be created inside of the block of the key
 * effect.
 *
 * Example:
 *
 *     for (el in elements)
 *       val selected = +key(el.id, parentId) { +state { false } }
 *       ListItem(item=el, selected=selected)
 *
 * @param inputs The set of values to be used to create a compound key. This will be compared to its previous value using `Object.equals`
 * @param block The block to execute other effects in
 */
@CheckResult(suggest = "+")
fun <T> key(vararg inputs: Any?, block: Effect<T>.() -> T) =
    Effect(
        block,
        inputs.reduce { acc, item -> joinKey(acc, item) })

/**
 * An Effect that positionally memoizes the result of a computation.
 *
 * @param calculation A function to produce the result
 * @return The result of the calculation, or the cached value from the composition
 */
@CheckResult(suggest = "+")
/* inline */ fun <T> memo(/* crossinline */ calculation: () -> T) = effectOf<T> {
    context.remember(calculation)
}

/**
 * An Effect that positionally memoizes the result of a computation.
 *
 * @param v1 An input to the memoization. If this value changes, the calculation will be re-executed.
 * @param calculation A function to produce the result
 * @return The result of the calculation, or the cached value from the composition
 */
@CheckResult(suggest = "+")
/* inline */ fun <T, /* reified */ V1> memo(
    v1: V1,
    /* crossinline */
    calculation: () -> T
) = effectOf<T> {
    context.remember(v1, calculation)
}

/**
 * An Effect that positionally memoizes the result of a computation.
 *
 * @param v1 An input to the memoization. If this value changes, the calculation will be re-executed.
 * @param v2 An input to the memoization. If this value changes, the calculation will be re-executed.
 * @param calculation A function to produce the result
 * @return The result of the calculation, or the cached value from the composition
 */
@CheckResult(suggest = "+")
/* inline */ fun <T, /* reified */ V1, /* reified */ V2> memo(
    v1: V1,
    v2: V2,
    /* crossinline */
    calculation: () -> T
) = effectOf<T> {
    context.remember(v1, v2, calculation)
}

/**
 * An Effect that positionally memoizes the result of a computation.
 *
 * @param inputs The inputs to the memoization. If any of these values change, the calculation will be re-executed.
 * @param calculation A function to produce the result
 * @return The result of the calculation, or the cached value from the composition
 */
@CheckResult(suggest = "+")
fun <T> memo(vararg inputs: Any?, calculation: () -> T) = effectOf<T> {
    context.remember(*inputs) { calculation() }
}

/**
 * An effect used to observe the lifecycle of the composition. The [callback] will execute once initially after the first composition
 * is applied, and then will not fire again. The [callback] will get executed with a receiver scope that has an
 * [onDispose][CommitScope.onDispose] method which can be used to schedule a callback to be executed once whenever the effect leaves
 * the composition
 *
 * The `onActive` effect is essentially a convenience effect for `onCommit(true) { ... }`.
 *
 * @param callback The lambda to execute when the composition commits for the first time and becomes active.
 *
 * @see [onCommit]
 * @see [onPreCommit]
 * @see [onDispose]
 */
@CheckResult(suggest = "+")
fun onActive(callback: CommitScope.() -> Unit) = effectOf<Unit> {
    context.remember { PostCommitScopeImpl(callback) }
}

/**
 * An effect used to schedule work to be done when the effect leaves the composition.
 *
 * The `onDispose` effect is essentially a convenience effect for `onActive { onDispose { ... } }`.
 *
 * @param callback The lambda to be executed when the effect leaves the composition.
 *
 * @see [onCommit]
 * @see [onPreCommit]
 * @see [onActive]
 */
@CheckResult(suggest = "+")
fun onDispose(callback: () -> Unit) = onActive { onDispose(callback) }

/**
 * The onCommit effect is a lifecycle effect that will execute [callback] every time the composition commits. It is useful for
 * executing code in lock-step with composition that has side-effects. The [callback] will get executed with a receiver scope that has an
 * [onDispose][CommitScope.onDispose] method which can be used to schedule a callback to schedule code that cleans up the code in the
 * callback.
 *
 * @param callback The lambda to be executed when the effect is committed to the composition.
 *
 * @see [onDispose]
 * @see [onPreCommit]
 * @see [onActive]
 */
@CheckResult(suggest = "+")
fun onCommit(callback: CommitScope.() -> Unit) = effectOf<Unit> {
    context.changed(PostCommitScopeImpl(callback))
}

/**
 * The onCommit effect is a lifecycle effect that will execute [callback] every time the inputs to the effect have changed. It is useful for
 * executing code in lock-step with composition that has side-effects that are based on the inputs. The [callback] will get executed with a
 * receiver scope that has an [onDispose][CommitScope.onDispose] method which can be used to schedule a callback to schedule code that
 * cleans up the code in the callback.
 *
 * @param v1 The input which will be compared across compositions to determine if [callback] will get executed.
 * @param callback The lambda to be executed when the effect is committed to the composition.
 *
 * @see [onDispose]
 * @see [onPreCommit]
 * @see [onActive]
 */
@CheckResult(suggest = "+")
/* inline */ fun </* reified */ V1> onCommit(
    v1: V1,
    /* noinline */
    callback: CommitScope.() -> Unit
) = effectOf<Unit> {
    context.remember(v1) { PostCommitScopeImpl(callback) }
}

/**
 * The onCommit effect is a lifecycle effect that will execute [callback] every time the inputs to the effect have changed. It is useful for
 * executing code in lock-step with composition that has side-effects that are based on the inputs. The [callback] will get executed with a
 * receiver scope that has an [onDispose][CommitScope.onDispose] method which can be used to schedule a callback to schedule code that
 * cleans up the code in the callback.
 *
 * @param v1 An input value which will be compared across compositions to determine if [callback] will get executed.
 * @param v2 An input value which will be compared across compositions to determine if [callback] will get executed.
 * @param callback The lambda to be executed when the effect is committed to the composition.
 *
 * @see [onDispose]
 * @see [onPreCommit]
 * @see [onActive]
 */
@CheckResult(suggest = "+")
/* inline */ fun </* reified */ V1, /* reified */ V2> onCommit(
    v1: V1,
    v2: V2,
    /* noinline */
    callback: CommitScope.() -> Unit
) = effectOf<Unit> {
    context.remember(v1, v2) { PostCommitScopeImpl(callback) }
}

/**
 * The onCommit effect is a lifecycle effect that will execute [callback] every time the inputs to the effect have changed. It is useful for
 * executing code in lock-step with composition that has side-effects that are based on the inputs. The [callback] will get executed with a
 * receiver scope that has an [onDispose][CommitScope.onDispose] method which can be used to schedule a callback to schedule code that
 * cleans up the code in the callback.
 *
 * @param inputs A set of inputs which will be compared across compositions to determine if [callback] will get executed.
 * @param callback The lambda to be executed when the effect is committed to the composition.
 *
 * @see [onDispose]
 * @see [onPreCommit]
 * @see [onActive]
 */
@CheckResult(suggest = "+")
fun onCommit(vararg inputs: Any?, callback: CommitScope.() -> Unit) =
    effectOf<Unit> {
        context.remember(*inputs) { PostCommitScopeImpl(callback) }
    }


/**
 * The onPreCommit effect is a lifecycle effect that will execute [callback] every time the composition commits,
 * but before those changes have been reflected on the screen. It is useful for executing code that needs to
 * update in response to a composition and it is critical that the previous results are never seen by the user.
 * If it is not critical, [onCommit] is recommended instead. The [callback] will get executed with a receiver scope that has an
 * [onDispose][CommitScope.onDispose] method which can be used to schedule a callback to schedule code that cleans up the code in the
 * callback.
 *
 * @param callback The lambda to be executed when the effect is committed to the composition.
 *
 * @see [onDispose]
 * @see [onPreCommit]
 * @see [onActive]
 */
@CheckResult(suggest = "+")
fun onPreCommit(callback: CommitScope.() -> Unit) =
    effectOf<Unit> {
        context.changed(PreCommitScopeImpl(callback))
    }

/**
 * The onPreCommit effect is a lifecycle effect that will execute [callback] every time the inputs to the
 * effect have changed, but before those changes have been reflected on the screen. It is useful for executing
 * code that needs to update in response to a composition and it is critical that the previous results are
 * never seen by the user. If it is not critical, [onCommit] is recommended instead. The [callback] will get
 * executed with a receiver scope that has an [onDispose][CommitScope.onDispose] method which can be used to
 * schedule a callback to schedule code that cleans up the code in the callback.
 *
 * @param v1 The input which will be compared across compositions to determine if [callback] will get executed.
 * @param callback The lambda to be executed when the effect is committed to the composition.
 *
 * @see [onDispose]
 * @see [onCommit]
 * @see [onActive]
 */
@CheckResult(suggest = "+")
/* inline */ fun </* reified */ V1> onPreCommit(
    v1: V1,
    /* noinline */
    callback: CommitScope.() -> Unit
) = effectOf<Unit> {
    context.remember(v1) { PreCommitScopeImpl(callback) }
}

/**
 * The onPreCommit effect is a lifecycle effect that will execute [callback] every time the inputs to the
 * effect have changed, but before those changes have been reflected on the screen. It is useful for executing
 * code that needs to update in response to a composition and it is critical that the previous results are
 * never seen by the user. If it is not critical, [onCommit] is recommended instead. The [callback] will get
 * executed with a receiver scope that has an [onDispose][CommitScope.onDispose] method which can be used to
 * schedule a callback to schedule code that cleans up the code in the callback.
 *
 * @param v1 An input value which will be compared across compositions to determine if [callback] will get executed.
 * @param v2 An input value which will be compared across compositions to determine if [callback] will get executed.
 * @param callback The lambda to be executed when the effect is committed to the composition.
 *
 * @see [onDispose]
 * @see [onCommit]
 * @see [onActive]
 */
@CheckResult(suggest = "+")
/* inline */ fun </* reified */ V1, /* reified */ V2> onPreCommit(
    v1: V1,
    v2: V2,
    /* noinline */
    callback: CommitScope.() -> Unit
) = effectOf<Unit> {
    context.remember(v1, v2) { PreCommitScopeImpl(callback) }
}

/**
 * The onPreCommit effect is a lifecycle effect that will execute [callback] every time the inputs to the
 * effect have changed, but before those changes have been reflected on the screen. It is useful for executing
 * code that needs to update in response to a composition and it is critical that the previous results are
 * never seen by the user. If it is not critical, [onCommit] is recommended instead. The [callback] will get
 * executed with a receiver scope that has an [onDispose][CommitScope.onDispose] method which can be used to
 * schedule a callback to schedule code that cleans up the code in the callback.
 *
 * @param inputs A set of inputs which will be compared across compositions to determine if [callback] will get executed.
 * @param callback The lambda to be executed when the effect is committed to the composition.
 *
 * @see [onDispose]
 * @see [onCommit]
 * @see [onActive]
 */
@CheckResult(suggest = "+")
fun onPreCommit(vararg inputs: Any?, callback: CommitScope.() -> Unit) =
    effectOf<Unit> {
        context.remember(*inputs) { PreCommitScopeImpl(callback) }
    }

/**
 * An effect to introduce a state value of type [T] into a composition.
 *
 * This is useful when you have a value that you would like to locally mutate and use in the context of a composition. Since
 * the returned [State] instance implements [Model], changes to the [State.value] property will be automatically tracked in
 * composition and schedule a recompose.
 *
 * The [State] class can be used several different ways. For example, the most basic way is to store the returned state
 * value into a local immutable variable, and then set the [State.value] property on it.
 *
 * Example:
 *
 *    @Composable
 *    fun Example() {
 *        val count = +state { 0 }
 *
 *        TextView(text="You clicked ${count.value} times")
 *        Button(
 *            text="Click me",
 *            onClick={ count.value += 1 }
 *        )
 *    }
 *
 * Additionally, you can destructure the [State] object into a value and a "setter" function.
 *
 * Example:
 *
 *    @Composable
 *    fun Example() {
 *        val (count, setCount) = +state { 0 }
 *
 *        TextView(text="You clicked ${count} times")
 *        Button(
 *            text="Click me",
 *            onClick={ setCount(count + 1) }
 *        )
 *    }
 *
 * Finally, the [State] instance can be used as a variable delegate to a local mutable variable.
 *
 * Example:
 *
 *    @Composable
 *    fun Example() {
 *        var count by +state { 0 }
 *
 *        TextView(text="You clicked $count times")
 *        Button(
 *            text="Click me",
 *            onClick={ count += 1 }
 *        )
 *    }
 *
 *
 * @param init A factory function to create the initial value of this state
 * @return An [Model] instance of [State] that wraps the value.
 *
 * @see [stateFor]
 * @see [model]
 * @see [modelFor]
 */
@CheckResult(suggest = "+")
/* inline */ fun <T> state(/* crossinline */ init: () -> T) =
    memo { State(init()) }

/**
 * An effect to introduce a state value of type [T] into a composition that will last as long as the input [v1] does not change.
 *
 * This is useful when you have a value that you would like to locally mutate and use in the context of a composition, and its
 * value is scoped to another value and you want it to be reset every time the other value changes.
 *
 * The returned [State] instance implements [Model] so that changes to the [State.value] property will be automatically tracked in
 * composition and schedule a recompose.
 *
 * @param v1 An input value that, when changed, will cause the state to reset and [init] to be rerun
 * @param init A factory function to create the initial value of this state
 * @return An [Model] instance of [State] that wraps the value.
 *
 * @see [state]
 * @see [model]
 * @see [modelFor]
 */
@CheckResult(suggest = "+")
/* inline */ fun <T, /* reified */ V1> stateFor(v1: V1, /* crossinline */ init: () -> T) =
    memo(v1) { State(init()) }

/**
 * An effect to introduce a state value of type [T] into a composition that will last as long as the inputs [v1] and [v2] do not change.
 *
 * This is useful when you have a value that you would like to locally mutate and use in the context of a composition, and its
 * value is scoped to another value and you want it to be reset every time the other value changes.
 *
 * The returned [State] instance implements [Model] so that changes to the [State.value] property will be automatically tracked in
 * composition and schedule a recompose.
 *
 * @param v1 An input value that, when changed, will cause the state to reset and [init] to be rerun
 * @param v2 An input value that, when changed, will cause the state to reset and [init] to be rerun
 * @param init A factory function to create the initial value of this state
 * @return An [Model] instance of [State] that wraps the value.
 *
 * @see [state]
 * @see [model]
 * @see [modelFor]
 */
@CheckResult(suggest = "+")
/* inline */ fun <T, /* reified */ V1, /* reified */ V2> stateFor(
    v1: V1,
    v2: V2,
    /* crossinline */
    init: () -> T
) = memo(v1, v2) { State(init()) }

/**
 * An effect to introduce a state value of type [T] into a composition that will last as long as the inputs [inputs] do not change.
 *
 * This is useful when you have a value that you would like to locally mutate and use in the context of a composition, and its
 * value is scoped to another value and you want it to be reset every time the other value changes.
 *
 * The returned [State] instance implements [Model] so that changes to the [State.value] property will be automatically tracked in
 * composition and schedule a recompose.
 *
 * @param inputs A set of inputs such that, when any of them have changed, will cause the state to reset and [init] to be rerun
 * @param init A factory function to create the initial value of this state
 * @return An [Model] instance of [State] that wraps the value.
 *
 * @see [state]
 * @see [model]
 * @see [modelFor]
 */
@CheckResult(suggest = "+")
/* inline */ fun <T> stateFor(vararg inputs: Any?, /* crossinline */ init: () -> T) =
    memo(*inputs) { State(init()) }

/**
 * The State class is an @Model class meant to wrap around a single value. It is used in the
 * `+state` and `+stateFor` effects.
 *
 * @property value the wrapped value
 *
 * @see [Model]
 * @see [state]
 * @see [stateFor]
 */
@Model
class State<T> @PublishedApi internal constructor(value: T) : Framed {
    /* NOTE(lmr): When this module is compiled with IR, we will need to remove the below Framed implementation */

    @Suppress("UNCHECKED_CAST")
    var value: T
        get() = next.readable(this).value
        set(value) {
            next.writable(this).value = value
        }

    private var next: StateRecord<T> =
        StateRecord(value)

    init {
        _created(this)
    }

    // NOTE(lmr): ideally we can compile `State` with our own compiler so that this is not visible
    @Hide
    override val firstFrameRecord: Record
        get() = next

    // NOTE(lmr): ideally we can compile `State` with our own compiler so that this is not visible
    @Hide
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
     * var (foo, setFoo) = +state { 0 }
     * setFoo(123) // set
     * foo == 123 // get
     */
    operator fun component1(): T = value

    operator fun component2(): (T) -> Unit = { value = it }

    /**
     * The getValue/setValue operators allow State to be used as a local variable with a delegate:
     *
     * var foo by +state { 0 }
     * foo += 123 // uses setValue(...)
     * foo == 123 // uses getValue(...)
     */
    operator fun getValue(thisObj: Any?, property: KProperty<*>): T = value

    operator fun setValue(thisObj: Any?, property: KProperty<*>, next: T) {
        value = next
    }
}

/**
 * The model effect is an alias to the `memo` effect, but the semantics behind how it is used are different from
 * memoization, so we provide new named functions for the different use cases.
 *
 * In the case of memoization, the "inputs" of the calculation should be provided for correctness, implying if the
 * inputs have not changed, the cached result and executing the calculation again would produce semantically identical
 * results.
 *
 * In the case of "model", we are actually *intentionally* under-specifying the inputs of the calculation to cause an
 * object to be cached across compositions. In this case, the calculation function is *not* a pure function of the inputs,
 * and instead we are relying on the "incorrect" memoization to produce state that survives across compositions.
 *
 * Because these usages are so contradictory to one another, we provide a `model` alias for `memo` that is expected to
 * be used in these cases instead of `memo`.
 */

/**
 * An effect to introduce a state value of type [T] into a composition. The [init] lambda will be called only once to create the
 * initial value, and then that instance will be used for the lifetime of the composition.
 *
 * This is useful when you have a [Model] class that you would like to instantiate and use in the context of a composition. Since
 * the returned.
 *
 * @param init A lambda that creates the [Model] instance
 * @return The Effect which resolves to the result of [init] cached across compositions
 *
 * @see [modelFor]
 * @see [state]
 * @see [stateFor]
 */
@CheckResult(suggest = "+")
/* inline */ fun <T> model(/* crossinline */ init: () -> T) = memo { init() }

/**
 * An effect to introduce a state value of type [T] into a composition. The [init] lambda will be called only once to create the
 * initial value, and then that instance will be used as long as the input [v1] has not changed.
 *
 * This is useful when you have a [Model] class that you would like to instantiate and use in the context of a composition. Since
 * the returned.
 *
 * @param v1 An input value that, when changed, will cause the state to reset and [init] to be rerun
 * @param init A lambda that creates the [Model] instance
 * @return The Effect which resolves to the result of [init] cached across compositions
 *
 * @see [model]
 * @see [state]
 * @see [stateFor]
 */
@CheckResult(suggest = "+")
/* inline */ fun <T, /* reified */ V1> modelFor(v1: V1, /* crossinline */ init: () -> T) =
    memo(v1) { init() }

/**
 * An effect to introduce a state value of type [T] into a composition. The [init] lambda will be called only once to create the
 * initial value, and then that instance will be used as long as the inputs [v1] and [v2] have not changed.
 *
 * This is useful when you have a [Model] class that you would like to instantiate and use in the context of a composition. Since
 * the returned.
 *
 * @param v1 An input value that, when changed, will cause the state to reset and [init] to be rerun
 * @param v2 An input value that, when changed, will cause the state to reset and [init] to be rerun
 * @param init A lambda that creates the [Model] instance
 * @return The Effect which resolves to the result of [init] cached across compositions
 *
 * @see [model]
 * @see [state]
 * @see [stateFor]
 */
@CheckResult(suggest = "+")
/* inline */ fun <
        T,
        /* reified */ V1,
        /* reified */ V2
        > modelFor(v1: V1, v2: V2, /* crossinline */ init: () -> T) =
    memo(v1, v2) { init() }

/**
 * An effect to introduce a state value of type [T] into a composition. The [init] lambda will be called only once to create the
 * initial value, and then that instance will be used  as long as the the values of [inputs] have not changed.
 *
 * This is useful when you have a [Model] class that you would like to instantiate and use in the context of a composition. Since
 * the returned.
 *
 * @param inputs A set of inputs such that, when any of them have changed, the state will reset and [init] will be rerun
 * @param init A lambda that creates the [Model] instance
 * @return The Effect which resolves to the result of [init] cached across compositions
 *
 * @see [model]
 * @see [state]
 * @see [stateFor]
 */
@CheckResult(suggest = "+")
/* inline */ fun <T> modelFor(vararg inputs: Any?, /* crossinline */ init: () -> T) =
    memo(*inputs) { init() }

/**
 * An Effect used to get the value of an ambient at a specific position during composition.
 *
 * @param key The Ambient that you want to consume the value of
 * @return An Effect that resolves to the current value of the Ambient
 *
 * @see [Ambient]
 */
@CheckResult(suggest = "+")
fun <T> ambient(key: Ambient<T>) = effectOf<T> {
    context.consume(key)
}

/**
 * An Effect to get the nearest invalidation lambda to the current point of composition. This can be used to
 * trigger an invalidation on the composition locally to cause a recompose.
 */
val invalidate = effectOf<() -> Unit> {
    val scope = context.currentInvalidate ?: error("no recompose scope found")
    return@effectOf { scope.invalidate?.let { it(false) } }
}

/**
 * An Effect to construct a CompositionReference at the current point of composition. This can be used
 * to run a separate composition in the context of the current one, preserving ambients and propagating
 * invalidations.
 */
fun compositionReference() = effectOf<CompositionReference> {
    context.buildReference()
}

/**
 * IMPORTANT:
 * This global operator is TEMPORARY, and should be removed whenever an answer for contextual composers is reached. At that time, the
 * unaryPlus operator on the composer itself is the one that should be used.
 *
 * Resolves the effect and returns the result.
 */
@Suppress("NOTHING_TO_INLINE")
/* inline */ operator fun <T> Effect<T>.unaryPlus(): T = resolve(currentComposerNonNull)