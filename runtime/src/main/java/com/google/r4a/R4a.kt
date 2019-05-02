package com.google.r4a

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
 * A global namespace to hold some Compose utility methods, such as [R4a.composeInto] and
 * [R4a.disposeComposition].
 */
object R4a {

    private class Root : Component() {
        @Suppress("DEPRECATION")
        fun update() = recomposeSync()
        lateinit var composable: @Composable() () -> Unit
        @Suppress("PLUGIN_ERROR")
        override fun compose() {
            val cc = currentComposerNonNull
            cc.startGroup(0)
            composable()
            cc.endGroup()
        }
    }

    private val TAG_ROOT_COMPONENT = "r4aRootComponent".hashCode()
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
    ): CompositionContext = CompositionContext.create(
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
     * @see R4a.disposeComposition
     * @see Composable
     */
    // TODO(lmr): rename to compose?
    @MainThread
    fun composeInto(
        container: ViewGroup,
        parent: CompositionReference? = null,
        composable: @Composable() () -> Unit
    ) {
        var root = getRootComponent(container) as? Root
        if (root == null) {
            container.removeAllViews()
            root = Root()
            root.composable = composable
            setRoot(container, root)
            val cc = CompositionContext.create(container.context, container, root, parent)
            cc.recompose()
        } else {
            root.composable = composable
            root.recomposeCallback?.invoke(true)
        }
    }

    /**
     * Disposes any composition previously run with [container] as the root. This will
     * release any resources that have been built around the composition, including all [onDispose]
     * callbacks that have been registered with [CompositionLifecycleObserver] objects.
     *
     * It is important to call this for any [R4a.composeInto] call that is made, or else you may have
     * memory leaks in your application.
     *
     * @param container The view that was passed into [R4a.composeInto] as the root container of the composition
     * @param parent The parent composition reference, if applicable.
     *
     * @see R4a.composeInto
     * @see CompositionLifecycleObserver
     */
    @MainThread
    fun disposeComposition(container: ViewGroup, parent: CompositionReference? = null) {
        // temporary easy way to call correct lifecycles on everything
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
     * It is important to call [R4a.disposeComposition] whenever this view is no longer needed in order
     * to release resources.
     *
     * @param container The emittable whose children is being composed.
     * @param context The android [Context] to associate with this composition.
     * @param parent The parent composition reference, if applicable. Default is null.
     * @param composable The composable function intended to compose the children of [container].
     *
     * @see R4a.disposeComposition
     * @see Composable
     */
    // TODO(lmr): rename to compose?
    @MainThread
    fun composeInto(
        container: Emittable,
        context: Context,
        parent: CompositionReference? = null,
        composable: @Composable() () -> Unit
    ) {
        var root = getRootComponent(container) as? Root
        if (root == null) {
            root = Root()
            root.composable = composable
            setRoot(container, root)
            val cc = CompositionContext.create(context, container, root, parent)
            cc.recompose()
        } else {
            root.composable = composable
            root.recomposeCallback?.invoke(true)
        }
    }

    /**
     * Disposes any composition previously run with [container] as the root. This will
     * release any resources that have been built around the composition, including all [onDispose]
     * callbacks that have been registered with [CompositionLifecycleObserver] objects.
     *
     * It is important to call this for any [R4a.composeInto] call that is made, or else you may have
     * memory leaks in your application.
     *
     * @param container The view that was passed into [R4a.composeInto] as the root container of the composition
     * @param context The android [Context] associated with the composition
     * @param parent The parent composition reference, if applicable.
     *
     * @see R4a.composeInto
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
 * with the passed in [composable]. This is a convenience method around [R4a.composeInto].
 *
 * @see R4a.composeInto
 * @see Activity.setContentView
 */
fun Activity.setContent(composable: @Composable() () -> Unit) =
    setContentView(FrameLayout(this).apply { compose(composable) })

/**
 * Disposes of a composition that was started using [setContent]. This is a convenience method
 * around [R4a.disposeComposition].
 *
 * @see setContent
 * @see R4a.disposeComposition
 */
fun Activity.disposeComposition() {
    val view = window
        .decorView
        .findViewById<ViewGroup>(android.R.id.content)
        .getChildAt(0) as? ViewGroup
            ?: error("No root view found")
    R4a.disposeComposition(view, null)
}

/**
 * Composes the children of the view with the passed in [composable]. This is a convenience method
 * around [R4a.composeInto].
 *
 * @see R4a.composeInto
 * @see disposeComposition
 */
fun ViewGroup.compose(composable: @Composable() () -> Unit) =
    R4a.composeInto(this, null, composable)

/**
 * Disposes of a composition of the children of this view. This is a convenience method around
 * [R4a.disposeComposition].
 *
 * @see R4a.disposeComposition
 * @see compose
 */
fun ViewGroup.disposeComposition() = R4a.disposeComposition(this, null)
