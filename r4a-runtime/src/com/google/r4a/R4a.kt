package com.google.r4a

import android.view.ViewGroup

object R4a {

    private class Root: Component() {
        lateinit var composable: () -> Unit
        override fun compose() {
            val cc = CompositionContext.current
            cc.start(0)
            cc.setInstance(composable)
            cc.compose()
            cc.end()
        }
    }

    fun composeInto(container: ViewGroup, composable: () -> Unit) {
        var root = CompositionContext.getRootComponent(container) as? Root
        if (root == null) {
            root = Root()
            root.composable = composable
            val cc = CompositionContext.create(container.getContext(), container, root)
            cc.recompose(root)
        } else {
            root.composable = composable
            CompositionContext.recompose(root)
        }
    }
}

inline fun ViewGroup.composeInto(noinline composable: () -> Unit) = R4a.composeInto(this, composable)