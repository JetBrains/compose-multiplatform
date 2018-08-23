package com.google.r4a

import android.view.ViewGroup

object R4a {

    private class Root: Component() {
        lateinit var composable: @Composable() () -> Unit
        override fun compose() {
            val cc = CompositionContext.current
            cc.start(0)
            composable()
            cc.end()
        }
    }


    fun composeInto(
        container: ViewGroup,
        parent: Ambient.Reference? = null,
        composable: () -> Unit
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
}

inline fun ViewGroup.composeInto(noinline composable: @Composable() () -> Unit) = R4a.composeInto(this, null, composable)