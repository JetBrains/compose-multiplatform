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
 * A global namespace to hold some Compose utility methods, such as [Compose.composeInto] and
 * [Compose.disposeComposition].
 */
object Compose {
    /**
     * Apply Code Changes will invoke the two functions before and after a code swap.
     *
     * This forces the whole view hierarchy to be redrawn to invoke any code change that was
     * introduce in the code swap.
     *
     * All these are private as within JVMTI / JNI accessibility is mostly a formality.
     */
    // NOTE(lmr): right now, this class only takes into account Emittables and Views composed using
    // compose. In reality, there might be more (ie, Vectors), and we should figure out a more
    // holistic way to capture those as well.
    private class HotReloader {
        companion object {
            private var compositions = mutableListOf<Pair<Composition, @Composable() () -> Unit>>()

            @TestOnly
            fun clearRoots() {
                EMITTABLE_ROOT_COMPONENT.clear()
                VIEWGROUP_ROOT_COMPONENT.clear()
            }

            // Called before Dex Code Swap
            @Suppress("UNUSED_PARAMETER")
            private fun saveStateAndDispose(context: Context) {
                compositions.clear()

                val emittableRoots = EMITTABLE_ROOT_COMPONENT.entries.toSet()

                for ((_, composition) in emittableRoots) {
                    compositions.add(composition to composition.composable)
                    composition.dispose()
                }

                val viewRoots = VIEWGROUP_ROOT_COMPONENT.entries.toSet()

                for ((_, composition) in viewRoots) {
                    compositions.add(composition to composition.composable)
                    composition.dispose()
                }
            }

            // Called after Dex Code Swap
            @Suppress("UNUSED_PARAMETER")
            private fun loadStateAndCompose(context: Context) {
                for ((composition, composable) in compositions) {
                    composition.compose(composable)
                }

                compositions.clear()
            }

            @TestOnly
            internal fun simulateHotReload(context: Context) {
                saveStateAndDispose(context)
                loadStateAndCompose(context)
            }
        }
    }

    private val TAG_COMPOSITION_CONTEXT = "androidx.compose.CompositionContext".hashCode()
    private val EMITTABLE_ROOT_COMPONENT = WeakHashMap<Emittable, Composition>()
    private val VIEWGROUP_ROOT_COMPONENT = WeakHashMap<ViewGroup, Composition>()

    private fun findComposition(view: View): Composition? {
        return view.getTag(TAG_COMPOSITION_CONTEXT) as? Composition
    }

    internal fun storeComposition(view: View, composition: Composition) {
        view.setTag(TAG_COMPOSITION_CONTEXT, composition)
        if (view is ViewGroup)
            VIEWGROUP_ROOT_COMPONENT[view] = composition
    }

    internal fun removeRoot(view: View) {
        view.setTag(TAG_COMPOSITION_CONTEXT, null)
        if (view is ViewGroup)
            VIEWGROUP_ROOT_COMPONENT.remove(view)
    }

    private fun findComposition(emittable: Emittable): Composition? {
        return EMITTABLE_ROOT_COMPONENT[emittable]
    }

    // TODO(b/138254844): Make findRoot/setRoot test-only & Android-only
    private fun storeComposition(emittable: Emittable, context: Composition) {
        EMITTABLE_ROOT_COMPONENT[emittable] = context
    }

    /**
     * @suppress
     */
    @TestOnly
    fun simulateHotReload(context: Context) = HotReloader.simulateHotReload(context)

    /**
     * @suppress
     */
    @TestOnly
    fun clearRoots() = HotReloader.clearRoots()

    /**
     * This method is the way to initiate a composition. The [composable] passed in will be executed
     * to compose the children of the passed in [container].  Optionally, a [parent]
     * [CompositionReference] can be provided to make the composition behave as a sub-composition of
     * the parent.  The children of [container] will be updated and maintained by the time this
     * method returns.
     *
     * It is important to call [disposeComposition] whenever this view is no longer needed in order
     * to release resources.
     *
     * @param container The view whose children is being composed.
     * @param parent The parent composition reference, if applicable. Default is null.
     * @param composable The composable function intended to compose the children of [container].
     *
     * @see Compose.disposeComposition
     * @see Composable
     */
    // TODO(lmr): rename to compose?
    @MainThread
    fun composeInto(
        container: ViewGroup,
        parent: CompositionReference? = null,
        composable: @Composable() () -> Unit
    ): Composition {
        val composition = findComposition(container)
            ?: UiComposition(container, container.context, parent).also {
                container.removeAllViews()
            }
        composition.compose(composable)
        return composition
    }

    /**
     * Disposes any composition previously run with [container] as the root. This will
     * release any resources that have been built around the composition, including all [onDispose]
     * callbacks that have been registered with [CompositionLifecycleObserver] objects.
     *
     * It is important to call this for any [Compose.composeInto] call that is made, or else you may have
     * memory leaks in your application.
     *
     * @param container The view that was passed into [Compose.composeInto] as the root container of the composition
     * @param parent The parent composition reference, if applicable.
     *
     * @see Compose.composeInto
     * @see CompositionLifecycleObserver
     */
    @MainThread
    fun disposeComposition(container: ViewGroup, parent: CompositionReference? = null) {
        // temporary easy way to call correct lifecycles on everything
        // need to remove compositionContext from context map as well
        composeInto(container, parent, emptyComposable)
        removeRoot(container)
    }

    private val emptyComposable: @Composable() () -> Unit = {}

    /**
     * This method is the way to initiate a composition. The [composable] passed in will be executed
     * to compose the children of the passed in [container].  Optionally, a [parent]
     * [CompositionReference] can be provided to make the composition behave as a sub-composition of
     * the parent.  The children of [container] will be updated and maintained by the time this
     * method returns.
     *
     * It is important to call [Compose.disposeComposition] whenever this view is no longer needed in order
     * to release resources.
     *
     * @param container The emittable whose children is being composed.
     * @param context The android [Context] to associate with this composition.
     * @param parent The parent composition reference, if applicable. Default is null.
     * @param composable The composable function intended to compose the children of [container].
     *
     * @see Compose.disposeComposition
     * @see Composable
     */
    // TODO(lmr): rename to compose?
    @MainThread
    fun composeInto(
        container: Emittable,
        context: Context,
        parent: CompositionReference? = null,
        composable: @Composable() () -> Unit
    ): Composition {
        val composition = findComposition(container)
            ?: UiComposition(container, context, parent)
        composition.compose(composable)
        return composition
    }

    private class UiComposition(
        private val root: Any,
        private val context: Context,
        parent: CompositionReference? = null
    ) : Composition(
        { slots, recomposer -> UiComposer(context, root, slots, recomposer) },
        parent
    ) {
        init {
            when (root) {
                is ViewGroup -> storeComposition(root, this)
                is Emittable -> storeComposition(root, this)
            }
        }
    }

    // TODO(chuckj): This is a temporary work-around until subframes exist so that
    // nextFrame() inside recompose() doesn't really start a new frame, but a new subframe
    // instead.
    @MainThread
    fun subcomposeInto(
        container: Emittable,
        context: Context,
        parent: CompositionReference? = null,
        composable: @Composable() () -> Unit
    ): Composition {
        val composition = findComposition(container)
            ?: UiComposition(container, context, parent).also { storeComposition(container, it) }
        composition.composer.runWithComposing {
            composition.compose(composable)
        }
        return composition
    }

    /**
     * Disposes any composition previously run with [container] as the root. This will
     * release any resources that have been built around the composition, including all [onDispose]
     * callbacks that have been registered with [CompositionLifecycleObserver] objects.
     *
     * It is important to call this for any [Compose.composeInto] call that is made, or else you may have
     * memory leaks in your application.
     *
     * @param container The view that was passed into [Compose.composeInto] as the root container of the composition
     * @param context The android [Context] associated with the composition
     * @param parent The parent composition reference, if applicable.
     *
     * @see Compose.composeInto
     * @see CompositionLifecycleObserver
     */
    @MainThread
    fun disposeComposition(
        container: Emittable,
        context: Context,
        parent: CompositionReference? = null
    ) {
        // temporary easy way to call correct lifecycles on everything
        composeInto(container, context, parent, emptyComposable)
        EMITTABLE_ROOT_COMPONENT.remove(container)
    }
}

/**
 * Composes the children of the view with the passed in [composable]. This is a convenience method
 * around [Compose.composeInto].
 *
 * @see Compose.composeInto
 * @see disposeComposition
 */
fun ViewGroup.setViewContent(composable: @Composable() () -> Unit): Composition =
    Compose.composeInto(this, null, composable)

/**
 * Disposes of a composition of the children of this view. This is a convenience method around
 * [Compose.disposeComposition].
 *
 * @see Compose.disposeComposition
 * @see Compose.composeInto
 */
fun ViewGroup.disposeComposition() = Compose.disposeComposition(this, null)
