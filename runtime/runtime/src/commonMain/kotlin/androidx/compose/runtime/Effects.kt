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
package androidx.compose.runtime

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
 * @see [onDispose]
 */
@Suppress("ComposableNaming")
@Composable
fun onActive(callback: CommitScope.() -> Unit) {
    remember { PreCommitScopeImpl(callback) }
}

/**
 * An effect used to schedule work to be done when the effect leaves the composition.
 *
 * The `onDispose` effect is essentially a convenience effect for `onPreCommit(true) { onDispose { ... } }`.
 *
 * @param callback The lambda to be executed when the effect leaves the composition.
 *
 * @see [onCommit]
 * @see [onActive]
 */
@Suppress("ComposableNaming")
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
 * @see [onActive]
 */
@Suppress("NOTHING_TO_INLINE", "ComposableNaming")
@OptIn(ComposeCompilerApi::class)
@Composable
inline fun onCommit(noinline callback: CommitScope.() -> Unit) {
    currentComposer.changed(PreCommitScopeImpl(callback))
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
 * @see [onActive]
 */
@Suppress("ComposableNaming")
@Composable
/*inline*/ fun </*reified*/ V1> onCommit(
    v1: V1,
    /*noinline*/
    callback: CommitScope.() -> Unit
) {
    remember(v1) { PreCommitScopeImpl(callback) }
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
 * @see [onActive]
 */
@Suppress("ComposableNaming")
@Composable
/*inline*/ fun </*reified*/ V1, /*reified*/ V2> onCommit(
    v1: V1,
    v2: V2,
    /*noinline*/
    callback: CommitScope.() -> Unit
) {
    remember(v1, v2) { PreCommitScopeImpl(callback) }
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
 * @see [onActive]
 */
@Suppress("ComposableNaming")
@Composable
fun onCommit(vararg inputs: Any?, callback: CommitScope.() -> Unit) {
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
 * An Effect to get the nearest invalidation lambda to the current point of composition. This can be used to
 * trigger an invalidation on the composition locally to cause a recompose.
 */
val invalidate: () -> Unit @Composable get() {
    val scope = currentComposer.currentRecomposeScope ?: error("no recompose scope found")
    scope.used = true
    return { scope.invalidate() }
}

/**
 * An Effect to construct a CompositionReference at the current point of composition. This can be used
 * to run a separate composition in the context of the current one, preserving ambients and propagating
 * invalidations. When this call leaves the composition, the reference is invalidated.
 */
@Composable
@OptIn(ComposeCompilerApi::class)
fun compositionReference(): CompositionReference {
    return currentComposer.buildReference()
}