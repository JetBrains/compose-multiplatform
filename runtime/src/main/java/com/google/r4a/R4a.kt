package com.google.r4a

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import org.jetbrains.annotations.TestOnly
import java.util.WeakHashMap

// TODO(lmr): consider moving this to the ViewComposer directly
object R4a {

    private class Root : Component() {
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

    fun disposeComposition(container: ViewGroup, parent: CompositionReference? = null) {
        // temporary easy way to call correct lifecycles on everything
        composeInto(container, parent) { }
        container.setTag(TAG_ROOT_COMPONENT, null)
    }

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

fun Activity.composeInto(composable: @Composable() () -> Unit) = setContentView(composable)
fun Activity.setContent(composable: @Composable() () -> Unit) = setContentView(composable)
fun Activity.setContentView(composable: @Composable() () -> Unit) =
    setContentView(FrameLayout(this).apply { composeInto(composable) })
fun ViewGroup.composeInto(composable: @Composable() () -> Unit) =
    R4a.composeInto(this, null, composable)
