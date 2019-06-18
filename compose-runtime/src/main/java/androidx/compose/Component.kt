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

/**
 * A [Component] is a class variant of a [Composable] function. [Component]s must implement the
 * [compose] method, which behaves like the body of the function. [Component] instances are preserved
 * across compositions, much like local state in [Composable] functions that is introduced using
 * [state] or [memo].
 *
 * There is nothing that [Component] classes offer that [Composable] functions don't also offer, so
 * we currently recommend against using [Component] for most use cases.
 *
 * [Component] can map to concepts of [Composable] functions, but also have a few restrictions that
 * most classes do not have. For example:
 *
 * 1. Private properties of component classes are similar to local state of a composable function.
 * 2. Non-Private properties of component classes are similar to parameters of [Composable] functions
 * 3. Constructor parameters of component classes are similar to [Pivotal] parameters of [Composable] functions
 * 4. You should only _read_ non-private properties. Never set them.
 *
 *
 * A simple "counter" Component using private properties as local state:
 *
 *     // Definition
 *     class Counter : Component {
 *       private var count = 0
 *       override fun compose() {
 *           Button(text="Increment", onClick={ count += 1; recompose() })
 *           Button(text="Decrement", onClick={ count -= 1; recompose() })
 *           Text(text="Counter: $count")
 *       }
 *
 *
 *     // Usage:
 *     Counter()
 *
 * @see Composable
 */
@Stateful
@Suppress("PLUGIN_ERROR")
abstract class Component {
    @HiddenAttribute
    internal var recomposeCallback: ((sync: Boolean) -> Unit)? = null
    private var composing = false

    /**
     * Schedules this component to be recomposed. If you have updated any state that is used during
     * [compose], you will likely want to call this to cause [compose] to get called again and have
     * the tree updated.
     */
    protected fun recompose() {
        if (composing) return
        recomposeCallback?.invoke(false)
    }

    /**
     * @suppress
     */
    @Deprecated("Use recompose() instead", replaceWith = ReplaceWith("recompose()"))
    protected fun recomposeSync() {
        if (composing) return
        recomposeCallback?.invoke(true)
    }

    /**
     * The [compose] method is the only required method to implement in Component sub-classes. The
     * method is [Composable], and the sub-hierarchy of the Component itself needs to be composed
     * during the execution of this method.
     *
     * The [compose] method should not mutate the component itself during its own execution (in
     * other words, [compose] can be thought of as "pure").
     *
     * The compose runtime calls [compose], which means that the [compose] method should never
     * be called directly.
     */
    @Composable
    abstract fun compose()

    @Suppress("PLUGIN_ERROR")
    private fun doCompose() {
        try {
            composing = true
            compose()
        } finally {
            composing = false
        }
    }

    /**
     * @suppress
     */
    @Composable
    operator fun invoke() {
        val composer = currentComposerNonNull
        val callback = composer.startJoin(false) { doCompose() }
        doCompose()
        composer.doneJoin(false)
        recomposeCallback = callback
    }
}
