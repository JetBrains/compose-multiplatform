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

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.MainThread
import org.jetbrains.annotations.TestOnly
import java.util.WeakHashMap

// TODO(lmr): consider moving this to the ViewComposer directly
/**
 * A global namespace to hold some Compose utility methods, such as [Compose.composeInto] and
 * [Compose.disposeComposition].
 */
object Compose {

    private class Root : Component() {
        fun update() = composer.compose()

        lateinit var composable: @Composable() () -> Unit
        lateinit var composer: CompositionContext
        @Suppress("PLUGIN_ERROR")
        override fun compose() {
            val cc = currentComposerNonNull
            cc.startGroup(0)
            composable()
            cc.endGroup()
        }
    }

    private val TAG_ROOT_COMPONENT = "composeRootComponent".hashCode()
    private val EMITTABLE_ROOT_COMPONENT = WeakHashMap<Emittable, Component>()

    private fun getRootComponent(view: View): Component? {
        return view.getTag(TAG_ROOT_COMPONENT) as? Component
    }

    // TODO(lmr): used by tests only. consider ways to remove.
    internal fun findRoot(view: View): Component? {
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
    }

    private fun getRootComponent(emittable: Emittable): Component? {
        return EMITTABLE_ROOT_COMPONENT[emittable]
    }

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
        composeInto(container, parent) { }
        container.setTag(TAG_ROOT_COMPONENT, null)
    }

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
        composeInto(container, context, parent) {}
        EMITTABLE_ROOT_COMPONENT.remove(container)
    }
}

/**
 * Sets the contentView of an activity to a FrameLayout, and composes the contents of the layout
 * with the passed in [composable]. This is a convenience method around [Compose.composeInto].
 *
 * @see Compose.composeInto
 * @see Activity.setContentView
 */
fun Activity.setContent(composable: @Composable() () -> Unit): CompositionContext? {
    // If there is already a FrameLayout in the root, we assume we want to compose
    // into it instead of create a new one. This allows for `setContent` to be
    // called multiple times.
    val root = window
        .decorView
        .findViewById<ViewGroup>(android.R.id.content)
        .getChildAt(0) as? ViewGroup
    ?: FrameLayout(this).also { setContentView(it) }
    return root.compose(composable)
}

/**
 * Disposes of a composition that was started using [setContent]. This is a convenience method
 * around [Compose.disposeComposition].
 *
 * @see setContent
 * @see Compose.disposeComposition
 */
fun Activity.disposeComposition() {
    val view = window
        .decorView
        .findViewById<ViewGroup>(android.R.id.content)
        .getChildAt(0) as? ViewGroup
        ?: error("No root view found")
    Compose.disposeComposition(view, null)
}

/**
 * Composes the children of the view with the passed in [composable]. This is a convenience method
 * around [Compose.composeInto].
 *
 * @see Compose.composeInto
 * @see disposeComposition
 */
fun ViewGroup.compose(composable: @Composable() () -> Unit): CompositionContext? =
    Compose.composeInto(this, null, composable)

/**
 * Disposes of a composition of the children of this view. This is a convenience method around
 * [Compose.disposeComposition].
 *
 * @see Compose.disposeComposition
 * @see compose
 */
fun ViewGroup.disposeComposition() = Compose.disposeComposition(this, null)
