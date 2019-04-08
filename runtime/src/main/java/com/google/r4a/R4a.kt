package com.google.r4a

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout

object R4a {

    private class Root : Component() {
        lateinit var composable: @Composable() () -> Unit
        @Suppress("PLUGIN_ERROR")
        override fun compose() {
            val cc = composer.composer
            cc.startGroup(0)
            composable()
            cc.endGroup()
        }
    }

    fun composeInto(
        container: ViewGroup,
        parent: Ambient.Reference? = null,
        composable: @Composable() () -> Unit
    ) {
        var root = CompositionContext.getRootComponent(container) as? Root
        if (root == null) {
            container.removeAllViews()
            root = Root()
            root.composable = composable
            val cc = CompositionContext.create(container.context, container, root, parent)
            cc.recomposeSync(root)
        } else {
            root.composable = composable
            CompositionContext.recomposeSync(root)
        }
    }

    fun disposeComposition(container: ViewGroup, parent: Ambient.Reference? = null) {
        // temporary easy way to call correct lifecycles on everything
        composeInto(container, parent) { }
        CompositionContext.disposeComposition(container, parent)
    }

    fun composeInto(
        container: Emittable,
        context: Context,
        parent: Ambient.Reference? = null,
        composable: @Composable() () -> Unit
    ) {
        var root = CompositionContext.getRootComponent(container) as? Root
        if (root == null) {
            root = Root()
            root.composable = composable
            CompositionContext.setRoot(container, root)
            val cc = CompositionContext.create(context, container, root, parent)
            cc.recomposeSync(root)
        } else {
            root.composable = composable
            CompositionContext.recomposeSync(root)
        }
    }

    fun disposeComposition(
        container: Emittable,
        context: Context,
        parent: Ambient.Reference? = null
    ) {
        // temporary easy way to call correct lifecycles on everything
        composeInto(container, context, parent) {}
        CompositionContext.disposeComposition(container, parent)
    }
}

fun Activity.composeInto(composable: @Composable() () -> Unit) = setContentView(composable)
fun Activity.setContent(composable: @Composable() () -> Unit) = setContentView(composable)
fun Activity.setContentView(composable: @Composable() () -> Unit) =
    setContentView(FrameLayout(this).apply { composeInto(composable) })
fun ViewGroup.composeInto(composable: @Composable() () -> Unit) =
    R4a.composeInto(this, null, composable)
