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

/**
 * This is the public version of the Effect constructor. It is meant to be used to compose effects together to create custom effects.
 *
 * For example, a custom `observeUser` Effect might look something like this:
 *
 * @sample androidx.compose.samples.observeUserSample
 *
 * @param block the executable block of code that returns the value of the effect, run in the context of the Effect
 */
@Deprecated(
    "Effects have been removed. Use @Composable functions instead.",
    ReplaceWith(""),
    DeprecationLevel.ERROR
)
@Composable
fun <T> effectOf(block: () -> T): T = block()

/**
 * A CommitScope represents an object that executes some code and has a cleanup in the context of the Composition lifecycle.
 * It has an "onDispose" operation to cleanup anything that it created whenever it leaves the composition.
 */
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
private val emptyCommit: CommitScope.() -> Unit = {}

@PublishedApi
internal class PreCommitScopeImpl(
    internal val onCommit: CommitScope.() -> Unit
) : CommitScope, CompositionLifecycleObserver {
    internal var disposeCallback = emptyDispose

    override fun onDispose(callback: () -> Unit) {
        require(disposeCallback === emptyDispose) {
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
) : CommitScope, CompositionLifecycleObserver, ChoreographerFrameCallback {

    private var disposeCallback = emptyDispose
    private var hasRun = false

    override fun onDispose(callback: () -> Unit) {
        require(disposeCallback === emptyDispose) {
            "onDispose(...) should only be called once"
        }
        disposeCallback = callback
    }

    override fun doFrame(frameTimeNanos: Long) {
        hasRun = true
        onCommit(this)
    }

    override fun onEnter() {
        Choreographer.postFrameCallback(this)
    }

    override fun onLeave() {
        // If `onCommit` hasn't executed yet, we should not call `onDispose`. We should document
        // somewhere the invariants we intend to have around call order for these.
        if (hasRun) {
            disposeCallback()
        } else {
            Choreographer.removeFrameCallback(this)
        }
    }
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
@Composable
fun onActive(callback: CommitScope.() -> Unit) {
    remember { PostCommitScopeImpl(callback) }
}

/**
 * An effect used to schedule work to be done when the effect leaves the composition.
 *
 * The `onDispose` effect is essentially a convenience effect for `onPreCommit(true) { onDispose { ... } }`.
 *
 * @param callback The lambda to be executed when the effect leaves the composition.
 *
 * @see [onCommit]
 * @see [onPreCommit]
 * @see [onActive]
 */
@Composable
fun onDispose(callback: () -> Unit) {
    remember { PreCommitScopeImpl(emptyCommit).also { it.disposeCallback = callback } }
}

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
@Composable
fun onCommit(callback: CommitScope.() -> Unit) {
    currentComposerNonNull.changed(PostCommitScopeImpl(callback))
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
@Composable
/*inline*/ fun </*reified*/ V1> onCommit(
    v1: V1,
    /*noinline*/
    callback: CommitScope.() -> Unit
) {
    remember(v1) { PostCommitScopeImpl(callback) }
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
@Composable
/*inline*/ fun </*reified*/ V1, /*reified*/ V2> onCommit(
    v1: V1,
    v2: V2,
    /*noinline*/
    callback: CommitScope.() -> Unit
) {
    remember(v1, v2) { PostCommitScopeImpl(callback) }
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
@Composable
fun onCommit(vararg inputs: Any?, callback: CommitScope.() -> Unit) {
    remember(*inputs) { PostCommitScopeImpl(callback) }
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
@Composable
fun onPreCommit(callback: CommitScope.() -> Unit) {
    currentComposerNonNull.changed(PreCommitScopeImpl(callback))
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
@Composable
/*inline*/ fun </*reified*/ V1> onPreCommit(
    v1: V1,
    /*noinline*/
    callback: CommitScope.() -> Unit
) {
    remember(v1) { PreCommitScopeImpl(callback) }
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
@Composable
/*inline*/ fun </*reified*/ V1, /*reified*/ V2> onPreCommit(
    v1: V1,
    v2: V2,
    /*noinline*/
    callback: CommitScope.() -> Unit
) {
    remember(v1, v2) { PreCommitScopeImpl(callback) }
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
@Composable
fun onPreCommit(vararg inputs: Any?, callback: CommitScope.() -> Unit) {
    remember(*inputs) { PreCommitScopeImpl(callback) }
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
 * An Effect used to get the value of an ambient at a specific position during composition.
 *
 * @param key The Ambient that you want to consume the value of
 * @return An Effect that resolves to the current value of the Ambient
 *
 * @see [Ambient]
 */
@Composable
@Deprecated(
    "Use Ambient<T>.current instead",
    ReplaceWith("key.current"),
    DeprecationLevel.WARNING
)
fun <T> ambient(key: Ambient<T>): T = key.current

/**
 * An Effect to get the nearest invalidation lambda to the current point of composition. This can be used to
 * trigger an invalidation on the composition locally to cause a recompose.
 */
@Composable
val invalidate: () -> Unit get() {
    return currentComposerNonNull.let {
        val scope = it.currentRecomposeScope ?: error("no recompose scope found")
        scope.used = true
        return@let { scope.invalidate() }
    }
}

/**
 * An Effect to construct a CompositionReference at the current point of composition. This can be used
 * to run a separate composition in the context of the current one, preserving ambients and propagating
 * invalidations.
 */
@Composable
fun compositionReference(): CompositionReference {
    return currentComposerNonNull.buildReference()
}

/**
 * IMPORTANT:
 * This global operator is TEMPORARY, and should be removed whenever an answer for contextual composers is reached. At that time, the
 * unaryPlus operator on the composer itself is the one that should be used.
 *
 * Resolves the effect and returns the result.
 */
@Deprecated(
    "The unary plus for effects is no longer needed. Remove it.",
    ReplaceWith(""),
    DeprecationLevel.ERROR
)
operator fun <T : Any?> T.unaryPlus(): T = this
