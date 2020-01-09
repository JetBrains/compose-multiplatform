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

// TODO(lmr): consider moving this to the ViewComposer directly
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
    private class HotReloader {
        companion object {
            private var emittables = HashMap<Emittable, @Composable() () -> Unit>()
            private var views = HashMap<ViewGroup, @Composable() () -> Unit>()

            @TestOnly
            fun clearRoots() {
                EMITTABLE_ROOT_COMPONENT.clear()
                VIEWGROUP_ROOT_COMPONENT.clear()
            }

            // Called before Dex Code Swap
            private fun saveStateAndDispose(context: Context) {
                emittables.clear()
                views.clear()

                val emittableRoots = EMITTABLE_ROOT_COMPONENT.entries.toSet()

                for ((emittable, component) in emittableRoots) {
                    (component as? Root)?.let {
                        emittables.put(emittable, it.composable)
                        disposeComposition(emittable, context, null)
                    }
                }

                val viewRoots = VIEWGROUP_ROOT_COMPONENT.entries.toSet()

                for ((view, component) in viewRoots) {
                    (component as? Root)?.let {
                        views.put(view, it.composable)
                        disposeComposition(view)
                    }
                }
            }

            // Called after Dex Code Swap
            private fun loadStateAndCompose(context: Context) {
                for ((emittable, composable) in emittables) {
                    composeInto(emittable, context, null, composable)
                }

                for ((view, composable) in views) {
                    view.setViewContent(composable)
                }

                emittables.clear()
                views.clear()
            }

            @TestOnly
            internal fun simulateHotReload(context: Context) {
                saveStateAndDispose(context)
                loadStateAndCompose(context)
            }
        }
    }

    private class Root : Component() {
        fun update() = composer.compose()

        lateinit var composable: @Composable() () -> Unit
        lateinit var composer: CompositionContext
        override fun compose() {
            val cc = currentComposerNonNull
            cc.startGroup(0)
            composable()
            cc.endGroup()
        }
    }

    private val TAG_ROOT_COMPONENT = "composeRootComponent".hashCode()
    private val EMITTABLE_ROOT_COMPONENT = WeakHashMap<Emittable, Component>()
    private val VIEWGROUP_ROOT_COMPONENT = WeakHashMap<ViewGroup, Component>()

    private fun getRootComponent(view: View): Component? {
        return view.getTag(TAG_ROOT_COMPONENT) as? Component
    }

    // TODO(b/138254844): Make findRoot/setRoot test-only & Android-only
    fun findRoot(view: View): Component? {
        var node: View? = view
        while (node != null) {
            val cc = node.getTag(TAG_ROOT_COMPONENT) as? Component
            if (cc != null) return cc
            node = node.parent as? View
        }
        return null
    }

    internal fun setRoot(view: View, component: Component) {
        view.setTag(TAG_ROOT_COMPONENT, component)
        if (view is ViewGroup)
            VIEWGROUP_ROOT_COMPONENT[view] = component
    }

    internal fun removeRoot(view: View) {
        view.setTag(TAG_ROOT_COMPONENT, null)
        if (view is ViewGroup)
            VIEWGROUP_ROOT_COMPONENT.remove(view)
    }

    private fun getRootComponent(emittable: Emittable): Component? {
        return EMITTABLE_ROOT_COMPONENT[emittable]
    }

    // TODO(b/138254844): Make findRoot/setRoot test-only & Android-only
    private fun setRoot(emittable: Emittable, component: Component) {
        EMITTABLE_ROOT_COMPONENT[emittable] = component
    }

    /**
     * @suppress
     */
    @TestOnly
    fun createCompositionContext(
        context: Context,
        group: Any,
        component: Component,
        reference: CompositionReference?
    ): CompositionContext = CompositionContext.prepare(
        context,
        group,
        component,
        reference
    ).also {
        when (group) {
            is ViewGroup -> setRoot(group, component)
            is Emittable -> setRoot(group, component)
        }
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
    ): CompositionContext? {
        var root = getRootComponent(container) as? Root
        if (root == null) {
            container.removeAllViews()
            root = Root()
            root.composable = composable
            setRoot(container, root)
            val cc = CompositionContext.prepare(
                container.context,
                container,
                root,
                parent
            )
            root.composer = cc
            root.update()
            return cc
        } else {
            root.composable = composable
            root.update()
        }
        return null
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
    ): CompositionContext {
        var root = getRootComponent(container) as? Root
        return if (root == null) {
            root = Root()
            root.composable = composable
            setRoot(container, root)
            val cc = CompositionContext.prepare(context, container, root, parent)
            root.composer = cc
            root.update()
            cc
        } else {
            root.composable = composable
            root.update()
            root.composer
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
    ): CompositionContext {
        var root = getRootComponent(container) as? Root
        return if (root == null) {
            root = Root()
            root.composable = composable
            setRoot(container, root)
            val cc = CompositionContext.prepare(context, container, root, parent)
            root.composer = cc
            val wasComposing = cc.composer.isComposing
            cc.composer.isComposing = true
            root.update()
            cc.composer.isComposing = wasComposing
            cc
        } else {
            root.composable = composable
            val wasComposing = root.composer.composer.isComposing
            root.composer.composer.isComposing = true
            root.update()
            root.composer.composer.isComposing = wasComposing
            root.composer
        }
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
fun ViewGroup.setViewContent(composable: @Composable() () -> Unit): CompositionContext? =
    Compose.composeInto(this, null, composable)

/**
 * Disposes of a composition of the children of this view. This is a convenience method around
 * [Compose.disposeComposition].
 *
 * @see Compose.disposeComposition
 * @see compose
 */
fun ViewGroup.disposeComposition() = Compose.disposeComposition(this, null)
