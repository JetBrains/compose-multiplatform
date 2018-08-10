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


    fun composeInto(container: ViewGroup, composable: @Composable() () -> Unit) {
        var root = CompositionContext.getRootComponent(container) as? Root
        if (root == null) {
            root = Root()
            root.composable = composable
            val cc = CompositionContext.create(container.getContext(), container, root)
            cc.recomposeSync(root)
        } else {
            root.composable = composable
            CompositionContext.recomposeSync(root)
        }
    }
}

inline fun ViewGroup.composeInto(noinline composable: @Composable() () -> Unit) = R4a.composeInto(this, composable)